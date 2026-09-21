package com.logix.browser.chromiumbridge

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks live [Engine] instances by tab id. Callers (TabPool policy)
 * decide *when* to release; the registry performs the release safely
 * (`onHide` + `destroy`) exactly once per instance.
 */
@Singleton
class EngineRegistry @Inject constructor() {

    private val lock = Any()
    private val engines = mutableMapOf<String, Engine>()

    fun register(tabId: String, engine: Engine) {
        synchronized(lock) {
            engines[tabId]?.let { existing ->
                if (existing !== engine) {
                    existing.destroy()
                }
            }
            engines[tabId] = engine
        }
    }

    fun get(tabId: String): Engine? = synchronized(lock) { engines[tabId] }

    /**
     * Moves the engine entry from one tab id to another (single-view reuse).
     * Destroys any *different* engine previously registered under [toId].
     */
    fun move(fromId: String, toId: String) {
        if (fromId == toId) return
        synchronized(lock) {
            val engine = engines.remove(fromId) ?: return
            engines[toId]?.let { existing ->
                if (existing !== engine) {
                    existing.destroy()
                }
            }
            engines[toId] = engine
        }
    }

    /** Releases the engine for [tabId]: pause first, then destroy. */
    fun release(tabId: String) {
        val engine = synchronized(lock) { engines.remove(tabId) } ?: return
        try {
            engine.onHide()
        } finally {
            engine.destroy()
        }
    }

    /** Releases every engine except [keepId]; returns released ids. */
    fun releaseAllExcept(keepId: String?): List<String> {
        val ids = synchronized(lock) {
            engines.keys.filter { it != keepId }
        }
        ids.forEach(::release)
        return ids
    }

    fun liveIds(): Set<String> = synchronized(lock) { engines.keys.toSet() }
}
