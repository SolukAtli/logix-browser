package com.logix.browser.tabs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the LRU admission policy: at most [TabPool.MAX_LIVE_ENGINES]
 * live tabs, least-recently-used evicted first.
 */
class TabPoolTest {

    @Test
    fun `up to three tabs stay live without eviction`() {
        val pool = TabPool()

        assertTrue(pool.accessed("a").isEmpty())
        assertTrue(pool.accessed("b").isEmpty())
        assertTrue(pool.accessed("c").isEmpty())

        assertEquals(setOf("a", "b", "c"), pool.liveIds.value)
    }

    @Test
    fun `fourth tab evicts least recently used`() {
        val pool = TabPool()
        pool.accessed("a")
        pool.accessed("b")
        pool.accessed("c")

        val evicted = pool.accessed("d")

        assertEquals(listOf("a"), evicted)
        assertEquals(setOf("b", "c", "d"), pool.liveIds.value)
        assertFalse(pool.isLive("a"))
    }

    @Test
    fun `re-access refreshes recency`() {
        val pool = TabPool()
        pool.accessed("a")
        pool.accessed("b")
        pool.accessed("c")
        pool.accessed("a") // a is MRU now; b is LRU

        val evicted = pool.accessed("d")

        assertEquals(listOf("b"), evicted)
        assertTrue(pool.isLive("a"))
    }

    @Test
    fun `remove drops tab without evicting others`() {
        val pool = TabPool()
        pool.accessed("a")
        pool.accessed("b")

        pool.remove("a")

        assertEquals(setOf("b"), pool.liveIds.value)
        assertTrue(pool.accessed("c").isEmpty())
        assertTrue(pool.accessed("d").isEmpty())
        assertEquals(setOf("b", "c", "d"), pool.liveIds.value)
    }

    @Test
    fun `freezeAllExcept keeps only active tab`() {
        val pool = TabPool()
        pool.accessed("a")
        pool.accessed("b")
        pool.accessed("c")

        val frozen = pool.freezeAllExcept("b")

        assertEquals(setOf("a", "c"), frozen.toSet())
        assertEquals(setOf("b"), pool.liveIds.value)
    }

    @Test
    fun `freezeAllExcept with null freezes everything`() {
        val pool = TabPool()
        pool.accessed("a")
        pool.accessed("b")

        val frozen = pool.freezeAllExcept(null)

        assertEquals(setOf("a", "b"), frozen.toSet())
        assertTrue(pool.liveIds.value.isEmpty())
    }
}
