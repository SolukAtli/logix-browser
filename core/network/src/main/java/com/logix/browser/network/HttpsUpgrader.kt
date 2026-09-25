package com.logix.browser.network

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Upgrades plain-HTTP navigations to HTTPS (HTTPS-Only behaviour).
 * Local development hosts, IP literals and non-HTTP schemes are left alone.
 */
@Singleton
class HttpsUpgrader @Inject constructor() {

    /**
     * Returns the HTTPS replacement for [url], or null when no upgrade applies.
     */
    fun upgraded(url: String): String? {
        if (!url.startsWith("http://")) return null
        val parsed = runCatching { android.net.Uri.parse(url) }.getOrNull() ?: return null
        val host = (parsed.host ?: return null).lowercase()
        if (host.isEmpty() || isLocalHost(host) || isIpLiteral(host)) return null
        return "https://" + url.removePrefix("http://")
    }

    private fun isLocalHost(host: String): Boolean =
        host == "localhost" || host.endsWith(".localhost") || host.endsWith(".local")

    private fun isIpLiteral(host: String): Boolean {
        if (host.startsWith("[") && host.endsWith("]")) return true // IPv6
        val v4 = host.split(".")
        if (v4.size == 4 && v4.all { part -> part.toIntOrNull()?.let { it in 0..255 } == true }) {
            return true
        }
        return false
    }
}
