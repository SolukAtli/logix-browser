package com.logix.browser.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DomainSettingsDao {

    @Query("SELECT * FROM domain_settings")
    fun observeAll(): Flow<List<DomainSetting>>

    @Upsert
    suspend fun upsert(setting: DomainSetting)

    @Query("DELETE FROM domain_settings WHERE host = :host")
    suspend fun deleteByHost(host: String)
}
