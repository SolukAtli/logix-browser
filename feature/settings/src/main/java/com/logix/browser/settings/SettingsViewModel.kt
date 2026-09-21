package com.logix.browser.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.logix.browser.search.SearchEngine
import com.logix.browser.search.SearchEngineManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Settings screen state over [SettingsRepository]; engine selection is kept
 * in sync with [SearchEngineManager].
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val engines: SearchEngineManager,
) : ViewModel() {

    val settings: StateFlow<BrowserSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BrowserSettings())

    val availableEngines: List<SearchEngine> = engines.available
    val selectedEngine = engines.selected

    fun setTheme(theme: String) {
        viewModelScope.launch { repository.setTheme(theme) }
    }

    fun setSearchEngine(key: String) {
        viewModelScope.launch {
            repository.setSearchEngine(key)
            engines.selectByKey(key)
        }
    }

    fun setAdBlock(enabled: Boolean) {
        viewModelScope.launch { repository.setAdBlock(enabled) }
    }

    fun setTrackerBlock(enabled: Boolean) {
        viewModelScope.launch { repository.setTrackerBlock(enabled) }
    }

    fun setDesktopSite(enabled: Boolean) {
        viewModelScope.launch { repository.setDesktopSite(enabled) }
    }

    fun setFilterAutoUpdate(enabled: Boolean) {
        viewModelScope.launch { repository.setFilterAutoUpdate(enabled) }
    }

    fun refreshFilters() {
        viewModelScope.launch { repository.markFiltersUpdated() }
    }
}
