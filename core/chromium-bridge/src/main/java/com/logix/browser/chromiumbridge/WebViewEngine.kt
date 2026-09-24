package com.logix.browser.chromiumbridge

import android.graphics.Bitmap
import android.graphics.Canvas
import android.webkit.WebView

/**
 * Temporary [Engine] implementation backed by the system WebView.
 * Replaced by the real Chromium embedding in Faz-2.
 */
class WebViewEngine(private val webView: WebView) : Engine {

    override fun loadUrl(url: String) {
        webView.loadUrl(url)
    }

    override fun goBack(): Boolean =
        if (webView.canGoBack()) {
            webView.goBack()
            true
        } else {
            false
        }

    override fun goForward(): Boolean =
        if (webView.canGoForward()) {
            webView.goForward()
            true
        } else {
            false
        }

    override fun reload() {
        webView.reload()
    }

    override fun canGoBack(): Boolean = webView.canGoBack()

    override fun canGoForward(): Boolean = webView.canGoForward()

    override fun currentUrl(): String? = webView.url

    override fun clearHistory() {
        webView.clearHistory()
    }

    override fun onShow() {
        webView.onResume()
        webView.resumeTimers()
    }

    override fun onHide() {
        webView.onPause()
        webView.pauseTimers()
    }

    override fun destroy() {
        webView.destroy()
    }

    override fun captureThumbnail(maxWidth: Int): Bitmap? = runCatching {
        val w = webView.width
        val h = webView.height
        if (w <= 0 || h <= 0) return null
        val full = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)
        val canvas = Canvas(full)
        canvas.drawColor(android.graphics.Color.WHITE)
        webView.draw(canvas)
        if (w <= maxWidth) return full
        val scale = maxWidth.toFloat() / w
        Bitmap.createScaledBitmap(full, maxWidth, (h * scale).toInt(), true)
    }.getOrNull()

    override fun findAll(text: String?) {
        runCatching {
            if (text.isNullOrEmpty()) {
                webView.clearMatches()
            } else {
                webView.findAllAsync(text)
            }
        }
    }

    override fun findNext(forward: Boolean) {
        runCatching { webView.findNext(forward) }
    }

    override fun evaluateJs(script: String, onResult: (String?) -> Unit) {
        runCatching { webView.evaluateJavascript(script, onResult) }
    }
}
