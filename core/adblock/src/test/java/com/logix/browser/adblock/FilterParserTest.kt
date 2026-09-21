package com.logix.browser.adblock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterParserTest {

    @Test
    fun `parses easylist style network rules`() {
        val parsed = FilterParser.parse(
            """
            [Adblock Plus 2.0]
            ! comment line
            ||doubleclick.net^
            ||sub.example.com^${'$'}third-party
            @@||allow.example.com^
            """.trimIndent(),
        )

        assertEquals(setOf("doubleclick.net", "sub.example.com"), parsed.blockedHosts)
        assertEquals(setOf("allow.example.com"), parsed.allowlistedHosts)
        assertTrue(parsed.cosmeticSelectors.isEmpty())
    }

    @Test
    fun `collects cosmetic selectors and skips regex rules`() {
        val parsed = FilterParser.parse(
            """
            example.com##.ad-banner
            ##.sponsored-box
            /ads/banner\d+\.js/
            ||cdn.example.com/ads/
            """.trimIndent(),
        )

        assertEquals(listOf(".ad-banner", ".sponsored-box"), parsed.cosmeticSelectors)
        assertEquals(setOf("cdn.example.com"), parsed.blockedHosts)
    }

    @Test
    fun `blocker honors allowlist over blocklist`() {
        val blocker = DomainAdBlocker()
        blocker.loadFilterText("||example.com^\n@@||safe.example.com^\n")

        assertTrue(blocker.shouldBlock("example.com"))
        assertTrue(blocker.shouldBlock("a.example.com"))
        assertTrue(!blocker.shouldBlock("safe.example.com"))
        assertTrue(!blocker.shouldBlock("other.com"))
    }

    @Test
    fun `seed lists block known hosts`() {
        val blocker = DomainAdBlocker()

        assertTrue(blocker.shouldBlock("doubleclick.net"))
        assertTrue(blocker.shouldBlock("stats.hotjar.com"))
        assertTrue(blocker.isTracker("stats.hotjar.com"))
        assertTrue(!blocker.isTracker("doubleclick.net"))
    }

    @Test
    fun `cosmetic script embeds selectors`() {
        val script = CosmeticFilter().buildScript(listOf(".ad", "#banner"))

        assertTrue(script.contains(".ad"))
        assertTrue(script.contains("#banner"))
        assertTrue(script.contains("MutationObserver"))
        assertEquals("", CosmeticFilter().buildScript(emptyList()))
    }
}
