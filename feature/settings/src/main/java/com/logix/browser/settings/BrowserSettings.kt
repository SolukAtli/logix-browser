package com.logix.browser.settings

/**
 * Snapshot of user-visible browser settings (Faz-1 subset).
 */
data class BrowserSettings(
    val theme: String = "dark",
    val searchEngineKey: String = "google",
    val adBlockEnabled: Boolean = true,
    val trackerBlockEnabled: Boolean = true,
    val desktopSite: Boolean = false,
)
