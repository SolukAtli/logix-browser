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

    fun addAll(entries: List<Pair<String, String>>) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            entries.filter { it.first.isNotBlank() }.forEach { (url, title) ->
                bookmarkDao.upsert(
                    Bookmark(url = url, title = title.ifBlank { url }, createdAt = now),
                )
            }
        }
    }

    fun importHtml(html: String): Int {
        var count = 0
        val regex = Regex(
            "<A[^>]+HREF=\"([^\"]+)\"[^>]*>([^<]*)</A>",
            RegexOption.IGNORE_CASE,
        )
        val now = System.currentTimeMillis()
        regex.findAll(html).forEach { match ->
            val url = match.groupValues[1]
            val title = match.groupValues[2].trim()
            if (url.startsWith("http")) {
                viewModelScope.launch {
                    bookmarkDao.upsert(
                        Bookmark(url = url, title = title.ifBlank { url }, createdAt = now),
                    )
                }
                count++
            }
        }
        return count
    }

    fun clearAll() {
        viewModelScope.launch { bookmarkDao.clearAll() }
    }
}
