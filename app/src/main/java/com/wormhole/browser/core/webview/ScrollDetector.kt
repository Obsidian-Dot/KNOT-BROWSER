package com.wormhole.browser.core.webview

import android.webkit.WebView
import kotlin.math.abs

/**
 * Tracks WebView scroll direction and whether the page can meaningfully scroll.
 *
 * Uses public WebView APIs only ([WebView.canScrollVertically], [WebView.contentHeight],
 * [WebView.scale]) — computeVerticalScrollRange/Extent are protected.
 */
class ScrollDetector(
    private val minDeltaPx: Int = 8,
    private val minScrollableOverflowPx: Int = 48,
    private val directionLockPx: Int = 24,
) {
    data class Snapshot(
        val scrollY: Int,
        val isScrollingDown: Boolean,
        val isScrollable: Boolean,
        val contentHeightPx: Int,
        val viewportHeightPx: Int,
    )

    private var lastScrollY: Int = 0
    private var accumulatedDelta: Int = 0
    private var lastDirectionDown: Boolean = false
    private var lastScrollable: Boolean = false
    private var lastContentHeight: Int = 0
    private var lastViewportHeight: Int = 0

    @Suppress("DEPRECATION") // WebView.scale has no public non-deprecated replacement;
    // computeVerticalScrollRange/Extent are protected and inaccessible here.
    private fun measure(webView: WebView): Pair<Int, Int> {
        val viewportH = webView.height.coerceAtLeast(1)
        // contentHeight is CSS px; scale converts toward screen px.
        val contentPx = (webView.contentHeight * webView.scale).toInt().coerceAtLeast(0)
        return contentPx to viewportH
    }

    private fun isScrollable(webView: WebView, contentPx: Int, viewportH: Int): Boolean {
        if (webView.canScrollVertically(1) || webView.canScrollVertically(-1)) return true
        return (contentPx - viewportH) >= minScrollableOverflowPx
    }

    fun onScrollChanged(webView: WebView, scrollY: Int): Snapshot? {
        val delta = scrollY - lastScrollY
        lastScrollY = scrollY

        val (contentPx, viewportH) = measure(webView)
        lastContentHeight = contentPx
        lastViewportHeight = viewportH
        lastScrollable = isScrollable(webView, contentPx, viewportH)

        if (abs(delta) < minDeltaPx) {
            return Snapshot(scrollY, lastDirectionDown, lastScrollable, lastContentHeight, lastViewportHeight)
        }

        accumulatedDelta += delta
        if (abs(accumulatedDelta) >= directionLockPx) {
            lastDirectionDown = accumulatedDelta > 0
            accumulatedDelta = 0
        }

        return Snapshot(scrollY, lastDirectionDown, lastScrollable, lastContentHeight, lastViewportHeight)
    }

    fun probe(webView: WebView): Snapshot {
        val scrollY = webView.scrollY
        lastScrollY = scrollY
        accumulatedDelta = 0
        val (contentPx, viewportH) = measure(webView)
        lastContentHeight = contentPx
        lastViewportHeight = viewportH
        lastScrollable = isScrollable(webView, contentPx, viewportH)
        return Snapshot(scrollY, lastDirectionDown, lastScrollable, lastContentHeight, lastViewportHeight)
    }

    fun reset() {
        lastScrollY = 0
        accumulatedDelta = 0
        lastDirectionDown = false
        lastScrollable = false
        lastContentHeight = 0
        lastViewportHeight = 0
    }
}
