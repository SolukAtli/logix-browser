package com.logix.browser.network

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Navigation-safety verdicts. Full Safe Browsing lookup is a Faz-3.1 item;
 * until then [NoopSafeBrowsingClient] keeps the call sites compiling and
 * the DI graph bound.
 */
enum class SafetyVerdict {
    SAFE,
    PHISHING,
    MALWARE,
    UNWANTED,
}

interface SafeBrowsingClient {
    suspend fun checkUrl(url: String): SafetyVerdict
}

@Singleton
class NoopSafeBrowsingClient @Inject constructor() : SafeBrowsingClient {
    override suspend fun checkUrl(url: String): SafetyVerdict = SafetyVerdict.SAFE
}
