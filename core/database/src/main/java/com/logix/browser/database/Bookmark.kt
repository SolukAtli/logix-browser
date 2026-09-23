package com.logix.browser.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Kayıtlı yer imi. URL benzersizdir, başlık son ziyarette güncellenir.
 */
@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey val url: String,
    val title: String,
    val createdAt: Long,
)
