package com.logix.browser.chromiumbridge

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
}
