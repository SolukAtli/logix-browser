package com.logix.browser.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Site başına geçersiz kılma. null = genel ayarı kullan.
 */
@Entity(tableName = "domain_settings")
data class DomainSetting(
    @PrimaryKey val host: String,
    val javaScript: Boolean? = null,
    val adBlock: Boolean? = null,
)
