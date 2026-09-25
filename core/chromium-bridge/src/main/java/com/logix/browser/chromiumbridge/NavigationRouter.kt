package com.logix.browser.chromiumbridge

/**
 * WebView navigasyon router'ı: saf Kotlin, Android framework bağımlılığı
 * yok — birim testleriyle kapsanır.
 */
object NavigationRouter {

    enum class Decision {
        /** WebView kendisi yükler (http/https). */
        IN_WEBVIEW,

        /** Sistem uygulaması açılır (harici intent). */
        EXTERNAL_APP,

        /** Kaynak metin olarak indirilip uygulama içinde gösterilir. */
        FETCH_SOURCE,
    }

    fun route(url: String, isMainFrame: Boolean): Decision {
        val scheme = url.substringBefore("://").substringBefore(":").lowercase()
        return when {
            url.startsWith("logix://") -> Decision.IN_WEBVIEW
            url.startsWith("view-source:") -> Decision.FETCH_SOURCE
            scheme == "http" || scheme == "https" -> Decision.IN_WEBVIEW
            !isMainFrame -> Decision.IN_WEBVIEW
            else -> Decision.EXTERNAL_APP
        }
    }

    /** intent:// URL'inden web fallback adresi çıkarır (yoksa null). */
    fun intentFallbackUrl(intentUri: String): String? {
        val marker = "S.browser_fallback_url="
        val start = intentUri.indexOf(marker)
        if (start < 0) return null
        val encoded = intentUri.substring(start + marker.length)
            .substringBefore(";")
            .substringBefore(" ")
        return runCatching {
            java.net.URLDecoder.decode(encoded, "UTF-8")
        }.getOrNull()?.takeIf { it.startsWith("http") }
    }
}
