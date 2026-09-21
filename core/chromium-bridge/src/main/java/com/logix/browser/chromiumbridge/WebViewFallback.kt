package com.logix.browser.chromiumbridge

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Temporary Faz-1 wrapper: hosts a system WebView inside Compose so UI tests
 * and the Phase-1 shell have real content. The [Engine] interface stays
 * stable when the real Chromium embedding lands.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ContentViewHost(
    url: String,
    modifier: Modifier = Modifier,
    onEngineReady: (Engine) -> Unit = {},
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = WebViewClient()
                onEngineReady(WebViewEngine(this))
                loadUrl(url)
            }
        },
        update = { view ->
            if (view.url != url) {
                view.loadUrl(url)
            }
        },
    )
}
