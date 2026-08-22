package com.wormhole.browser.core.webview

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import org.mozilla.geckoview.GeckoView

object TabThumbnailCache {
    private const val THUMBNAIL_WIDTH_PX = 320

    private val thumbnails: SnapshotStateMap<String, Bitmap> = mutableStateMapOf()

    fun get(tabId: String): Bitmap? = thumbnails[tabId]

    /**
     * Captures the tab strip/grid preview for [tabId] from the live [geckoView]
     * while it still holds that tab's content, called from WormHoleGeckoViewHost
     * just before the view's session is swapped to a different tab (ordinary tab
     * switch) or released entirely (view leaving composition). Those are the
     * only two moments a GeckoView is about to stop showing a given tab.
     * GeckoView.capturePixels() is the only public API for reading back
     * rendered pixels -- there is no WebView-style Canvas.draw() path on
     * GeckoView, so this can't be done synchronously the way the old
     * WebView-based capture was.
     */
    fun capture(tabId: String, geckoView: GeckoView) {
        if (geckoView.width <= 0 || geckoView.height <= 0) return
        runCatching {
            geckoView.capturePixels().accept({ bitmap ->
                if (bitmap == null) return@accept
                val scale = THUMBNAIL_WIDTH_PX / bitmap.width.toFloat()
                val targetHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(bitmap, THUMBNAIL_WIDTH_PX, targetHeight, true)
                thumbnails[tabId]?.recycle()
                thumbnails[tabId] = scaled
            }, { /* Session not ready to render (isCompositorReady == false) -- keep any existing thumbnail rather than clearing it. */ })
        }
    }

    fun remove(tabId: String) {
        thumbnails.remove(tabId)?.recycle()
    }

    fun clear() {
        thumbnails.keys.toList().forEach { remove(it) }
    }
}
