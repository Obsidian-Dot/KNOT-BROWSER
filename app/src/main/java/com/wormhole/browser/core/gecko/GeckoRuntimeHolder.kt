package com.wormhole.browser.core.gecko

import android.content.Context
import android.os.Looper
import androidx.core.os.HandlerCompat
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import java.util.concurrent.CountDownLatch

/**
 * Process-wide [GeckoRuntime] (one per app, same pattern as Fenix/Iceraven).
 */
object GeckoRuntimeHolder {
    @Volatile private var runtime: GeckoRuntime? = null
    @Volatile private var pendingDarkContent: Boolean = true

    // Content blocking defaults mirror the Settings screen's own defaults
    // (SettingsRepository: trackers/ads/popups default enabled) so a runtime
    // created before the real prefs load starts in the same state they'll be
    // applied to moments later, rather than briefly running wide open.
    @Volatile private var pendingTrackerBlocking: Boolean = true
    @Volatile private var pendingAdBlocking: Boolean = true

    /**
     * @param initialContentPrefersDark the user's actual "website appearance"
     * setting, read from disk. Only used the first time the runtime is created;
     * ignored on subsequent calls since the runtime is a process-wide singleton.
     */
    fun get(
        context: Context,
        initialContentPrefersDark: Boolean = pendingDarkContent,
        initialTrackerBlocking: Boolean = pendingTrackerBlocking,
        initialAdBlocking: Boolean = pendingAdBlocking,
    ): GeckoRuntime {
        runtime?.let { return it }
        synchronized(this) {
            runtime?.let { return it }
            pendingDarkContent = initialContentPrefersDark
            pendingTrackerBlocking = initialTrackerBlocking
            pendingAdBlocking = initialAdBlocking
            val scheme = if (pendingDarkContent) {
                GeckoRuntimeSettings.COLOR_SCHEME_DARK
            } else {
                GeckoRuntimeSettings.COLOR_SCHEME_LIGHT
            }
            // preferredColorScheme is deliberately NOT set on the Builder here.
            // GeckoRuntimeSettings.Builder.preferredColorScheme() calls through
            // to GeckoSystemStateListener, which touches a native JNI method
            // that only exists once Gecko's native library has been loaded --
            // and that load only happens as a side effect of
            // GeckoRuntime.create() itself. Calling it on the Builder (i.e.
            // before create() has run) throws UnsatisfiedLinkError the very
            // first time a runtime is created in this process. Setting it via
            // the settings instance AFTER create() (below) hits the same
            // native call once the library is guaranteed to be loaded.
            val settings = GeckoRuntimeSettings.Builder()
                .aboutConfigEnabled(false)
                .consoleOutput(false)
                .debugLogging(false)
                .contentBlocking(buildContentBlockingSettings(pendingTrackerBlocking, pendingAdBlocking))
                .build()
            val created = createOnMainThread(context.applicationContext, settings)
            runtime = created
            runCatching { created.settings.preferredColorScheme = scheme }
            // Installs the bundled knot-bridge WebExtension so page
            // read/tap/type/scroll/etc. tools have a real DOM channel
            // instead of falling back to GeckoJs's "unavailable" sentinel.
            GeckoExtensionBridge.ensureInstalled(created)
            return created
        }
    }

    /**
     * GeckoRuntime.create() asserts it's being called on the main/UI thread
     * and throws IllegalThreadStateException otherwise. [get] can be invoked
     * from anywhere (a background coroutine at app startup, an IO-dispatched
     * call site, a Compose click callback, etc.), so rather than trusting
     * every caller to already be on the main thread, hop there ourselves and
     * block the calling thread until creation finishes. This keeps [get]'s
     * contract simple ("call from any thread") and fixes the crash at its
     * single root cause instead of needing every call site fixed
     * individually.
     */
    private fun createOnMainThread(
        context: Context,
        settings: GeckoRuntimeSettings,
    ): GeckoRuntime {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return GeckoRuntime.create(context, settings)
        }
        var result: GeckoRuntime? = null
        var error: Throwable? = null
        val latch = CountDownLatch(1)
        val mainHandler = HandlerCompat.createAsync(Looper.getMainLooper())
        mainHandler.post {
            try {
                result = GeckoRuntime.create(context, settings)
            } catch (t: Throwable) {
                error = t
            } finally {
                latch.countDown()
            }
        }
        latch.await()
        error?.let { throw it }
        return result ?: error("GeckoRuntime.create returned null with no error")
    }

    private fun antiTrackingCategory(trackerBlocking: Boolean, adBlocking: Boolean): Int {
        // Ads aren't their own ETP category -- AT_AD covers ad-network trackers,
        // which is the closest match GeckoView's tracking-protection categories
        // have to a dedicated "block ads" toggle (full cosmetic ad-element hiding
        // would need a content script / uBlock-style filter list, which this
        // engine doesn't run).
        var category = 0
        if (trackerBlocking) {
            category = category or ContentBlocking.AntiTracking.STP or
                ContentBlocking.AntiTracking.FINGERPRINTING or
                ContentBlocking.AntiTracking.CRYPTOMINING
        }
        if (adBlocking) {
            category = category or ContentBlocking.AntiTracking.AD
        }
        return category
    }

    private fun buildContentBlockingSettings(trackerBlocking: Boolean, adBlocking: Boolean): ContentBlocking.Settings =
        ContentBlocking.Settings.Builder()
            .antiTracking(antiTrackingCategory(trackerBlocking, adBlocking))
            .enhancedTrackingProtectionLevel(
                if (trackerBlocking || adBlocking) {
                    ContentBlocking.EtpLevel.DEFAULT
                } else {
                    ContentBlocking.EtpLevel.NONE
                },
            )
            .cookieBehavior(
                if (trackerBlocking) {
                    ContentBlocking.CookieBehavior.ACCEPT_NON_TRACKERS
                } else {
                    ContentBlocking.CookieBehavior.ACCEPT_ALL
                },
            )
            .build()

    /**
     * Applies tracker/ad blocking changes live. Safe to call before the runtime
     * exists (the values are simply remembered for the eventual [get] call).
     */
    fun setContentBlocking(trackerBlocking: Boolean, adBlocking: Boolean) {
        pendingTrackerBlocking = trackerBlocking
        pendingAdBlocking = adBlocking
        val runtime = runtime ?: return
        // ContentBlocking.Settings is mutated in place via its own setters --
        // there is no `GeckoRuntimeSettings.contentBlocking` setter to reassign,
        // only a getter that returns the live, already-attached settings object
        // (this mirrors Mozilla's own GeckoViewActivity reference usage).
        runtime.settings.contentBlocking.apply {
            setAntiTracking(antiTrackingCategory(trackerBlocking, adBlocking))
            setEnhancedTrackingProtectionLevel(
                if (trackerBlocking || adBlocking) ContentBlocking.EtpLevel.DEFAULT else ContentBlocking.EtpLevel.NONE,
            )
            setCookieBehavior(
                if (trackerBlocking) ContentBlocking.CookieBehavior.ACCEPT_NON_TRACKERS else ContentBlocking.CookieBehavior.ACCEPT_ALL,
            )
        }
    }

    /**
     * Firefox "Website appearance": tell Gecko [prefers-color-scheme] so sites
     * follow the app theme.
     */
    fun setContentPrefersDark(dark: Boolean) {
        pendingDarkContent = dark
        val runtime = runtime ?: return
        runtime.settings.preferredColorScheme = if (dark) {
            GeckoRuntimeSettings.COLOR_SCHEME_DARK
        } else {
            GeckoRuntimeSettings.COLOR_SCHEME_LIGHT
        }
    }
}
