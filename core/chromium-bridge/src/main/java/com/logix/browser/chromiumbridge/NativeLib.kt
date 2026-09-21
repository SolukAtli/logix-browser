package com.logix.browser.chromiumbridge

/**
 * JNI entry point for the Faz-4 native libs (`native/`).
 *
 * Libraries are absent until the NDK wiring lands (Faz-4.1); every call
 * site must check [isLoaded] first and fall back to the JVM/WebView path.
 * `external` declarations are never invoked while unloaded.
 */
object NativeLib {

    const val LIB_FILTER_ENGINE = "logix_filter_engine"
    const val LIB_CONTENT_BRIDGE = "logix_content_bridge"

    val isFilterLoaded: Boolean = loadQuietly(LIB_FILTER_ENGINE)
    val isContentLoaded: Boolean = loadQuietly(LIB_CONTENT_BRIDGE)

    /** True only when both native libs are present. */
    val isLoaded: Boolean
        get() = isFilterLoaded && isContentLoaded

    // -- filter_engine (jni_bridge.cc) --

    external fun nativeFilterCheck(host: String): Boolean

    external fun nativeFilterAddBlocked(host: String)

    external fun nativeFilterAddAllowed(host: String)

    external fun nativeFilterClear()

    external fun nativeVersion(): String

    // -- content bridge (content_bridge.cc) --

    external fun nativeCreate(): Long

    external fun nativeDestroy(handle: Long)

    external fun nativeNavigate(handle: Long, url: String): Boolean

    private fun loadQuietly(name: String): Boolean =
        try {
            System.loadLibrary(name)
            true
        } catch (e: UnsatisfiedLinkError) {
            false
        }
}
