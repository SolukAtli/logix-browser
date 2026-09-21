package com.logix.browser.chromiumbridge

/**
 * Abstraction over the underlying web engine. UI code talks only to this
 * interface so the real Chromium embedding (Faz-2) can replace the
 * temporary WebView fallback without touching callers.
 */
interface Engine {
    fun loadUrl(url: String)
    fun goBack(): Boolean
    fun goForward(): Boolean
    fun reload()
    fun canGoBack(): Boolean
    fun canGoForward(): Boolean
    fun currentUrl(): String?
}
