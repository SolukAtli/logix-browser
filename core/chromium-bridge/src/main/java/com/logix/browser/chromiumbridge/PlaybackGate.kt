package com.logix.browser.chromiumbridge

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Arka planda ses anahtarı: açıkken sekmeler arası geçişte
 * motor duraklatılmaz, ses çalmaya devam eder.
 */
@Singleton
class PlaybackGate @Inject constructor() {
    @Volatile
    var keepAudioInBackground: Boolean = false
}
