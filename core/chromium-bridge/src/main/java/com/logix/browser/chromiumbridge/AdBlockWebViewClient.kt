package com.logix.browser.chromiumbridge

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.logix.browser.adblock.AdBlockStatsRepository
import com.logix.browser.adblock.CosmeticFilter
import com.logix.browser.adblock.DomainAdBlocker
import com.logix.browser.network.HttpsUpgrader
import java.io.ByteArrayInputStream
import javax.inject.Inject

/**
 * WebViewClient that enforces the Faz-3 shields:
 * - network blocking (known ad/tracker/malware hosts → 204 No Content),
 * - HTTPS upgrade for main-frame navigations,
 * - cosmetic hiding of leftover ad slots after page load.
 *
 * Blocking decisions stay synchronous (trie lookup, sub-millisecond);
 * stats writes are fire-and-forget inside [AdBlockStatsRepository].
 */
class AdBlockWebViewClient @Inject constructor(
    private val adBlocker: DomainAdBlocker,
    private val stats: AdBlockStatsRepository,
    private val httpsUpgrader: HttpsUpgrader,
    private val cosmetic: CosmeticFilter,
) : WebViewClient() {

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest,
    ): WebResourceResponse? {
        val url = request.url.toString()
        if (!url.startsWith("http://") && !url.startsWith("https://")) return null
        val host = request.url.host ?: return null
        if (adBlocker.shouldBlock(host)) {
            stats.recordBlocked(adBlocker.isTracker(host))
            return blockedResponse()
        }
        return null
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val upgraded = httpsUpgrader.upgraded(request.url.toString())
        return if (upgraded != null && request.isForMainFrame) {
            view.loadUrl(upgraded)
            true
        } else {
            false
        }
    }

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        val script = cosmetic.buildScript(adBlocker.cosmeticSelectors())
        if (script.isNotEmpty()) {
            view.evaluateJavascript(script, null)
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
}
