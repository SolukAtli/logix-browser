package com.logix.browser.chromiumbridge

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Owns the bound [Engine] instance and exposes navigation state.
 * Activities/fragments bind in onCreate and unbind in onDestroy.
 */
@Singleton
class ChromiumEngineManager @Inject constructor() {

    private var engine: Engine? = null

    private val _currentUrl = MutableStateFlow<String?>(null)
    val currentUrl: StateFlow<String?> = _currentUrl.asStateFlow()

    /**
     * Faz-4: wraps [fallback] with the native bridge when its libraries
     * are present ([ChromiumEngine]), otherwise returns [fallback] as-is.
     */
    fun wrapNative(fallback: Engine): Engine = ChromiumEngine(fallback)

    fun bind(engine: Engine) {
        this.engine = engine
        _currentUrl.value = engine.currentUrl()
    }

    fun unbind() {
        engine = null
    }

    fun loadUrl(url: String) {
        engine?.loadUrl(url)
        _currentUrl.value = url
    }

    fun goBack(): Boolean = engine?.goBack() == true

    fun goForward(): Boolean = engine?.goForward() == true

    fun reload() {
        engine?.reload()
    }

    fun canGoBack(): Boolean = engine?.canGoBack() == true

    fun canGoForward(): Boolean = engine?.canGoForward() == true

    /** Bağlı motorun o anki görünümünü yakalar (UI thread'de çağrılmalı). */
    fun captureActiveThumbnail(): android.graphics.Bitmap? =
        try {
            engine?.captureThumbnail()
        } catch (e: Exception) {
            null
        }
}
