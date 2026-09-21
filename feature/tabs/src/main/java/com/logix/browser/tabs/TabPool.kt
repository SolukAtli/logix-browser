package com.logix.browser.tabs

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * LRU admission policy for live engines: at most [MAX_LIVE_ENGINES] tabs
 * may hold a native engine (WebContents) at once. Everything else is a
 * frozen [com.logix.browser.database.TabState] row resurrected on demand.
 *
 * Pure JVM logic (no Android APIs) — covered by unit tests.
 */
@Singleton
class TabPool @Inject constructor() {

    private val lock = Any()

    /** Access-ordered ids, eldest entry = least recently used. */
    private val order = LinkedHashMap<String, Unit>()

    private val _liveIds = MutableStateFlow<Set<String>>(emptySet())
    val liveIds: StateFlow<Set<String>> = _liveIds.asStateFlow()

    /**
     * Marks [id] as most-recently-used. Returns ids evicted to stay within
     * [MAX_LIVE_ENGINES]; callers must release their engines.
     */
    fun accessed(id: String): List<String> {
        synchronized(lock) {
            order.remove(id)
            order[id] = Unit
            val evicted = mutableListOf<String>()
            while (order.size > MAX_LIVE_ENGINES) {
                val lru = order.keys.first()
                order.remove(lru)
                evicted += lru
            }
            _liveIds.value = order.keys.toSet()
            return evicted
        }
    }

    /** Drops [id] from the pool (tab closed). */
    fun remove(id: String) {
        synchronized(lock) {
            order.remove(id)
            _liveIds.value = order.keys.toSet()
        }
    }

    /**
     * Freezes everything except [keepId] (memory pressure). Returns the
     * frozen ids so callers can release their engines.
     */
    fun freezeAllExcept(keepId: String?): List<String> {
        synchronized(lock) {
            val frozen = order.keys.filter { it != keepId }
            order.clear()
            if (keepId != null) {
                order[keepId] = Unit
            }
            _liveIds.value = order.keys.toSet()
            return frozen
        }
    }

    fun isLive(id: String): Boolean = synchronized(lock) { order.containsKey(id) }

    companion object {
        const val MAX_LIVE_ENGINES = 3
    }
}
