package com.github.radupana.featherweight.data.exercise

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.time.LocalDateTime

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String, // Canonical name following our naming convention
    val category: ExerciseCategory,
    val equipment: Equipment,
    val muscleGroup: String, // Primary muscle group
    val movementPattern: String, // e.g., "Squat", "Press", "Row"
    val type: ExerciseType = ExerciseType.STRENGTH,
    val difficulty: ExerciseDifficulty = ExerciseDifficulty.BEGINNER,
    val requiresWeight: Boolean = true,
    val instructions: String? = null,
    val usageCount: Int = 0,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)

// Simplified data class for exercise with aliases
data class ExerciseWithDetails(
    @Embedded val exercise: Exercise,
    @Relation(
        parentColumn = "id",
        entityColumn = "exerciseId",
    )
    val aliases: List<ExerciseAlias>,
)

@Entity(
    tableName = "exercise_aliases",
    primaryKeys = ["exerciseId", "alias"],
    foreignKeys = [
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ExerciseAlias(
    val exerciseId: Long,
    val alias: String,
    // Confidence score for fuzzy matching (0.0 to 1.0)
    val confidence: Float = 1.0f,
    // Whether this alias should be exact match only
    val exactMatchOnly: Boolean = false,
    // Source of the alias (e.g., "common", "ai", "user")
    val source: String = "common",
)
