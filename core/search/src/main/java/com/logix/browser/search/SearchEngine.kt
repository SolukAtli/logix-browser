package com.logix.browser.search

/**
 * Supported search engines with their URL templates.
 * `{query}` is replaced with the URL-encoded query string.
 */
enum class SearchEngineId(val key: String) {
    GOOGLE("google"),
    BING("bing"),
    DUCKDUCKGO("duckduckgo"),
    YANDEX("yandex"),
    BRAVE("brave"),
}

data class SearchEngine(
    val id: SearchEngineId,
    val displayName: String,
    val homepageUrl: String,
    val searchUrlTemplate: String,
    val suggestUrlTemplate: String? = null,
) {
    fun buildSearchUrl(query: String): String =
        searchUrlTemplate.replace(
            "{query}",
            java.net.URLEncoder.encode(query, Charsets.UTF_8.name()),
        )

    fun buildSuggestUrl(query: String): String? =
        suggestUrlTemplate?.replace(
            "{query}",
            java.net.URLEncoder.encode(query, Charsets.UTF_8.name()),
        )
}

object DefaultSearchEngines {
    val GOOGLE = SearchEngine(
        id = SearchEngineId.GOOGLE,
        displayName = "Google",
        homepageUrl = "https://www.google.com",
        searchUrlTemplate = "https://www.google.com/search?q={query}",
        suggestUrlTemplate = "https://suggestqueries.google.com/complete/search?client=firefox&q={query}",
    )
    val BING = SearchEngine(
        id = SearchEngineId.BING,
        displayName = "Bing",
        homepageUrl = "https://www.bing.com",
        searchUrlTemplate = "https://www.bing.com/search?q={query}",
        suggestUrlTemplate = "https://api.bing.com/osjson.aspx?query={query}",
    )
    val DUCKDUCKGO = SearchEngine(
        id = SearchEngineId.DUCKDUCKGO,
        displayName = "DuckDuckGo",
        homepageUrl = "https://duckduckgo.com",
        searchUrlTemplate = "https://duckduckgo.com/?q={query}",
        suggestUrlTemplate = "https://duckduckgo.com/ac/?q={query}",
    )
    val YANDEX = SearchEngine(
        id = SearchEngineId.YANDEX,
        displayName = "Yandex",
        homepageUrl = "https://yandex.com",
        searchUrlTemplate = "https://yandex.com/search/?text={query}",
    )
    val BRAVE = SearchEngine(
        id = SearchEngineId.BRAVE,
        displayName = "Brave",
        homepageUrl = "https://search.brave.com",
        searchUrlTemplate = "https://search.brave.com/search?q={query}",
        suggestUrlTemplate = "https://search.brave.com/api/suggest?q={query}",
    )

    val ALL: List<SearchEngine> = listOf(GOOGLE, BING, DUCKDUCKGO, YANDEX, BRAVE)

    fun byId(id: SearchEngineId): SearchEngine = ALL.first { it.id == id }

    fun byKey(key: String): SearchEngine =
        ALL.firstOrNull { it.id.key == key } ?: GOOGLE
}
