package com.logix.browser.database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Faz-1 Room database. `exportSchema` stays false until the schema
 * stabilises; then enable it with a schemas directory (Faz-2).
 */
@Database(
    entities = [TabState::class, HistoryEntry::class, SearchEngineEntity::class, AdBlockStats::class],
    version = 2,
    exportSchema = false,
)
abstract class BrowserDatabase : RoomDatabase() {
    abstract fun tabDao(): TabDao
    abstract fun historyDao(): HistoryDao
    abstract fun searchEngineDao(): SearchEngineDao
    abstract fun adBlockStatsDao(): AdBlockStatsDao
}
