package com.logix.browser

import android.app.Application
import android.content.ComponentCallbacks2
import com.logix.browser.chromiumbridge.MemoryPressureHandler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class BrowserApp : Application() {

    @Inject
    lateinit var memoryPressure: MemoryPressureHandler

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (::memoryPressure.isInitialized) {
            memoryPressure.onTrimMemory(level)
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        if (::memoryPressure.isInitialized) {
            // Treat as the strongest pressure signal.
            memoryPressure.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
        }
    }
}
