package com.logix.browser.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AdBlockStatsDao {

    @Query("SELECT * FROM adblock_stats WHERE id = 0")
    fun observe(): Flow<AdBlockStats?>

    @Upsert
    suspend fun upsert(stats: AdBlockStats)

    /**
     * Adds to the counters. Returns the touched row count (0 when the row
     * does not exist yet and the caller must insert it).
     */
    @Query(
        "UPDATE adblock_stats SET " +
            "totalBlockedAds = totalBlockedAds + :ads, " +
            "totalBlockedTrackers = totalBlockedTrackers + :trackers, " +
            "updatedAt = :now WHERE id = 0",
    )
    suspend fun increment(ads: Long, trackers: Long, now: Long): Int

    @Query("DELETE FROM adblock_stats")
    suspend fun clear()
}
