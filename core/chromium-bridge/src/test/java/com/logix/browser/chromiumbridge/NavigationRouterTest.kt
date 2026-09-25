package com.logix.browser.chromiumbridge

import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationRouterTest {

    @Test
    fun `http ve https webviewde acilir`() {
        assertEquals(
            NavigationRouter.Decision.IN_WEBVIEW,
            NavigationRouter.route("https://example.com", true),
        )
        assertEquals(
            NavigationRouter.Decision.IN_WEBVIEW,
            NavigationRouter.route("http://example.com", true),
        )
    }

    @Test
    fun `tiktok ve ozel semalar disari gider`() {
        assertEquals(
            NavigationRouter.Decision.EXTERNAL_APP,
            NavigationRouter.route("snssdk1233://aweme/detail/123", true),
        )
        assertEquals(
            NavigationRouter.Decision.EXTERNAL_APP,
            NavigationRouter.route("mailto:a@b.com", true),
        )
        assertEquals(
            NavigationRouter.Decision.EXTERNAL_APP,
            NavigationRouter.route("tel:+90111", true),
        )
        assertEquals(
            NavigationRouter.Decision.EXTERNAL_APP,
            NavigationRouter.route("intent://x#Intent;scheme=y;end", true),
        )
    }

    @Test
    fun `view-source kaynak olarak islenir`() {
        assertEquals(
            NavigationRouter.Decision.FETCH_SOURCE,
            NavigationRouter.route("view-source:https://example.com", true),
        )
    }

    @Test
    fun `iframe ici ozel sema webviewde birakilir`() {
        assertEquals(
            NavigationRouter.Decision.IN_WEBVIEW,
            NavigationRouter.route("snssdk1233://x", false),
        )
    }

    @Test
    fun `intent fallback adresi cozulur`() {
        val fallback = NavigationRouter.intentFallbackUrl(
            "intent://x#Intent;scheme=https;S.browser_fallback_url=https%3A%2F%2Fexample.com%2Fy;end",
        )
        assertEquals("https://example.com/y", fallback)
    }
}
