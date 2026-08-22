package com.wormhole.browser.core.webview

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.webkit.WebView
import kotlin.math.max
import kotlin.math.min

object BottomEdgeColorSampler {

    private const val SAMPLE_HEIGHT_PX = 120

    private const val SAMPLE_WIDTH_PX = 24
    private const val SAMPLE_TARGET_HEIGHT_PX = 6

    fun sampleIsLight(webView: WebView): Boolean? {
        val bitmap = captureBottomStrip(webView) ?: return null
        return try {
            averageIsLight(bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    /**
     * Samples the average color of the WebView's bottom edge strip, used as the
     * background fill behind the animated toolbar inset so it blends with the page
     * instead of showing the app's base background color during the transition.
     */
    fun sampleAverageColor(webView: WebView): Int? {
        val bitmap = captureBottomStrip(webView) ?: return null
        return try {
            averageColor(bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    private fun captureBottomStrip(webView: WebView): Bitmap? {
        val viewWidth = webView.width
        val viewHeight = webView.height
        if (viewWidth <= 0 || viewHeight <= 0) return null

        val stripHeight = min(SAMPLE_HEIGHT_PX, viewHeight)
        val stripTop = max(0, viewHeight - stripHeight)

        val bitmap = Bitmap.createBitmap(SAMPLE_WIDTH_PX, SAMPLE_TARGET_HEIGHT_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.scale(SAMPLE_WIDTH_PX / viewWidth.toFloat(), SAMPLE_TARGET_HEIGHT_PX / stripHeight.toFloat())
        canvas.translate(0f, -stripTop.toFloat())
        return try {
            webView.draw(canvas)
            bitmap
        } catch (_: Exception) {
            bitmap.recycle()
            null
        }
    }

    private fun averageIsLight(bitmap: Bitmap): Boolean {
        var totalLuminance = 0.0
        var sampleCount = 0
        for (x in 0 until bitmap.width) {
            for (y in 0 until bitmap.height) {
                val pixel = bitmap.getPixel(x, y)

                val luminance = (0.299 * Color.red(pixel) + 0.587 * Color.green(pixel) + 0.114 * Color.blue(pixel))
                totalLuminance += luminance
                sampleCount++
            }
        }
        if (sampleCount == 0) return true
        val average = totalLuminance / sampleCount

        return average > 127.5
    }

    private fun averageColor(bitmap: Bitmap): Int? {
        var totalRed = 0L
        var totalGreen = 0L
        var totalBlue = 0L
        var sampleCount = 0
        for (x in 0 until bitmap.width) {
            for (y in 0 until bitmap.height) {
                val pixel = bitmap.getPixel(x, y)
                totalRed += Color.red(pixel)
                totalGreen += Color.green(pixel)
                totalBlue += Color.blue(pixel)
                sampleCount++
            }
        }
        if (sampleCount == 0) return null
        return Color.rgb(
            (totalRed / sampleCount).toInt(),
            (totalGreen / sampleCount).toInt(),
            (totalBlue / sampleCount).toInt(),
        )
    }
}
