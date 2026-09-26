package com.logix.browser.settings

import android.content.Context
import com.logix.browser.chromiumbridge.BrowserDataWiper
import com.logix.browser.database.AdBlockStatsDao
import com.logix.browser.database.BookmarkDao
import com.logix.browser.database.DomainSettingsDao
import com.logix.browser.database.HistoryDao
import com.logix.browser.database.TabDao
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * Ölüm Anahtarı: tarayıcı belirlenen gün sayısı boyunca hiç açılmazsa,
 * açılışta TÜM yerel veriyi geri döndürülemez şekilde imha eder.
 *
 * Silinenler: geçmiş, yer imleri, sekmeler, çerezler/oturumlar,
 * site verileri, önbellek, engelleme istatistikleri, site ayarları.
 *
 * @return true → bu açılışta imha gerçekleşti.
 */
@Singleton
class DeathResetManager @Inject constructor(
    private val repository: SettingsRepository,
    private val historyDao: HistoryDao,
    private val bookmarkDao: BookmarkDao,
    private val tabDao: TabDao,
    private val statsDao: AdBlockStatsDao,
    private val domainDao: DomainSettingsDao,
    private val wiper: BrowserDataWiper,
    @ApplicationContext private val context: Context,
) {

    suspend fun onAppStart(): Boolean {
        val now = System.currentTimeMillis()
        return try {
            val current = repository.settings.first()
            if (!current.deathResetEnabled) {
                return false
            }
            val last = current.lastActiveAt
            val limitMs = current.deathResetDays.coerceIn(1, 365) * 24L * 60L * 60L * 1000L
            if (last > 0L && now - last > limitMs) {
                wipe()
                repository.updateLastActive(now)
                true
            } else {
                repository.updateLastActive(now)
                false
            }
        } catch (e: Exception) {
            // Açılışta asla çökertme; sayaç bir sonraki açılışta devam eder.
            runCatching { repository.updateLastActive(now) }
            false
        }
    }

    private suspend fun wipe() {
        historyDao.clearAll()
        bookmarkDao.clearAll()
        tabDao.clearAll()
        statsDao.clear()
        domainDao.clearAll()
        wiper.wipeCookiesAndStorage()
        runCatching { wiper.wipeAppCache(context.cacheDir) }
    }
}
