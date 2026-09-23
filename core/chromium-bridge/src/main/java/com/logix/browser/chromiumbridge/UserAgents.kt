package com.logix.browser.chromiumbridge

/**
 * selectable WebView user-agent profiles (settings key → UA string).
 */
object UserAgents {

    const val MOBILE_KEY = "mobile"
    const val DESKTOP_KEY = "desktop"
    const val SAFARI_KEY = "safari"

    const val MOBILE: String =
        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"

    const val DESKTOP: String =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

    const val SAFARI: String =
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) " +
            "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 " +
            "Mobile/15E148 Safari/604.1"

    fun forKey(key: String): String = when (key) {
        DESKTOP_KEY -> DESKTOP
        SAFARI_KEY -> SAFARI
        else -> MOBILE
    }
}
