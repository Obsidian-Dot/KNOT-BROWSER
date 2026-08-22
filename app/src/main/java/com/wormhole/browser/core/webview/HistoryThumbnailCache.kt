package com.wormhole.browser.core.webview

import android.graphics.Bitmap
import android.graphics.Canvas
import android.webkit.WebView
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap

object HistoryThumbnailCache {
    private const val THUMBNAIL_WIDTH_PX = 480
    private const val MAX_ENTRIES = 12

    private val thumbnails: SnapshotStateMap<String, Bitmap> = mutableStateMapOf()

    private val accessOrder = ArrayDeque<String>()

    fun get(url: String): Bitmap? {
        if (thumbnails.containsKey(url)) {
            accessOrder.remove(url)
            accessOrder.addLast(url)
        }
        return thumbnails[url]
    }

    fun capture(url: String, webView: WebView) {
        if (url.isBlank()) return
        val viewWidth = webView.width
        val viewHeight = webView.height
        if (viewWidth <= 0 || viewHeight <= 0) return

        val scale = THUMBNAIL_WIDTH_PX / viewWidth.toFloat()
        val targetWidth = THUMBNAIL_WIDTH_PX
        val targetHeight = (viewHeight * scale).toInt().coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.scale(scale, scale)
        webView.draw(canvas)

        thumbnails[url]?.recycle()
        thumbnails[url] = bitmap
        accessOrder.remove(url)
        accessOrder.addLast(url)

        while (accessOrder.size > MAX_ENTRIES) {
            val oldest = accessOrder.removeFirstOrNull() ?: break
            thumbnails.remove(oldest)?.recycle()
        }
    }

    fun remove(url: String) {
        accessOrder.remove(url)
        thumbnails.remove(url)?.recycle()
    }

    fun clear() {
        accessOrder.clear()
        thumbnails.keys.toList().forEach { thumbnails.remove(it)?.recycle() }
    }
}
