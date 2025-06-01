package com.github.radupana.featherweight.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity
data class Workout(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDateTime,
    val notes: String? = null,
)
