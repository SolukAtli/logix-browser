package com.logix.browser.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Veri koruyan geçişler. Yıkıcı geçiş yok: sürüm atlarken
 * geçmiş, yer imleri ve sekmeler silinmez.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `domain_settings` (" +
                "`host` TEXT NOT NULL, `javaScript` INTEGER, `adBlock` INTEGER, " +
                "PRIMARY KEY(`host`))",
        )
    }
}
