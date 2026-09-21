package com.logix.browser.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single visited page record. Timestamps are epoch millis.
 */
@Entity(tableName = "history")
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val visitedAt: Long,
)
