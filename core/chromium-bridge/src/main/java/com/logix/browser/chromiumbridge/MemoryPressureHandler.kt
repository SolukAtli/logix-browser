package com.logix.browser.chromiumbridge

import android.content.ComponentCallbacks2
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Forwards system memory signals ([BrowserApp.onTrimMemory]) to whoever
 * owns releasable resources (tab pool). Buffer holds the latest signal so
 * a collector that subscribes late still reacts.
 */
@Singleton
class MemoryPressureHandler @Inject constructor() {

    private val _pressure = MutableSharedFlow<Int>(
        replay = 0,
        extraBufferCapacity = 1,
    )
    val pressure: SharedFlow<Int> = _pressure.asSharedFlow()

    fun onTrimMemory(level: Int) {
        _pressure.tryEmit(level)
    }

    companion object {
        /**
         * Critical background-memory pressure. Covers both
         * TRIM_MEMORY_RUNNING_CRITICAL (15) and TRIM_MEMORY_COMPLETE (80).
         */
        fun isCritical(level: Int): Boolean =
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL
    }
}
