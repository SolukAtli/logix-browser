package com.logix.browser.adblock

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decides whether a request host must be blocked. Matching mirrors the
 * legacy WebView interceptor: exact host or any subdomain.
 */
interface AdBlocker {
    fun shouldBlock(host: String): Boolean
}

@Singleton
class DomainAdBlocker @Inject constructor() : AdBlocker {

    @Volatile
    private var blocked: Set<String> = DEFAULT_BLOCKED_DOMAINS

    /**
     * Replaces the in-memory lists (e.g. after downloading fresh copies).
     */
    fun updateLists(adDomains: Set<String>, trackerDomains: Set<String>) {
        blocked = (adDomains + trackerDomains).map { it.lowercase() }.toSet()
    }

    override fun shouldBlock(host: String): Boolean {
        val h = host.lowercase()
        if (h.isEmpty()) return false
        return blocked.any { domain -> h == domain || h.endsWith(".$domain") }
    }

    companion object {
        /** Minimal Faz-1 seed list; full lists arrive over the update path. */
        val DEFAULT_BLOCKED_DOMAINS: Set<String> = setOf(
            "doubleclick.net",
            "googlesyndication.com",
            "googleadservices.com",
            "facebook.net",
            "hotjar.com",
            "mixpanel.com",
        )
    }
}
