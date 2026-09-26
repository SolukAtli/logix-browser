package com.logix.browser.chromiumbridge

import android.webkit.CookieManager
import android.webkit.WebStorage
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Ortak veri silme servisi: incognito, Hızlı Temizle ve Death Reset
 * aynı await edilen akışı kullanır.
 */
@Singleton
class BrowserDataWiper @Inject constructor() {

    /** Çerez + site verisini silinmesi bitene kadar bekler. */
    suspend fun wipeCookiesAndStorage(): Boolean {
        val cookiesDone = suspendCancellableCoroutine { cont ->
            runCatching {
                CookieManager.getInstance().removeAllCookies { cont.resume(true) }
            }.onFailure { cont.resume(false) }
        }
        runCatching {
            CookieManager.getInstance().flush()
            WebStorage.getInstance().deleteAllData()
        }
        return cookiesDone
    }

    /** Uygulama önbellek dizinini temizler (WebView profili hariç). */
    fun wipeAppCache(cacheDir: java.io.File): Boolean = runCatching {
        var ok = true
        cacheDir.listFiles()?.forEach { ok = it.deleteRecursively() && ok }
        ok
    }.getOrDefault(false)
}
