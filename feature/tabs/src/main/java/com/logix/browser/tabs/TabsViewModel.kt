package com.logix.browser.tabs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.logix.browser.chromiumbridge.ChromiumEngineManager
import com.logix.browser.chromiumbridge.Engine
import com.logix.browser.database.TabState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Tab list state over [TabsRepository]; programmatic navigation goes
 * through [ChromiumEngineManager] so the bound [Engine] stays in sync.
 */
@HiltViewModel
class TabsViewModel @Inject constructor(
    private val repository: TabsRepository,
    private val engineManager: ChromiumEngineManager,
) : ViewModel() {

    val tabs: StateFlow<List<TabState>> = repository.tabs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeTab: StateFlow<TabState?> = repository.tabs
        .map { list -> list.firstOrNull { it.isActive } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            if (repository.tabs.first().isEmpty()) {
                repository.createTab()
            }
        }
    }

    fun bindEngine(engine: Engine) = engineManager.bind(engine)

    fun openInActiveTab(url: String) {
        viewModelScope.launch {
            val current = activeTab.value
            if (current == null) {
                repository.createTab(url = url, title = hostOf(url))
            } else {
                repository.openUrl(current.id, url)
            }
            engineManager.loadUrl(url)
        }
    }

    fun createTab() {
        viewModelScope.launch { repository.createTab() }
    }

    fun closeTab(id: String) {
        viewModelScope.launch {
            repository.closeTab(id)
            val current = repository.tabs.first()
            if (current.isEmpty()) {
                repository.createTab()
            } else if (current.none { it.isActive }) {
                repository.selectTab(current.first().id)
            }
        }
    }

    fun selectTab(id: String) {
        viewModelScope.launch { repository.selectTab(id) }
    }

    fun goBack(): Boolean = engineManager.goBack()

    fun goForward(): Boolean = engineManager.goForward()

    fun refresh() = engineManager.reload()

    private fun hostOf(url: String): String =
        try {
            java.net.URI(url).host ?: url.take(40)
        } catch (e: Exception) {
            url.take(40)
        }
}
