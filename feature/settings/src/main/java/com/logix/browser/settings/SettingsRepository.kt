package com.logix.browser.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.logix.browser.settings.ui.AccentPalette
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "logix_settings")

/**
 * DataStore-backed settings repository.
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val SEARCH_ENGINE = stringPreferencesKey("search_engine")
        val AD_BLOCK = booleanPreferencesKey("ad_block")
        val TRACKER_BLOCK = booleanPreferencesKey("tracker_block")
        val DESKTOP_SITE = booleanPreferencesKey("desktop_site")
        val FILTER_AUTO_UPDATE = booleanPreferencesKey("filter_auto_update")
        val FILTER_UPDATED_AT = longPreferencesKey("filter_updated_at")
        val ACCENT = stringPreferencesKey("accent")
        val AMOLED = booleanPreferencesKey("amoled")
        val COOKIES_ACCEPTED = booleanPreferencesKey("cookies_accepted")
        val HTTPS_ONLY = booleanPreferencesKey("https_only")
        val DEATH_RESET_ENABLED = booleanPreferencesKey("death_reset_enabled")
        val DEATH_RESET_DAYS = intPreferencesKey("death_reset_days")
        val LAST_ACTIVE_AT = longPreferencesKey("last_active_at")
        val BAR_POSITION = stringPreferencesKey("bar_position")
        val USER_AGENT = stringPreferencesKey("user_agent")
        val TEXT_SCALE = floatPreferencesKey("text_scale")
        val INCOGNITO = booleanPreferencesKey("incognito")
    }

    val settings: Flow<BrowserSettings> =
        context.settingsDataStore.data.map { prefs ->
            BrowserSettings(
                theme = prefs[Keys.THEME] ?: "dark",
                searchEngineKey = prefs[Keys.SEARCH_ENGINE] ?: "google",
                adBlockEnabled = prefs[Keys.AD_BLOCK] ?: true,
                trackerBlockEnabled = prefs[Keys.TRACKER_BLOCK] ?: true,
                desktopSite = prefs[Keys.DESKTOP_SITE] ?: false,
                filterAutoUpdate = prefs[Keys.FILTER_AUTO_UPDATE] ?: true,
                filterUpdatedAt = prefs[Keys.FILTER_UPDATED_AT] ?: 0L,
                accent = prefs[Keys.ACCENT] ?: "blue",
                amoled = prefs[Keys.AMOLED] ?: false,
                cookiesAccepted = prefs[Keys.COOKIES_ACCEPTED] ?: true,
                httpsOnly = prefs[Keys.HTTPS_ONLY] ?: false,
                deathResetEnabled = prefs[Keys.DEATH_RESET_ENABLED] ?: false,
                deathResetDays = prefs[Keys.DEATH_RESET_DAYS] ?: 30,
                lastActiveAt = prefs[Keys.LAST_ACTIVE_AT] ?: 0L,
                barPosition = prefs[Keys.BAR_POSITION] ?: "top",
                userAgent = prefs[Keys.USER_AGENT] ?: "mobile",
                textScale = prefs[Keys.TEXT_SCALE] ?: 1f,
                incognito = prefs[Keys.INCOGNITO] ?: false,
            )
        }

    suspend fun setTheme(theme: String) {
        context.settingsDataStore.edit { it[Keys.THEME] = theme }
    }

    suspend fun setSearchEngine(key: String) {
        context.settingsDataStore.edit { it[Keys.SEARCH_ENGINE] = key }
    }

    suspend fun setAdBlock(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.AD_BLOCK] = enabled }
    }

    suspend fun setTrackerBlock(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.TRACKER_BLOCK] = enabled }
    }

    suspend fun setDesktopSite(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.DESKTOP_SITE] = enabled }
    }

    suspend fun setFilterAutoUpdate(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.FILTER_AUTO_UPDATE] = enabled }
    }

    /** Records a filter-list refresh. Download itself is a Faz-3.1 item. */
    suspend fun markFiltersUpdated() {
        context.settingsDataStore.edit { it[Keys.FILTER_UPDATED_AT] = System.currentTimeMillis() }
    }

    suspend fun setAccent(value: String) {
        context.settingsDataStore.edit { it[Keys.ACCENT] = value }
    }

    suspend fun setAmoled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.AMOLED] = enabled }
    }

    suspend fun setCookiesAccepted(accepted: Boolean) {
        context.settingsDataStore.edit { it[Keys.COOKIES_ACCEPTED] = accepted }
    }

    suspend fun setHttpsOnly(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.HTTPS_ONLY] = enabled }
    }

    suspend fun setDeathResetEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.DEATH_RESET_ENABLED] = enabled }
    }

    suspend fun setDeathResetDays(days: Int) {
        context.settingsDataStore.edit { it[Keys.DEATH_RESET_DAYS] = days.coerceIn(1, 365) }
    }

    /** Son aktif olma anı — ölüm anahtarı sayacı her açılışta buradan beslenir. */
    suspend fun updateLastActive(now: Long = System.currentTimeMillis()) {
        context.settingsDataStore.edit { it[Keys.LAST_ACTIVE_AT] = now }
    }

    suspend fun setBarPosition(position: String) {
        context.settingsDataStore.edit { it[Keys.BAR_POSITION] = position }
    }

    suspend fun setUserAgent(key: String) {
        context.settingsDataStore.edit { it[Keys.USER_AGENT] = key }
    }

    suspend fun setTextScale(scale: Float) {
        context.settingsDataStore.edit { it[Keys.TEXT_SCALE] = scale.coerceIn(0.5f, 3f) }
    }

    suspend fun setIncognito(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.INCOGNITO] = enabled }
    }
}
