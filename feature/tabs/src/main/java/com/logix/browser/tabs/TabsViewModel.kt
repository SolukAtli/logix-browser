package com.logix.browser.tabs

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.logix.browser.chromiumbridge.ChromiumEngineManager
import com.logix.browser.chromiumbridge.Engine
import com.logix.browser.chromiumbridge.EngineRegistry
import com.logix.browser.chromiumbridge.MemoryPressureHandler
import com.logix.browser.database.TabState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Tab list state over [TabsRepository]; programmatic navigation goes
 * through [ChromiumEngineManager] so the bound [Engine] stays in sync.
 *
 * Freeze/restore contract: the pool admits at most
 * [TabPool.MAX_LIVE_ENGINES] live tabs. Evicted tabs keep only their
 * [TabState] row; switching back rehydrates the engine from
 * `TabState.url`. [EngineRegistry] performs the actual native releases.
 */
@HiltViewModel
class TabsViewModel @Inject constructor(
    private val repository: TabsRepository,
    private val engineManager: ChromiumEngineManager,
    private val registry: EngineRegistry,
    memoryPressure: MemoryPressureHandler,
) : ViewModel() {

    val tabs: StateFlow<List<TabState>> = repository.tabs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeTab: StateFlow<TabState?> = repository.tabs
        .map { list -> list.firstOrNull { it.isActive } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val liveTabIds: StateFlow<Set<String>> = repository.liveTabIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val _canGoBack = MutableStateFlow(false)
    val canGoBack: StateFlow<Boolean> = _canGoBack

    private val _canGoForward = MutableStateFlow(false)
    val canGoForward: StateFlow<Boolean> = _canGoForward

    private val _thumbnails = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val thumbnails: StateFlow<Map<String, Bitmap>> = _thumbnails

    init {
        // Sekme listesi her boşaldığında (ilk açılış, tek tek kapatma veya
        // ölüm anahtarı imhası) taze bir sekme aç — tarayıcı sekmesiz kalmaz.
        viewModelScope.launch {
            repository.tabs.collect { list ->
                if (list.isEmpty()) {
                    repository.createTab()
                }
            }
        }
        viewModelScope.launch {
            memoryPressure.pressure.collect { level ->
                if (MemoryPressureHandler.isCritical(level)) {
                    releaseBackgroundEngines()
                }
            }
        }
    }

    fun bindEngine(engine: Engine) {
        val id = activeTab.value?.id ?: return
        registry.register(id, engine)
        engineManager.bind(engine)
        engine.onShow()
    }

    /** Called by the hosted view on every navigation commit. */
    fun onNavigationStateChanged(canGoBack: Boolean, canGoForward: Boolean) {
        _canGoBack.value = canGoBack
        _canGoForward.value = canGoForward
    }

    fun openInActiveTab(url: String) {
        viewModelScope.launch {
            resetNavigationState()
            val current = activeTab.value
            val evicted = if (current == null) {
                repository.createTab(url = url, title = hostOf(url))
                emptyList()
            } else {
                repository.openUrl(current.id, url)
            }
            evicted.forEach(registry::release)
            engineManager.loadUrl(url)
        }
    }

    fun createTab() {
        viewModelScope.launch {
            resetNavigationState()
            repository.createTab()
        }
    }

    fun closeTab(id: String) {
        viewModelScope.launch {
            resetNavigationState()
            registry.release(id)
            repository.closeTab(id)
            _thumbnails.value = _thumbnails.value - id
            val current = repository.tabs.first()
            // Boş kalırsa init'teki toplayıcı taze sekmeyi açar.
            if (current.none { it.isActive } && current.isNotEmpty()) {
                selectTab(current.first().id)
            }
        }
    }

    fun selectTab(id: String) {
        viewModelScope.launch {
            resetNavigationState()
            val prevId = activeTab.value?.id
            if (prevId != null && prevId != id) {
                registry.get(prevId)?.onHide()
            }
            val evicted = repository.selectTab(id)
            evicted.forEach(registry::release)
            if (prevId != null && prevId != id) {
                registry.move(prevId, id)
            }
            // Frozen tab restore: session was dropped, TabState.url survived.
            val tab = repository.tabs.first().firstOrNull { it.id == id }
            val restoreUrl = tab?.url
            if (restoreUrl != null) {
                engineManager.loadUrl(restoreUrl)
            } else {
                registry.get(id)?.onShow()
            }
        }
    }

    /**
     * System memory pressure: freeze every background tab now, keep only
     * the active one alive.
     */
    fun releaseBackgroundEngines() {
        viewModelScope.launch {
            val frozen = repository.freezeAllExcept(activeTab.value?.id)
            frozen.forEach(registry::release)
        }
    }

    fun goBack(): Boolean = engineManager.goBack()

    fun goForward(): Boolean = engineManager.goForward()

    fun refresh() = engineManager.reload()

    /** Drops the bound engine's back/forward list (incognito entry). */
    fun clearActiveEngineHistory() {
        activeTab.value?.id?.let { registry.get(it)?.clearHistory() }
    }

    /** Aktif sekmenin o anki görünümünü önizleme olarak yakalar. */
    fun refreshThumbnail() {
        viewModelScope.launch {
            val id = activeTab.value?.id ?: return@launch
            val bmp = withContext(Dispatchers.Main) {
                engineManager.captureActiveThumbnail()
            }
            if (bmp != null) {
                _thumbnails.value = _thumbnails.value + (id to bmp)
            }
        }
    }

    private fun resetNavigationState() {
        _canGoBack.value = false
        _canGoForward.value = false
    }

    private fun hostOf(url: String): String =
        try {
            java.net.URI(url).host ?: url.take(40)
        } catch (e: Exception) {
            url.take(40)
        }
}
