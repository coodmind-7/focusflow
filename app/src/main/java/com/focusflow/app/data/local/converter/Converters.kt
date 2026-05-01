package com.focusflow.app.data.local.converter

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Long = value ?: System.currentTimeMillis()

    @TypeConverter
    fun toTimestamp(value: Long): Long = value

    @TypeConverter
    fun fromStringList(value: String): List<String> =
        value.split(",").filter { it.isNotBlank() }

    @TypeConverter
    fun toStringList(value: List<String>): String = value.joinToString(",")
}
