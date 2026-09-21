package com.logix.browser.di

import android.content.Context
import androidx.room.Room
import com.logix.browser.database.BrowserDatabase
import com.logix.browser.database.HistoryDao
import com.logix.browser.database.SearchEngineDao
import com.logix.browser.database.TabDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

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
        ).build()

    @Provides
    fun provideTabDao(db: BrowserDatabase): TabDao = db.tabDao()

    @Provides
    fun provideHistoryDao(db: BrowserDatabase): HistoryDao = db.historyDao()

    @Provides
    fun provideSearchEngineDao(db: BrowserDatabase): SearchEngineDao = db.searchEngineDao()
}
