package com.logix.browser.di

import android.content.Context
import androidx.room.Room
import com.logix.browser.adblock.ApplicationScope
import com.logix.browser.database.AdBlockStatsDao
import com.logix.browser.database.BookmarkDao
import com.logix.browser.database.BrowserDatabase
import com.logix.browser.database.DomainSettingsDao
import com.logix.browser.database.HistoryDao
import com.logix.browser.database.MIGRATION_3_4
import com.logix.browser.database.SearchEngineDao
import com.logix.browser.database.TabDao
import com.logix.browser.network.NoopSafeBrowsingClient
import com.logix.browser.network.SafeBrowsingClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Faz-1 DI graph: Room database and DAOs. Feature/core classes use
 * `@Inject` constructors and need no manual bindings.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): BrowserDatabase =
        Room.databaseBuilder(
            context,
            BrowserDatabase::class.java,
            "logix-browser.db",
        ).addMigrations(MIGRATION_3_4)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()

    @Provides
    fun provideTabDao(db: BrowserDatabase): TabDao = db.tabDao()

    @Provides
    fun provideHistoryDao(db: BrowserDatabase): HistoryDao = db.historyDao()

    @Provides
    fun provideSearchEngineDao(db: BrowserDatabase): SearchEngineDao = db.searchEngineDao()

    @Provides
    fun provideAdBlockStatsDao(db: BrowserDatabase): AdBlockStatsDao = db.adBlockStatsDao()

    @Provides
    fun provideBookmarkDao(db: BrowserDatabase): BookmarkDao = db.bookmarkDao()

    @Provides
    fun provideDomainSettingsDao(db: BrowserDatabase): DomainSettingsDao = db.domainSettingsDao()

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Provides
    @Singleton
    fun provideSafeBrowsing(): SafeBrowsingClient = NoopSafeBrowsingClient()
}
