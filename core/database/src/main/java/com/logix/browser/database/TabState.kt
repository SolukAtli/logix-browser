package com.logix.browser.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted browser tab. `id` is a client-generated UUID string.
 */
@Entity(tableName = "tabs")
data class TabState(
    @PrimaryKey val id: String,
    val url: String?,
    val title: String,
    val isActive: Boolean,
    val lastAccessed: Long,
)
