package com.logix.browser.adblock

/**
 * Parses the widely-used subset of EasyList / AdGuard rule syntax:
 * - `||host^...` network block, `@@||host^...` exception (allowlist)
 * - `! comment` and `[Adblock ...]` headers are skipped
 * - `domain##selector` cosmetic rules are collected separately
 * - `|https://host/...` absolute-URL prefixes reduce to their host
 *
 * Anything more expressive (regex, scriptlet injections, `$` modifiers
 * beyond host anchoring) is intentionally out of scope for Faz-3.
 */
object FilterParser {

    data class ParsedFilters(
        val blockedHosts: Set<String>,
        val allowlistedHosts: Set<String>,
        val cosmeticSelectors: List<String>,
    )

    fun parse(text: String): ParsedFilters = parse(text.lineSequence())

    fun parse(lines: Sequence<String>): ParsedFilters {
        val blocked = LinkedHashSet<String>()
        val allowlisted = LinkedHashSet<String>()
        val cosmetic = ArrayList<String>()
        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("!") || line.startsWith("[")) continue
            if ("##" in line && !line.startsWith("@@")) {
                val selector = line.substringAfter("##").trim()
                if (selector.isNotEmpty() && cosmetic.size < MAX_COSMETIC) {
                    cosmetic += selector
                }
                continue
            }
            if (line.startsWith("#@#") || line.startsWith("#?#") || line.startsWith("#\$#")) continue
            var rule = line
            var isAllow = false
            if (rule.startsWith("@@")) {
                isAllow = true
                rule = rule.removePrefix("@@")
            }
            val host = extractHost(rule) ?: continue
            if (isAllow) allowlisted += host else blocked += host
        }
        return ParsedFilters(blocked, allowlisted, cosmetic)
    }

    private fun extractHost(rule: String): String? {
        var candidate = rule.substringBefore("$")
        if (candidate.startsWith("||")) {
            candidate = candidate.removePrefix("||")
            val end = candidate.indexOfFirst { it == '^' || it == '/' }
            candidate = if (end >= 0) candidate.substring(0, end) else candidate
        } else if (candidate.startsWith("|")) {
            candidate = candidate.removePrefix("|")
            if (!candidate.startsWith("http")) return null
            candidate = hostOfUrl(candidate) ?: return null
        } else {
            // Bare patterns: accept host-only entries, reject regex/substring rules.
            if (candidate.any { it == '*' || it == '/' || it == ' ' || it == '|' || it == '^' }) return null
            if (!candidate.contains('.')) return null
        }
        candidate = candidate.trim().lowercase().removePrefix("*.").trimEnd('.')
        if (candidate.isEmpty() || !candidate.contains('.')) return null
        if (candidate.any { it == ' ' || it == '*' || it == '/' || it == '^' || it == '|' }) return null
        return candidate
    }

    private fun hostOfUrl(url: String): String? {
        val withoutScheme = url.substringAfter("://", "")
        if (withoutScheme.isEmpty()) return null
        return withoutScheme.substringBefore("/").substringBefore(":").lowercase()
            .takeIf { it.contains('.') }
    }

    private const val MAX_COSMETIC = 5_000
}
