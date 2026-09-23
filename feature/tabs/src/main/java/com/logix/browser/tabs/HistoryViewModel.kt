package com.logix.browser.tabs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.logix.browser.database.HistoryDao
import com.logix.browser.database.HistoryEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyDao: HistoryDao,
) : ViewModel() {

    val recent: StateFlow<List<HistoryEntry>> = historyDao.observeRecent(200)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun logVisit(url: String, title: String) {
        if (url.isBlank()) return
        if (url == "about:blank") return
        viewModelScope.launch {
            historyDao.insert(
                HistoryEntry(url = url, title = title.ifBlank { url }, visitedAt = System.currentTimeMillis()),
            )
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { historyDao.deleteById(id) }
    }

    fun clearAll() {
        viewModelScope.launch { historyDao.clearAll() }
    }
}
