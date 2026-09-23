package com.logix.browser.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TabDao {

    @Query("SELECT * FROM tabs ORDER BY lastAccessed DESC")
    fun observeTabs(): Flow<List<TabState>>

    @Query("SELECT * FROM tabs WHERE isActive = 1 LIMIT 1")
    suspend fun activeTab(): TabState?

    @Upsert
    suspend fun upsert(tab: TabState)

    @Query("DELETE FROM tabs WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM tabs")
    suspend fun clearAll()

    @Query("UPDATE tabs SET isActive = 0")
    suspend fun clearActive()

    @Query("UPDATE tabs SET isActive = 1 WHERE id = :id")
    suspend fun setActive(id: String)
}
