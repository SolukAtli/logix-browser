package com.logix.browser.chromiumbridge

/**
 * [Engine] that prefers the native content bridge when its libraries are
 * present and delegates to [fallback] (system WebView) otherwise.
 *
 * Navigation history still lives in the fallback until the Trichrome
 * rendering path lands; the native handle currently tracks the current
 * URL for future handoff (Faz-4.1).
 *
 * Lifetime note: [destroy] releases the native handle only — the fallback
 * instance belongs to its creator (e.g. ContentViewHost) and is left alone.
 */
class ChromiumEngine(
    private val fallback: Engine,
) : Engine {

    val isNative: Boolean = NativeLib.isLoaded

    private var handle: Long = openHandle()

    override fun loadUrl(url: String) {
        val h = handle
        if (h != 0L && navigateNative(h, url)) return
        fallback.loadUrl(url)
    }

    override fun goBack(): Boolean = fallback.goBack()

    override fun goForward(): Boolean = fallback.goForward()

    override fun reload() = fallback.reload()

    override fun canGoBack(): Boolean = fallback.canGoBack()

    override fun canGoForward(): Boolean = fallback.canGoForward()

    override fun currentUrl(): String? = fallback.currentUrl()

    override fun clearHistory() = fallback.clearHistory()

    override fun onShow() = fallback.onShow()

    override fun onHide() = fallback.onHide()

    override fun destroy() {
        closeHandle()
    }

    override fun captureThumbnail(maxWidth: Int): android.graphics.Bitmap? =
        fallback.captureThumbnail(maxWidth)

    override fun findAll(text: String?) = fallback.findAll(text)

    override fun findNext(forward: Boolean) = fallback.findNext(forward)

    override fun evaluateJs(script: String, onResult: (String?) -> Unit) =
        fallback.evaluateJs(script, onResult)

    private fun openHandle(): Long {
        if (!isNative) return 0L
        return try {
            NativeLib.nativeCreate()
        } catch (e: UnsatisfiedLinkError) {
            0L
        }
    }

    private fun navigateNative(h: Long, url: String): Boolean =
        try {
            NativeLib.nativeNavigate(h, url)
        } catch (e: UnsatisfiedLinkError) {
            handle = 0L
            false
        }

    private fun closeHandle() {
        val h = handle
        handle = 0L
        if (h == 0L || !isNative) return
        try {
            NativeLib.nativeDestroy(h)
        } catch (e: UnsatisfiedLinkError) {
            // Already unloaded; nothing to release.
        }
    }
}
