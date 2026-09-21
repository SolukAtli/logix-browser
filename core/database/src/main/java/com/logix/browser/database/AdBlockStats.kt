package com.logix.browser.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single-row (id = 0) lifetime counters of blocked content.
 */
@Entity(tableName = "adblock_stats")
data class AdBlockStats(
    @PrimaryKey val id: Int = 0,
    val totalBlockedAds: Long = 0,
    val totalBlockedTrackers: Long = 0,
    val updatedAt: Long = 0,
)
