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

    /** Drops the in-engine back/forward history (incognito entry). */
    fun clearHistory()

    /** Tab becomes visible: resume rendering/timers. */
    fun onShow()

    /** Tab goes to background: pause rendering/timers, keep session. */
    fun onHide()

    /** Tab evicted: release native resources (WebContents). Session data
     *  (URL/title) must already be persisted as TabState. */
    fun destroy()

    /** O anki görünümün küçük ekran görüntüsü; sekme önizlemeleri için. */
    fun captureThumbnail(maxWidth: Int = 480): android.graphics.Bitmap? = null
}
