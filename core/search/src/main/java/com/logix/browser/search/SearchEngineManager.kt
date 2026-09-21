package com.logix.browser.search

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the available engines and the currently selected one.
 * Persisting the selection is a Faz-2 concern (database-backed).
 */
@Singleton
class SearchEngineManager @Inject constructor() {

    val available: List<SearchEngine> = DefaultSearchEngines.ALL

    private val _selected = MutableStateFlow(DefaultSearchEngines.GOOGLE)
    val selected: StateFlow<SearchEngine> = _selected.asStateFlow()

    fun select(id: SearchEngineId) {
        _selected.value = DefaultSearchEngines.byId(id)
    }

    fun selectByKey(key: String) {
        _selected.value = DefaultSearchEngines.byKey(key)
    }

    fun searchUrlFor(query: String): String = _selected.value.buildSearchUrl(query)
}
