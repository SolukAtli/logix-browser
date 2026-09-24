package com.logix.browser.settings

import android.webkit.CookieManager
import android.webkit.WebStorage
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.logix.browser.database.BookmarkDao
import com.logix.browser.database.HistoryDao
import com.logix.browser.search.SearchEngine
import com.logix.browser.search.SearchEngineManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Settings screen state over [SettingsRepository]; engine selection is kept
 * in sync with [SearchEngineManager]. Her açılışta [DeathResetManager]
 * çalışır; imha olduysa [deathResetTriggered] ile UI bilgilendirilir.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val engines: SearchEngineManager,
    private val historyDao: HistoryDao,
    private val bookmarkDao: BookmarkDao,
    private val deathReset: DeathResetManager,
) : ViewModel() {

    val settings: StateFlow<BrowserSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BrowserSettings())

    val availableEngines: List<SearchEngine> = engines.available
    val selectedEngine = engines.selected

    private val _deathResetTriggered = MutableStateFlow(false)
    val deathResetTriggered: StateFlow<Boolean> = _deathResetTriggered.asStateFlow()

    init {
        viewModelScope.launch {
            _deathResetTriggered.value = deathReset.onAppStart()
        }
    }

    fun acknowledgeDeathReset() {
        _deathResetTriggered.value = false
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

    fun setQuickClearHistory(enabled: Boolean) {
        viewModelScope.launch { repository.setQuickClearHistory(enabled) }
    }

    fun setQuickClearCookies(enabled: Boolean) {
        viewModelScope.launch { repository.setQuickClearCookies(enabled) }
    }

    fun setQuickClearCache(enabled: Boolean) {
        viewModelScope.launch { repository.setQuickClearCache(enabled) }
    }

    fun setQuickClearBookmarks(enabled: Boolean) {
        viewModelScope.launch { repository.setQuickClearBookmarks(enabled) }
    }

    /** Ayarlarda seçili kapsamda temizler. Suspend: bitmeden dönmez. */
    suspend fun quickClearAll() {
        val current = repository.settings.first()
        if (current.quickClearHistory) historyDao.clearAll()
        if (current.quickClearBookmarks) bookmarkDao.clearAll()
        if (current.quickClearCookies) {
            runCatching {
                CookieManager.getInstance().removeAllCookies(null)
                CookieManager.getInstance().flush()
            }
        }
        if (current.quickClearCache) {
            runCatching { WebStorage.getInstance().deleteAllData() }
        }
    }
}
