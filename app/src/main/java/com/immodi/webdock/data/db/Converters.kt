package com.immodi.webdock.data.db

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json

class Converters {

    @TypeConverter
    fun fromStringList(value: List<String>): String = Json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        runCatching { Json.decodeFromString<List<String>>(value) }.getOrDefault(emptyList())
}
