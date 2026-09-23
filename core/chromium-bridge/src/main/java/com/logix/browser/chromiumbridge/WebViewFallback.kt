package com.logix.browser.chromiumbridge

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Temporary wrapper: hosts a system WebView inside Compose. The [Engine]
 * interface stays stable when the real Chromium embedding lands.
 *
 * [onNavigationStateChanged] reports back/forward availability so the host
 * can drive system-back handling ([canGoBack] gating).
 *
 * Desktop-class mobile Chrome UA: presents a standard browser profile so
 * Google serves normal results instead of bot-detection challenges.
 */
const val LOGIX_CHROME_UA = UserAgents.MOBILE
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ContentViewHost(
    url: String,
    modifier: Modifier = Modifier,
    onEngineReady: (Engine) -> Unit = {},
    onNavigationStateChanged: (canGoBack: Boolean, canGoForward: Boolean) -> Unit = { _, _ -> },
    onProgressChanged: (progress: Int) -> Unit = {},
    onPageVisited: (url: String, title: String) -> Unit = { _, _ -> },
    userAgent: String = UserAgents.MOBILE,
    textScale: Float = 1f,
    incognito: Boolean = false,
) {
    var wasIncognito by remember { mutableStateOf(incognito) }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.userAgentString = userAgent
                settings.textZoom = (textScale.coerceIn(0.5f, 3f) * 100).toInt()
                applyIncognito(this, incognito)
                webViewClient = object : WebViewClient() {
                    private fun report(view: WebView) {
                        onNavigationStateChanged(view.canGoBack(), view.canGoForward())
                    }

                    override fun onPageFinished(view: WebView, url: String) {
                        super.onPageFinished(view, url)
                        report(view)
                        if (!incognito) {
                            onPageVisited(url, view.title.orEmpty())
                        }
                    }

                    override fun doUpdateVisitedHistory(
                        view: WebView?,
                        url: String?,
                        isReload: Boolean,
                    ) {
                        super.doUpdateVisitedHistory(view, url, isReload)
                        if (view != null) report(view)
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest,
                    ): Boolean = false
                }
                onEngineReady(WebViewEngine(this))
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        onProgressChanged(newProgress)
                    }
                }
                loadUrl(url)
            }
        },
        update = { view ->
            if (view.settings.userAgentString != userAgent) {
                view.settings.userAgentString = userAgent
                view.reload()
            }
            view.settings.textZoom = (textScale.coerceIn(0.5f, 3f) * 100).toInt()
            if (incognito != wasIncognito) {
                applyIncognito(view, incognito)
                wasIncognito = incognito
            }
            if (view.url != url) {
                view.loadUrl(url)
            }
        },
    )
}

/**
 * Incognito isolation: nothing (history, cookies, cache, form data) is
 * written to disk while enabled; restores normal persistence afterwards.
 */
private fun applyIncognito(view: WebView, incognito: Boolean) {
    val cookies = CookieManager.getInstance()
    if (incognito) {
        view.clearHistory()
        view.clearFormData()
        view.clearCache(true)
        cookies.setAcceptCookie(false)
        view.settings.saveFormData = false
        view.settings.databaseEnabled = false
        view.settings.cacheMode = WebSettings.LOAD_NO_CACHE
    } else {
        cookies.setAcceptCookie(true)
        view.settings.saveFormData = true
        view.settings.databaseEnabled = true
        view.settings.cacheMode = WebSettings.LOAD_DEFAULT
    }
}
