package com.logix.browser.search

import javax.inject.Inject

/**
 * Resolves raw omnibox input into either a direct URL or a search request,
 * mirroring the shared web UI behaviour (URL vs. query detection).
 */
sealed interface ResolvedInput {
    data class OpenUrl(val url: String) : ResolvedInput
    data class Search(val query: String, val searchUrl: String) : ResolvedInput
}

class OmniboxResolver @Inject constructor() {

    fun resolve(rawInput: String, engine: SearchEngine): ResolvedInput? {
        val input = rawInput.trim()
        if (input.isEmpty()) return null
        if (HAS_SCHEME_REGEX.containsMatchIn(input)) {
            return ResolvedInput.OpenUrl(input)
        }
        if (LOOKS_LIKE_DOMAIN_REGEX.containsMatchIn(input) && !input.contains(" ")) {
            return ResolvedInput.OpenUrl("https://$input")
        }
        return ResolvedInput.Search(query = input, searchUrl = engine.buildSearchUrl(input))
    }

    private companion object {
        val HAS_SCHEME_REGEX = Regex("^(https?|ftp)://", RegexOption.IGNORE_CASE)
        val LOOKS_LIKE_DOMAIN_REGEX = Regex("^[a-z0-9]+\\.[a-z]{2,}", RegexOption.IGNORE_CASE)
    }
}
