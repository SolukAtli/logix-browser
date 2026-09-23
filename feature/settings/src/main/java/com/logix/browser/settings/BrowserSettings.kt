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
    val filterAutoUpdate: Boolean = true,
    val filterUpdatedAt: Long = 0L,
    val accent: String = "blue",
    val amoled: Boolean = false,
    val cookiesAccepted: Boolean = true,
    val httpsOnly: Boolean = false,
    val deathResetEnabled: Boolean = false,
    val deathResetDays: Int = 30,
    val lastActiveAt: Long = 0L,
    val barPosition: String = "top",
    val userAgent: String = "mobile",
    val textScale: Float = 1f,
    val incognito: Boolean = false,
)
