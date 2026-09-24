package com.logix.browser.omnibox

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.logix.browser.search.SearchEngine
import com.logix.browser.search.SearchEngineId
import com.logix.browser.search.SearchEngineManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Owns omnibox text + selected engine + live suggestions; delegates URL
 * resolution to [OmniboxController].
 */
@HiltViewModel
class OmniboxViewModel @Inject constructor(
    private val controller: OmniboxController,
    private val engines: SearchEngineManager,
    private val suggestionsRepo: SuggestionsRepository,
) : ViewModel() {

    var query by mutableStateOf("")
        private set

    var suggestions by mutableStateOf<List<String>>(emptyList())
        private set

    val availableEngines: List<SearchEngine> = engines.available
    val selectedEngine = engines.selected

    private var suggestJob: Job? = null

    fun onQueryChange(value: String) {
        query = value
        suggestJob?.cancel()
        // URL yazılırken öneri getirme.
        if (value.isBlank() || value.trimStart().startsWith("http")) {
            suggestions = emptyList()
            return
        }
        suggestJob = viewModelScope.launch {
            delay(300)
            suggestions = suggestionsRepo.fetch(engines.selected.value, value)
        }
    }

    fun clearQuery() {
        suggestJob?.cancel()
        query = ""
        suggestions = emptyList()
    }

    fun dismissSuggestions() {
        suggestJob?.cancel()
        suggestions = emptyList()
    }

    fun selectEngine(id: SearchEngineId) = engines.select(id)

    /** Returns the URL to load, or null when the input is empty. */
    fun resolve(): String? = controller.resolveToUrl(query)
}
