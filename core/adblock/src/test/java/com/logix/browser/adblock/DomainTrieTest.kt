package com.logix.browser.adblock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainTrieTest {

    @Test
    fun `apex and subdomains match`() {
        val trie = DomainTrie.of(listOf("example.com"))

        assertTrue(trie.matches("example.com"))
        assertTrue(trie.matches("sub.example.com"))
        assertTrue(trie.matches("a.b.example.com"))
    }

    @Test
    fun `partial labels do not match`() {
        val trie = DomainTrie.of(listOf("example.com"))

        assertFalse(trie.matches("notexample.com"))
        assertFalse(trie.matches("example.com.evil.com"))
        assertFalse(trie.matches("com"))
        assertFalse(trie.matches(""))
    }

    @Test
    fun `matching is case-insensitive`() {
        val trie = DomainTrie.of(listOf("Example.COM"))

        assertTrue(trie.matches("SUB.EXAMPLE.com"))
    }

    @Test
    fun `clear empties the trie`() {
        val trie = DomainTrie.of(listOf("example.com"))
        trie.clear()

        assertFalse(trie.matches("example.com"))
    }
}
