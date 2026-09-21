package com.logix.browser.tabs

import com.logix.browser.database.TabDao
import com.logix.browser.database.TabState
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * Tab CRUD over [TabDao] with [TabPool] admission: every navigation-grade
 * operation marks the tab live and returns ids the caller must freeze
 * (their engines released, state already persisted as [TabState]).
 */
@Singleton
class TabsRepository @Inject constructor(
    private val tabDao: TabDao,
    private val pool: TabPool,
) {

    val tabs: Flow<List<TabState>> = tabDao.observeTabs()

    val liveTabIds = pool.liveIds

    suspend fun createTab(url: String? = null, title: String = "Yeni Sekme"): TabState {
        val now = System.currentTimeMillis()
        val tab = TabState(
            id = UUID.randomUUID().toString(),
            url = url,
            title = title,
            isActive = true,
            lastAccessed = now,
        )
        tabDao.clearActive()
        tabDao.upsert(tab)
        pool.accessed(tab.id)
        return tab
    }

    suspend fun closeTab(id: String) {
        tabDao.deleteById(id)
        pool.remove(id)
    }

    /** Returns ids evicted by LRU admission. */
    suspend fun selectTab(id: String): List<String> {
        tabDao.clearActive()
        tabDao.setActive(id)
        return pool.accessed(id)
    }

    /** Returns ids evicted by LRU admission. */
    suspend fun openUrl(id: String, url: String): List<String> {
        tabDao.clearActive()
        tabDao.upsert(
            TabState(
                id = id,
                url = url,
                title = hostOf(url),
                isActive = true,
                lastAccessed = System.currentTimeMillis(),
            ),
        )
        return pool.accessed(id)
    }

    /**
     * Memory pressure: freezes everything except [keepId].
     * Returns frozen ids whose engines must be released now.
     */
    suspend fun freezeAllExcept(keepId: String?): List<String> =
        pool.freezeAllExcept(keepId)

    suspend fun activeTab(): TabState? = tabDao.activeTab()

    private fun hostOf(url: String): String =
        try {
            java.net.URI(url).host ?: url.take(40)
        } catch (e: Exception) {
            url.take(40)
        }
}
