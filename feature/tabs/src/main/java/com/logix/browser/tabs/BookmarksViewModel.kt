package com.logix.browser.tabs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.logix.browser.database.Bookmark
import com.logix.browser.database.BookmarkDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val bookmarkDao: BookmarkDao,
) : ViewModel() {

    val bookmarks: StateFlow<List<Bookmark>> = bookmarkDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggle(url: String, title: String, isBookmarked: Boolean) {
        viewModelScope.launch {
            if (isBookmarked) {
                bookmarkDao.deleteByUrl(url)
            } else {
                bookmarkDao.upsert(
                    Bookmark(url = url, title = title.ifBlank { url }, createdAt = System.currentTimeMillis()),
                )
            }
        }
    }

    fun remove(url: String) {
        viewModelScope.launch { bookmarkDao.deleteByUrl(url) }
    }

    fun clearAll() {
        viewModelScope.launch { bookmarkDao.clearAll() }
    }
}
