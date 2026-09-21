package com.logix.browser.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
    }

    val settings: Flow<BrowserSettings> =
        context.settingsDataStore.data.map { prefs ->
            BrowserSettings(
                theme = prefs[Keys.THEME] ?: "dark",
                searchEngineKey = prefs[Keys.SEARCH_ENGINE] ?: "google",
                adBlockEnabled = prefs[Keys.AD_BLOCK] ?: true,
                trackerBlockEnabled = prefs[Keys.TRACKER_BLOCK] ?: true,
                desktopSite = prefs[Keys.DESKTOP_SITE] ?: false,
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
}
