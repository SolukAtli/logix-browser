package com.logix.browser.adblock

import com.logix.browser.database.AdBlockStatsDao
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Totals of blocked ads/trackers. Writes are fire-and-forget on the
 * application scope so interception callbacks never block.
 */
@Singleton
class AdBlockStatsRepository @Inject constructor(
    private val dao: AdBlockStatsDao,
    @ApplicationScope private val scope: CoroutineScope,
) {

    data class Totals(val ads: Long, val trackers: Long)

    val totals: Flow<Totals> = dao.observe().map { stats ->
        Totals(
            ads = stats?.totalBlockedAds ?: 0L,
            trackers = stats?.totalBlockedTrackers ?: 0L,
        )
    }

    fun recordBlocked(tracker: Boolean) {
        scope.launch {
            val now = System.currentTimeMillis()
            val updated = if (tracker) {
                dao.increment(ads = 0, trackers = 1, now = now)
            } else {
                dao.increment(ads = 1, trackers = 0, now = now)
            }
            if (updated == 0) {
                dao.upsert(
                    com.logix.browser.database.AdBlockStats(
                        totalBlockedAds = if (tracker) 0 else 1,
                        totalBlockedTrackers = if (tracker) 1 else 0,
                        updatedAt = now,
                    ),
                )
            }
        }
    }
}
