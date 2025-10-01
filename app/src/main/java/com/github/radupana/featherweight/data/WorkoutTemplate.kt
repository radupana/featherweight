package com.github.radupana.featherweight.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.github.radupana.featherweight.util.IdGenerator
import java.time.LocalDateTime

@Entity(
    tableName = "workout_templates",
    indices = [Index("userId")],
)
data class WorkoutTemplate(
    @PrimaryKey val id: String = IdGenerator.generateId(),
    val userId: String,
    val name: String,
    val description: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)
