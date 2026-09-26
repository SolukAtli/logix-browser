package com.logix.browser.adblock

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decides whether a request host must be blocked. Matching is trie-backed
 * (label-boundary suffix match, O(labels)) with allowlist precedence.
 */
interface AdBlocker {
    fun shouldBlock(host: String): Boolean
}

@Singleton
class DomainAdBlocker @Inject constructor() : AdBlocker {

    private val lock = Any()

    private var adHosts: Set<String> = DEFAULT_AD_DOMAINS
    private var trackerHosts: Set<String> = DEFAULT_TRACKER_DOMAINS
    private var allowHosts: Set<String> = emptySet()
    private var selectors: List<String> = emptyList()

    private var blockedTrie: DomainTrie = rebuildBlocked()
    private var allowTrie: DomainTrie = DomainTrie.of(allowHosts)
    private var trackerTrie: DomainTrie = DomainTrie.of(trackerHosts)

    /**
     * Replaces the in-memory lists (e.g. after downloading fresh copies).
     */
    fun updateLists(adDomains: Set<String>, trackerDomains: Set<String>) {
        synchronized(lock) {
            adHosts = adDomains.map { it.lowercase() }.toSet()
            trackerHosts = trackerDomains.map { it.lowercase() }.toSet()
            rebuildLocked()
        }
    }

    /**
     * Merges an EasyList/AdGuard-format filter text into the active lists.
     * Cosmetic selectors accumulate (bounded by the parser).
     */
    fun loadFilterText(text: String) {
        val parsed = FilterParser.parse(text)
        synchronized(lock) {
            adHosts = adHosts + parsed.blockedHosts
            allowHosts = allowHosts + parsed.allowlistedHosts
            selectors = (selectors + parsed.cosmeticSelectors).distinct()
            rebuildLocked()
        }
    }

    /**
     * Atomik liste değişimi: indirme + parse staging'de biter, burası
     * eski listeyi tek seferde yenisiyle değiştirir (stale kural kalmaz).
     */
    fun replaceAll(
        adDomains: Set<String>,
        trackerDomains: Set<String>,
        allowlisted: Set<String>,
        cosmetic: List<String>,
    ) {
        synchronized(lock) {
            adHosts = adDomains.map { it.lowercase() }.toSet()
            trackerHosts = trackerDomains.map { it.lowercase() }.toSet()
            allowHosts = allowlisted.map { it.lowercase() }.toSet()
            selectors = cosmetic.distinct()
            rebuildLocked()
        }
    }

    override fun shouldBlock(host: String): Boolean {
        val h = host.lowercase()
        if (h.isEmpty()) return false
        synchronized(lock) {
            if (allowTrie.matches(h)) return false
            return blockedTrie.matches(h)
        }
    }

    /** True when [host] belongs to a known tracker (for stats labelling). */
    fun isTracker(host: String): Boolean {
        val h = host.lowercase()
        if (h.isEmpty()) return false
        synchronized(lock) {
            return trackerTrie.matches(h)
        }
    }

    fun cosmeticSelectors(): List<String> = synchronized(lock) { selectors }

    private fun rebuildBlocked(): DomainTrie = DomainTrie.of(adHosts + trackerHosts)

    private fun rebuildLocked() {
        blockedTrie = rebuildBlocked()
        allowTrie = DomainTrie.of(allowHosts)
        trackerTrie = DomainTrie.of(trackerHosts)
    }

    companion object {
        /** Minimal seed lists; full lists arrive over the update path. */
        val DEFAULT_AD_DOMAINS: Set<String> = setOf(
            "doubleclick.net",
            "googlesyndication.com",
            "googleadservices.com",
            "facebook.net",
        )
        val DEFAULT_TRACKER_DOMAINS: Set<String> = setOf(
            "hotjar.com",
            "mixpanel.com",
        )

        /** Kept for compatibility; equals ads + trackers. */
        val DEFAULT_BLOCKED_DOMAINS: Set<String> = DEFAULT_AD_DOMAINS + DEFAULT_TRACKER_DOMAINS
    }
}
