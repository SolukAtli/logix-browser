package com.logix.browser.chromiumbridge

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.http.SslError
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val siteJs: Boolean?,
)

/** İzlenmek istemiyorum + Global Gizlilik Denetimi başlıkları. */
private fun gpcHeaders(): Map<String, String> = mapOf(
    "DNT" to "1",
    "Sec-GPC" to "1",
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
    onFindResult: (active: Int, total: Int) -> Unit = { _, _ -> },
    onPermissionRequest: (
        origin: String,
        resources: List<String>,
        decide: (granted: List<String>) -> Unit,
    ) -> Unit = { _, _, _ -> },
    onSourceLoaded: (title: String, source: String?) -> Unit = { _, _ -> },
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
    siteJsEnabled: Boolean? = null,
    siteAdBlock: Boolean? = null,
) {
    var privacyKey by remember { mutableStateOf(PrivacyKey(incognito, cookiesAccepted, userAgent, shields, desktopMode, siteJsEnabled)) }
    // İstenen URL'yi gerçekten yükledik mi? Aynı URL'yi tekrar tekrar
    // yükleyip sayfayı bozmayı engeller (yeniden çizim döngüleri).
    var lastLoaded by remember { mutableStateOf<String?>(null) }
    // Kalkan DI'ı hazır değilse sayfa kalkansız açılır, çökmez.
    val appContext = LocalContext.current.applicationContext
    val entry = remember {
        runCatching {
            EntryPointAccessors.fromApplication(appContext, ShieldsEntryPoint::class.java)
        }.getOrNull()
    }
    val latestFileChooser by rememberUpdatedState(onFileChooserRequest)
    val latestFind by rememberUpdatedState(onFindResult)
    val latestPermission by rememberUpdatedState(onPermissionRequest)
    val latestSource by rememberUpdatedState(onSourceLoaded)
    val ioScope = rememberCoroutineScope()
    val latestSiteAd by rememberUpdatedState(siteAdBlock)
    val latestNavState by rememberUpdatedState(onNavigationStateChanged)
    val latestProgress by rememberUpdatedState(onProgressChanged)
    val latestVisited by rememberUpdatedState(onPageVisited)
    val latestShields by rememberUpdatedState(shields)

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = siteJsEnabled ?: true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.userAgentString = userAgent
                settings.textZoom = (textScale.coerceIn(0.5f, 3f) * 100).toInt()
                // Masaüstü görünüm: geniş viewport + genel bakış modu.
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = desktopMode
                applyPrivacy(this, incognito, cookiesAccepted, entering = incognito, leaving = false)
                webViewClient = object : WebViewClient() {
                    private fun report(view: WebView) {
                        latestNavState(view.canGoBack(), view.canGoForward())
                    }

                    // Bekleyen SSL kararı: kullanıcı error sayfasından seçene
                    // kadar handler açık tutulur; tek karar verilir.
                    var pendingSsl: SslErrorHandler? = null
                    var pendingSslUrl: String? = null
                    var sslDecided = false

                    private fun settleSsl(proceed: Boolean, view: WebView) {
                        if (!sslDecided) {
                            sslDecided = true
                            runCatching {
                                if (proceed) pendingSsl?.proceed() else pendingSsl?.cancel()
                            }
                        }
                        pendingSsl = null
                        pendingSslUrl = null
                        if (!proceed && view.canGoBack()) view.goBack()
                    }

                    private fun openExternal(view: WebView, target: String) {
                        val opened = runCatching {
                            if (target.startsWith("intent://")) {
                                val intent = Intent.parseUri(target, Intent.URI_INTENT_SCHEME)
                                intent.addCategory(Intent.CATEGORY_BROWSABLE)
                                intent.component = null
                                intent.selector = null
                                if (intent.resolveActivity(appContext.packageManager) != null) {
                                    appContext.startActivity(
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                    )
                                    return@runCatching true
                                }
                                val fallback = NavigationRouter.intentFallbackUrl(target)
                                if (fallback != null) {
                                    view.loadUrl(fallback)
                                    return@runCatching true
                                }
                                return@runCatching false
                            }
                            val view2 = Intent(Intent.ACTION_VIEW, Uri.parse(target)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            if (view2.resolveActivity(appContext.packageManager) == null) {
                                return@runCatching false
                            }
                            appContext.startActivity(view2)
                            true
                        }.getOrDefault(false)
                        if (!opened) {
                            Toast.makeText(
                                appContext,
                                "Bu bağlantıyı açacak uygulama bulunamadı",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }

                    private fun fetchSource(view: WebView, url: String) {
                        ioScope.launch(Dispatchers.IO) {
                            val text = runCatching {
                                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                                    connectTimeout = 15_000
                                    readTimeout = 20_000
                                }
                                try {
                                    if (conn.responseCode != HttpURLConnection.HTTP_OK) return@runCatching null
                                    conn.inputStream.bufferedReader().readText().take(200_000)
                                } finally {
                                    conn.disconnect()
                                }
                            }.getOrNull()
                            withContext(Dispatchers.Main) {
                                latestSource(view.title.orEmpty(), text)
                            }
                        }
                    }

                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest,
                    ): WebResourceResponse? {
                        return try {
                            val shieldEntry = entry ?: return null
                            val cfg = latestShields
                            val adOn = latestSiteAd ?: cfg.adBlock
                            if (!adOn && !cfg.trackerBlock) return null
                            val urlStr = request.url.toString()
                            if (!urlStr.startsWith("http://") && !urlStr.startsWith("https://")) {
                                return null
                            }
                            val host = request.url.host ?: return null
                            if (!shieldEntry.adBlocker().shouldBlock(host)) return null
                            val isTracker = shieldEntry.adBlocker().isTracker(host)
                            if (isTracker && !cfg.trackerBlock) return null
                            if (!isTracker && !adOn) return null
                            shieldEntry.stats().recordBlocked(isTracker)
                            blockedResponse()
                        } catch (e: Exception) {
                            null
                        }
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest,
                    ): Boolean {
                        val target = request.url.toString()
                        // Uygulama içi SSL ekranı.
                        when (target) {
                            "logix://ssl-back" -> {
                                settleSsl(false, view)
                                return true
                            }
                            "logix://ssl-proceed" -> {
                                settleSsl(true, view)
                                return true
                            }
                        }
                        when (NavigationRouter.route(target, request.isForMainFrame)) {
                            NavigationRouter.Decision.FETCH_SOURCE -> {
                                fetchSource(view, target.removePrefix("view-source:"))
                                return true
                            }
                            NavigationRouter.Decision.EXTERNAL_APP -> {
                                openExternal(view, target)
                                return true
                            }
                            NavigationRouter.Decision.IN_WEBVIEW -> {
                                // Aşağıda HTTPS yükseltme.
                            }
                        }
                        if (!latestShields.httpsOnly || !request.isForMainFrame) return false
                        return try {
                            val upgraded = entry?.httpsUpgrader()?.upgraded(request.url.toString())
                            if (upgraded != null) {
                                view.loadUrl(upgraded)
                                true
                            } else {
                                false
                            }
                        } catch (e: Exception) {
                            false
                        }
                    }

                    override fun onPageFinished(view: WebView, url: String) {
                        super.onPageFinished(view, url)
                        report(view)
                        if (!incognito) {
                            latestVisited(url, view.title.orEmpty())
                        }
                        try {
                            val cosmeticEntry = entry
                            val cfg = latestShields
                            val adOn = latestSiteAd ?: cfg.adBlock
                            if (cosmeticEntry != null && (adOn || cfg.trackerBlock)) {
                                val script = cosmeticEntry.cosmetic()
                                    .buildScript(cosmeticEntry.adBlocker().cosmeticSelectors())
                                if (script.isNotEmpty()) {
                                    view.evaluateJavascript(script, null)
                                }
                            }
                        } catch (e: Exception) {
                            // Kozmetik gizleme opsiyonel; sayfayı engellemez.
                        }
                    }

                    override fun onPageStarted(
                        view: WebView,
                        url: String?,
                        favicon: android.graphics.Bitmap?,
                    ) {
                        // Kullanıcı başka sayfaya geçtiyse eski SSL kararı ölür.
                        if (pendingSsl != null && url != pendingSslUrl) {
                            runCatching { pendingSsl?.cancel() }
                            pendingSsl = null
                            pendingSslUrl = null
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

                    override fun onReceivedError(
                        view: WebView,
                        request: WebResourceRequest,
                        error: WebResourceError,
                    ) {
                        // Ana sayfa yüklenemediyse kilidi aç: yenileme tekrar deneyebilsin.
                        if (request.isForMainFrame) {
                            lastLoaded = null
                        }
                    }

                    override fun onReceivedSslError(
                        view: WebView,
                        handler: SslErrorHandler,
                        error: SslError,
                    ) {
                        // Önceki kararı kapat, yenisini kullanıcı seçene kadar beklet.
                        runCatching { pendingSsl?.cancel() }
                        pendingSsl = handler
                        pendingSslUrl = error.url
                        sslDecided = false
                        view.loadDataWithBaseURL(
                            error.url,
                            sslErrorHtml(error.url.orEmpty()),
                            "text/html",
                            "utf-8",
                            null,
                        )
                    }
                }
                onEngineReady(WebViewEngine(this))
                setFindListener { activeMatchOrdinal, numberOfMatches, _ ->
                    latestFind(activeMatchOrdinal, numberOfMatches)
                }
                setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                    runCatching {
                        val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
                        val request = DownloadManager.Request(Uri.parse(url)).apply {
                            addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url))
                            addRequestHeader("User-Agent", userAgent)
                            setTitle(fileName)
                            setMimeType(mimeType)
                            setNotificationVisibility(
                                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED,
                            )
                            setDestinationInExternalPublicDir(
                                Environment.DIRECTORY_DOWNLOADS,
                                fileName,
                            )
                            allowScanningByMediaScanner()
                        }
                        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                        manager.enqueue(request)
                    }
                }
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

                    override fun onPermissionRequest(request: PermissionRequest) {
                        val origin = request.origin.toString()
                        val resources = request.resources.toList()
                        latestPermission(origin, resources) { granted ->
                            runCatching {
                                if (granted.isNotEmpty()) request.grant(granted.toTypedArray())
                                else request.deny()
                            }
                        }
                    }

                    override fun onPermissionRequestCanceled(request: PermissionRequest) {
                        latestPermission(request.origin.toString(), emptyList()) { }
                    }
                }
                loadUrl(url, gpcHeaders())
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
            val key = PrivacyKey(incognito, cookiesAccepted, userAgent, shields, desktopMode, siteJsEnabled)
            if (privacyKey != key) {
                val entering = incognito && !privacyKey.incognito
                val leaving = !incognito && privacyKey.incognito
                applyPrivacy(view, incognito, cookiesAccepted, entering, leaving)
                if (view.settings.javaScriptEnabled != (siteJsEnabled ?: true)) {
                    view.settings.javaScriptEnabled = siteJsEnabled ?: true
                    reloadNeeded = true
                }
                // Kalkan/çerez değiştiyse sayfayı kalkanlarla yeniden yükle.
                if (privacyKey.shields != key.shields || privacyKey.cookiesAccepted != key.cookiesAccepted) {
                    reloadNeeded = true
                }
                privacyKey = key
            }
            if (reloadNeeded) {
                view.reload()
            } else if (view.url != url && lastLoaded != url) {
                lastLoaded = url
                view.loadUrl(url, gpcHeaders())
            }
        },
    )
}

/**
 * Gizlilik uygulaması. Aynı WebView profili paylaşıldığı için gerçek
 * izolasyon, gizli moda girerken VE çıkarken çerez + site verisini
 * silmekle sağlanır. Normal modda çerez anahtarı ayarlardan gelir.
 */
private fun applyPrivacy(
    view: WebView,
    incognito: Boolean,
    cookiesAccepted: Boolean,
    entering: Boolean,
    leaving: Boolean,
) {
    runCatching {
        val cookies = CookieManager.getInstance()
        if (entering || leaving) {
            // Gizli oturum artığı normal profile, normal profil gizliye taşınmasın.
            runCatching {
                cookies.removeAllCookies(null)
                cookies.flush()
            }
            runCatching { WebStorage.getInstance().deleteAllData() }
            view.clearCache(true)
            view.clearFormData()
        }
        if (incognito) {
            view.clearHistory()
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

private fun sslErrorHtml(url: String): String {
    val safe = url.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    return """
        <html><head><meta name="viewport" content="width=device-width, initial-scale=1">
        <style>body{font-family:sans-serif;background:#1a1b26;color:#fff;text-align:center;padding:48px 24px}
        h1{font-size:22px}.url{color:#b9b9d6;word-break:break-all;margin:12px 0 24px}
        a{display:block;margin:10px auto;padding:14px;border-radius:16px;text-decoration:none;max-width:320px}
        .back{background:#232433;color:#fff}.go{background:#b3261e;color:#fff}</style></head>
        <body><h1>⚠️ Bağlantı güvenli değil</h1>
        <div class="url">$safe</div>
        <p>Sitenin güvenlik sertifikası doğrulanamadı. Bilgilerin çalınabilir.</p>
        <a class="back" href="logix://ssl-back">Geri dön (önerilir)</a>
        <a class="go" href="logix://ssl-proceed">Riski anlıyorum, yine de devam et</a>
        </body></html>
    """.trimIndent()
}
