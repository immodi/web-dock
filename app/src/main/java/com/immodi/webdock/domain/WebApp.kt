package com.immodi.webdock.domain

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "web_apps",
    indices = [Index(value = ["url"], unique = true)],
)
data class WebApp(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    /** Normalized start URL. Unique across the list. */
    val url: String,
    val name: String,
    val nameEdited: Boolean = false,
    val iconUrl: String? = null,
    val iconFile: String? = null,
    val themeColor: Int? = null,
    val backgroundColor: Int? = null,
    val tags: List<String> = emptyList(),
    val pinned: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastOpenedAt: Long = 0L,
    @Embedded(prefix = "settings_") val settings: AppSettings = AppSettings(),
)
