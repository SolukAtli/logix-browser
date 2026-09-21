package com.logix.browser.adblock

/**
 * Reversed-label domain trie: inserting `example.com` matches the apex and
 * every subdomain (`sub.example.com`) but never partial labels
 * (`notexample.com`). Lookups walk at most the label count — well under 1ms.
 */
class DomainTrie {

    private class Node {
        val children = HashMap<String, Node>()
        var terminal = false
    }

    private val root = Node()

    fun insert(domain: String) {
        val labels = domain.lowercase().split(".")
        if (labels.any { it.isEmpty() }) return
        var node = root
        for (i in labels.indices.reversed()) {
            node = node.children.getOrPut(labels[i]) { Node() }
        }
        node.terminal = true
    }

    fun matches(host: String): Boolean {
        val labels = host.lowercase().split(".")
        if (labels.any { it.isEmpty() }) return false
        var node = root
        for (i in labels.indices.reversed()) {
            if (node.terminal) return true
            node = node.children[labels[i]] ?: return false
        }
        return node.terminal
    }

    fun clear() {
        root.children.clear()
        root.terminal = false
    }

    companion object {
        fun of(domains: Collection<String>): DomainTrie {
            val trie = DomainTrie()
            domains.forEach(trie::insert)
            return trie
        }
    }
}
