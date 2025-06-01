package com.github.radupana.featherweight.data

import androidx.room.TypeConverter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object DateConverters {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    @TypeConverter
    fun fromTimestamp(value: String?): LocalDateTime? =
        value?.let { LocalDateTime.parse(it, formatter) }

    @TypeConverter
    fun dateToTimestamp(date: LocalDateTime?): String? =
        date?.format(formatter)
}
