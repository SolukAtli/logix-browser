package com.logix.browser.tabs

import com.logix.browser.database.TabDao
import com.logix.browser.database.TabState
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * Tab CRUD over [TabDao]. UI state mapping lives in the (Faz-2) ViewModel.
 */
@Singleton
class TabsRepository @Inject constructor(
    private val tabDao: TabDao,
) {

    val tabs: Flow<List<TabState>> = tabDao.observeTabs()

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
        return tab
    }

    suspend fun closeTab(id: String) {
        tabDao.deleteById(id)
    }

    suspend fun selectTab(id: String) {
        tabDao.clearActive()
        tabDao.setActive(id)
    }

    suspend fun activeTab(): TabState? = tabDao.activeTab()
}
