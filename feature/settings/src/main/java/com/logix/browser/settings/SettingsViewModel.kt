package com.logix.browser.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.logix.browser.database.HistoryDao
import com.logix.browser.search.SearchEngine
import com.logix.browser.search.SearchEngineManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Settings screen state over [SettingsRepository]; engine selection is kept
 * in sync with [SearchEngineManager]. Applies the death-reset retention
 * policy (purge history older than N days) on startup.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val engines: SearchEngineManager,
    private val historyDao: HistoryDao,
) : ViewModel() {

    val settings: StateFlow<BrowserSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BrowserSettings())

    val availableEngines: List<SearchEngine> = engines.available
    val selectedEngine = engines.selected

    init {
        viewModelScope.launch {
            val current = repository.settings.first()
            if (current.deathResetEnabled) {
                val cutoff = System.currentTimeMillis() -
                    current.deathResetDays.coerceIn(1, 365) * 24L * 60L * 60L * 1000L
                historyDao.deleteOlderThan(cutoff)
            }
        }
    }

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

    fun setAccent(value: String) {
        viewModelScope.launch { repository.setAccent(value) }
    }

    fun setAmoled(enabled: Boolean) {
        viewModelScope.launch { repository.setAmoled(enabled) }
    }

    fun setCookiesAccepted(accepted: Boolean) {
        viewModelScope.launch { repository.setCookiesAccepted(accepted) }
    }

    fun setHttpsOnly(enabled: Boolean) {
        viewModelScope.launch { repository.setHttpsOnly(enabled) }
    }

    fun setDeathResetEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setDeathResetEnabled(enabled) }
    }

    fun setDeathResetDays(days: Int) {
        viewModelScope.launch { repository.setDeathResetDays(days) }
    }

    fun setBarPosition(position: String) {
        viewModelScope.launch { repository.setBarPosition(position) }
    }

    fun setUserAgent(key: String) {
        viewModelScope.launch { repository.setUserAgent(key) }
    }

    fun setTextScale(scale: Float) {
        viewModelScope.launch { repository.setTextScale(scale) }
    }

    fun setIncognito(enabled: Boolean) {
        viewModelScope.launch { repository.setIncognito(enabled) }
    }

    fun quickClearHistory(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            historyDao.clearAll()
            onDone()
        }
    }
}
