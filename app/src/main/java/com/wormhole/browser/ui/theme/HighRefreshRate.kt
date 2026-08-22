package com.wormhole.browser.ui.theme

import android.app.Activity
import android.os.Build
import android.view.Display
import android.view.View
import android.view.Window
import android.view.WindowManager
import kotlin.math.abs
import kotlin.math.min

/**
 * Requests the highest refresh rate the panel can hold, capped at [TARGET_HZ].
 * Window preferred mode is the reliable path; Surface frame-rate is applied
 * reflectively so this compiles on every compileSdk that still lacks View.setFrameRate.
 */
object HighRefreshRate {
    const val TARGET_HZ = 185f

    fun apply(activity: Activity) {
        apply(activity.window)
        applyToView(activity.window.decorView)
    }

    fun apply(window: Window) {
        val display = currentDisplay(window) ?: return
        val choice = pickMode(display) ?: return
        val capped = min(choice.refreshRate, TARGET_HZ)

        val attrs = window.attributes
        attrs.preferredDisplayModeId = choice.modeId
        attrs.preferredRefreshRate = capped
        window.attributes = attrs
        window.addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)

        applyToView(window.decorView, capped)
    }

    fun applyToView(view: View, hz: Float = TARGET_HZ) {
        val rate = hz.coerceAtLeast(60f)
        // View.setFrameRate is API 30+ and missing from some compile SDKs.
        runCatching {
            val floatT = java.lang.Float.TYPE
            val intT = java.lang.Integer.TYPE
            if (Build.VERSION.SDK_INT >= 31) {
                view.javaClass.getMethod("setFrameRate", floatT, intT, intT)
                    .invoke(view, rate, /* FIXED_SOURCE */ 0, /* ALWAYS */ 1)
            } else if (Build.VERSION.SDK_INT >= 30) {
                view.javaClass.getMethod("setFrameRate", floatT, intT)
                    .invoke(view, rate, /* FIXED_SOURCE */ 0)
            }
        }
    }

    fun pickMode(display: Display): Display.Mode? {
        val modes = display.supportedModes
        if (modes.isEmpty()) return null

        val atOrUnder = modes.filter { it.refreshRate <= TARGET_HZ + 3f }
        if (atOrUnder.isNotEmpty()) {
            return atOrUnder.maxByOrNull { it.refreshRate }
        }

        return modes.minByOrNull { abs(it.refreshRate - TARGET_HZ) }
    }

    private fun currentDisplay(window: Window): Display? {
        @Suppress("DEPRECATION")
        return window.windowManager.defaultDisplay
    }
}
