package com.logix.browser.chromiumbridge

/**
 * Kesin URI doğrulamaları. `startsWith("http")` kalıbı `httpx://`,
 * `httpfoo:` gibi değerleri geçirir; burada şema tam eşleşir.
 */
object UrlSafety {

    fun isHttp(url: String): Boolean {
        val scheme = url.substringBefore(":").lowercase()
        return scheme == "http" || scheme == "https"
    }

    fun hostOf(url: String): String = runCatching {
        android.net.Uri.parse(url).host.orEmpty()
    }.getOrDefault("")

    fun encode(value: String): String = runCatching {
        java.net.URLEncoder.encode(value, "UTF-8")
    }.getOrDefault(value)
}
