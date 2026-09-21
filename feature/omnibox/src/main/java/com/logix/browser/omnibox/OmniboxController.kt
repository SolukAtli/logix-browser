package com.logix.browser.omnibox

import com.logix.browser.search.OmniboxResolver
import com.logix.browser.search.ResolvedInput
import com.logix.browser.search.SearchEngineManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridges the omnibox UI and [OmniboxResolver]: returns the final URL
 * the engine must load, or null when the input is empty.
 */
@Singleton
class OmniboxController @Inject constructor(
    private val resolver: OmniboxResolver,
    private val engines: SearchEngineManager,
) {
    fun resolveToUrl(input: String): String? =
        when (val resolved = resolver.resolve(input, engines.selected.value)) {
            is ResolvedInput.OpenUrl -> resolved.url
            is ResolvedInput.Search -> resolved.searchUrl
            null -> null
        }
}
