package com.github.radupana.featherweight.data.exercise

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.github.radupana.featherweight.util.IdGenerator

/**
 * Muscles targeted by exercise variations.
 * Tracks primary and secondary muscle groups.
 */
@Entity(
    tableName = "exercise_muscles",
    foreignKeys = [
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["exerciseId"]),
        Index(value = ["muscle"]),
        Index(value = ["exerciseId", "muscle"], unique = true),
    ],
)
data class ExerciseMuscle(
    @PrimaryKey
    val id: String = IdGenerator.generateId(),
    val exerciseId: String,
    val muscle: String, // Was MuscleGroup enum
    val targetType: String, // "PRIMARY" or "SECONDARY"
    // Soft delete flag for sync
    val isDeleted: Boolean = false,
)
