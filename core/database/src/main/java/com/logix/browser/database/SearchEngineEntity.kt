package com.logix.browser.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Search engine catalogue row. `id` matches [com.logix.browser.search.SearchEngineId.key].
 * Exactly one row should have `isSelected = true`.
 */
@Entity(tableName = "search_engines")
data class SearchEngineEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val isSelected: Boolean,
)
