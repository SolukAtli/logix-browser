package com.logix.browser.settings

import com.logix.browser.adblock.DomainAdBlocker
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Gerçek filtre listesi indirici: EasyList + EasyPrivacy indirilir,
 * ayrıştırılıp bellekteki engelleyiciye yüklenir.
 */
@Singleton
class FilterUpdater @Inject constructor(
    private val adBlocker: DomainAdBlocker,
    private val repository: SettingsRepository,
) {

    companion object {
        private const val EASYLIST = "https://easylist.to/easylist/easylist.txt"
        private const val EASYPRIVACY = "https://easylist.to/easylist/easyprivacy.txt"
        private const val STALE_AFTER_MS = 7L * 24L * 60L * 60L * 1000L
    }

    /** Listeler bayatsa indirir; sonuç true ise güncellendi. */
    suspend fun updateIfStale(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val last = repository.filterUpdatedAt()
            if (System.currentTimeMillis() - last < STALE_AFTER_MS) return@runCatching false
            updateNow()
        }.getOrDefault(false)
    }

    suspend fun updateNow(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            download(EASYLIST)?.let { adBlocker.loadFilterText(it) }
            download(EASYPRIVACY)?.let { adBlocker.loadFilterText(it) }
            repository.markFiltersUpdated()
            true
        }.getOrDefault(false)
    }

    private fun download(url: String): String? {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/128.0 Mobile Safari/537.36",
            )
        }
        return try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return null
            conn.inputStream.bufferedReader().readText().takeIf { it.length > 10_000 }
        } catch (e: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }
}
