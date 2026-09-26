package com.logix.browser.chromiumbridge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlSafetyTest {

    @Test
    fun `http ve https gecer`() {
        assertTrue(UrlSafety.isHttp("http://example.com"))
        assertTrue(UrlSafety.isHttp("https://example.com/x?q=1"))
        assertTrue(UrlSafety.isHttp("HTTP://EXAMPLE.COM"))
    }

    @Test
    fun `sahte semalar reddedilir`() {
        assertFalse(UrlSafety.isHttp("httpx://evil.com"))
        assertFalse(UrlSafety.isHttp("httpfoo:bar"))
        assertFalse(UrlSafety.isHttp("https-malformed:"))
        assertFalse(UrlSafety.isHttp("javascript:alert(1)"))
        assertFalse(UrlSafety.isHttp("snssdk1233://aweme/detail/1"))
        assertFalse(UrlSafety.isHttp(""))
    }
}
