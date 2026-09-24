package com.logix.browser.tabs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.logix.browser.database.DomainSetting
import com.logix.browser.database.DomainSettingsDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Site başına geçersiz kılmalar (host → ayar). */
@HiltViewModel
class SiteSettingsViewModel @Inject constructor(
    private val dao: DomainSettingsDao,
) : ViewModel() {

    val overrides: StateFlow<Map<String, DomainSetting>> = dao.observeAll()
        .map { list -> list.associateBy { it.host } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun setJavaScript(host: String, enabled: Boolean?) {
        viewModelScope.launch { save(host) { it.copy(javaScript = enabled) } }
    }

    fun setAdBlock(host: String, enabled: Boolean?) {
        viewModelScope.launch { save(host) { it.copy(adBlock = enabled) } }
    }

    fun reset(host: String) {
        viewModelScope.launch { dao.deleteByHost(host) }
    }

    private suspend fun save(host: String, edit: (DomainSetting) -> DomainSetting) {
        val current = dao.observeAll().first().firstOrNull { it.host == host }
            ?: DomainSetting(host)
        val updated = edit(current)
        if (updated.javaScript == null && updated.adBlock == null) {
            dao.deleteByHost(host)
        } else {
            dao.upsert(updated)
        }
    }
}
