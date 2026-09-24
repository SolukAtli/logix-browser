package com.logix.browser.chromiumbridge

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.logix.browser.adblock.AdBlockStatsRepository
import com.logix.browser.adblock.CosmeticFilter
import com.logix.browser.adblock.DomainAdBlocker
import com.logix.browser.network.HttpsUpgrader
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.io.ByteArrayInputStream

/**
 * Kalkanlara giden DI köprüsü (bu modülde Hilt plugin'i yok).
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ShieldsEntryPoint {
    fun adBlocker(): DomainAdBlocker
    fun stats(): AdBlockStatsRepository
    fun httpsUpgrader(): HttpsUpgrader
    fun cosmetic(): CosmeticFilter
}

/** Ayarlardan gelen kalkan anahtarlarının anlık görüntüsü. */
data class ShieldsConfig(
    val adBlock: Boolean,
    val trackerBlock: Boolean,
    val httpsOnly: Boolean,
)

private data class PrivacyKey(
    val incognito: Boolean,
    val cookiesAccepted: Boolean,
    val userAgent: String,
    val shields: ShieldsConfig,
    val desktopMode: Boolean,
)

/**
 * Temporary wrapper: hosts a system WebView inside Compose. The [Engine]
 * interface stays stable when the real Chromium embedding lands.
 *
 * [onNavigationStateChanged] reports back/forward availability so the host
 * can drive system-back handling ([canGoBack] gating).
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
    onFileChooserRequest: (
        callback: ValueCallback<Array<Uri>>?,
        params: WebChromeClient.FileChooserParams?,
    ) -> Unit = { _, _ -> },
    userAgent: String = UserAgents.MOBILE,
    textScale: Float = 1f,
    incognito: Boolean = false,
    cookiesAccepted: Boolean = true,
    shields: ShieldsConfig = ShieldsConfig(true, true, false),
    desktopMode: Boolean = false,
) {
    var privacyKey by remember { mutableStateOf(PrivacyKey(incognito, cookiesAccepted, userAgent, shields, desktopMode)) }
    val latestFileChooser by rememberUpdatedState(onFileChooserRequest)
    val latestNavState by rememberUpdatedState(onNavigationStateChanged)
    val latestProgress by rememberUpdatedState(onProgressChanged)
    val latestVisited by rememberUpdatedState(onPageVisited)
    val latestShields by rememberUpdatedState(shields)

    AndroidView(
        modifier = modifier,
        factory = { context ->
            val entry = EntryPointAccessors.fromApplication(
                context.applicationContext,
                ShieldsEntryPoint::class.java,
            )
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.userAgentString = userAgent
                settings.textZoom = (textScale.coerceIn(0.5f, 3f) * 100).toInt()
                // Masaüstü görünüm: geniş viewport + genel bakış modu.
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = desktopMode
                applyPrivacy(this, incognito, cookiesAccepted)
                webViewClient = object : WebViewClient() {
                    private fun report(view: WebView) {
                        latestNavState(view.canGoBack(), view.canGoForward())
                    }

                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest,
                    ): WebResourceResponse? {
                        return try {
                            val cfg = latestShields
                            if (!cfg.adBlock && !cfg.trackerBlock) return null
                            val urlStr = request.url.toString()
                            if (!urlStr.startsWith("http://") && !urlStr.startsWith("https://")) {
                                return null
                            }
                            val host = request.url.host ?: return null
                            if (!entry.adBlocker().shouldBlock(host)) return null
                            val isTracker = entry.adBlocker().isTracker(host)
                            if (isTracker && !cfg.trackerBlock) return null
                            if (!isTracker && !cfg.adBlock) return null
                            entry.stats().recordBlocked(isTracker)
                            blockedResponse()
                        } catch (e: Exception) {
                            null
                        }
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest,
                    ): Boolean {
                        if (latestShields.httpsOnly && request.isForMainFrame) {
                            val upgraded = entry.httpsUpgrader().upgraded(request.url.toString())
                            if (upgraded != null) {
                                view.loadUrl(upgraded)
                                return true
                            }
                        }
                        return false
                    }

                    override fun onPageFinished(view: WebView, url: String) {
                        super.onPageFinished(view, url)
                        report(view)
                        if (!incognito) {
                            latestVisited(url, view.title.orEmpty())
                        }
                        try {
                            val cfg = latestShields
                            if (cfg.adBlock || cfg.trackerBlock) {
                                val script = entry.cosmetic()
                                    .buildScript(entry.adBlocker().cosmeticSelectors())
                                if (script.isNotEmpty()) {
                                    view.evaluateJavascript(script, null)
                                }
                            }
                        } catch (e: Exception) {
                            // Kozmetik gizleme opsiyonel; sayfayı engellemez.
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
                }
                onEngineReady(WebViewEngine(this))
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        latestProgress(newProgress)
                    }

                    override fun onShowFileChooser(
                        view: WebView,
                        filePathCallback: ValueCallback<Array<Uri>>?,
                        fileChooserParams: FileChooserParams?,
                    ): Boolean {
                        latestFileChooser(filePathCallback, fileChooserParams)
                        return true
                    }
                }
                loadUrl(url)
            }
        },
        update = { view ->
            var reloadNeeded = false
            if (view.settings.userAgentString != userAgent) {
                view.settings.userAgentString = userAgent
                view.settings.loadWithOverviewMode = desktopMode
                reloadNeeded = true
            }
            view.settings.textZoom = (textScale.coerceIn(0.5f, 3f) * 100).toInt()
            val key = PrivacyKey(incognito, cookiesAccepted, userAgent, shields, desktopMode)
            if (privacyKey != key) {
                applyPrivacy(view, incognito, cookiesAccepted)
                // Kalkan/çerez değiştiyse sayfayı kalkanlarla yeniden yükle.
                if (privacyKey.shields != key.shields || privacyKey.cookiesAccepted != key.cookiesAccepted) {
                    reloadNeeded = true
                }
                privacyKey = key
            }
            if (reloadNeeded) {
                view.reload()
            } else if (view.url != url) {
                view.loadUrl(url)
            }
        },
    )
}

/**
 * Gizlilik uygulaması: gizli modda hiçbir şey diske yazılmaz;
 * normal modda çerez anahtarı ayarlardan gelir.
 */
private fun applyPrivacy(view: WebView, incognito: Boolean, cookiesAccepted: Boolean) {
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
        cookies.setAcceptCookie(cookiesAccepted)
        runCatching {
            CookieManager.getInstance().setAcceptThirdPartyCookies(view, cookiesAccepted)
        }
        view.settings.saveFormData = true
        view.settings.databaseEnabled = true
        view.settings.cacheMode = WebSettings.LOAD_DEFAULT
    }
}

private fun blockedResponse(): WebResourceResponse =
    WebResourceResponse(
        "text/plain",
        "utf-8",
        204,
        "No Content",
        null,
        ByteArrayInputStream(ByteArray(0)),
    )
