package com.github.radupana.featherweight.data

import androidx.room.TypeConverter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object DateConverters {
    @TypeConverter
    fun fromTimestamp(value: String?): LocalDateTime? =
        value?.let { LocalDateTime.parse(it, DateTimeFormatter.ISO_LOCAL_DATE_TIME) }

    @TypeConverter
    fun dateToTimestamp(date: LocalDateTime?): String? =
        date?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
}