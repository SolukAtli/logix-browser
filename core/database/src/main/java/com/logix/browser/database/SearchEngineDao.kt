package com.logix.browser.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchEngineDao {

    @Query("SELECT * FROM search_engines ORDER BY displayName")
    fun observeAll(): Flow<List<SearchEngineEntity>>

    @Query("SELECT * FROM search_engines WHERE isSelected = 1 LIMIT 1")
    suspend fun selected(): SearchEngineEntity?

    @Upsert
    suspend fun upsertAll(engines: List<SearchEngineEntity>)

    @Query("UPDATE search_engines SET isSelected = 0")
    suspend fun clearSelected()

    @Query("UPDATE search_engines SET isSelected = 1 WHERE id = :id")
    suspend fun setSelected(id: String)
}
