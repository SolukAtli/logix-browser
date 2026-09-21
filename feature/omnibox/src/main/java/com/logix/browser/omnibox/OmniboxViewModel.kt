package com.logix.browser.omnibox

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.logix.browser.search.SearchEngine
import com.logix.browser.search.SearchEngineId
import com.logix.browser.search.SearchEngineManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Owns omnibox text + selected engine; delegates URL resolution to
 * [OmniboxController] (which uses [com.logix.browser.search.OmniboxResolver]).
 */
@HiltViewModel
class OmniboxViewModel @Inject constructor(
    private val controller: OmniboxController,
    private val engines: SearchEngineManager,
) : ViewModel() {

    var query by mutableStateOf("")
        private set

    val availableEngines: List<SearchEngine> = engines.available
    val selectedEngine = engines.selected

    fun onQueryChange(value: String) {
        query = value
    }

    fun clearQuery() {
        query = ""
    }

    fun selectEngine(id: SearchEngineId) = engines.select(id)

    /** Returns the URL to load, or null when the input is empty. */
    fun resolve(): String? = controller.resolveToUrl(query)
}
