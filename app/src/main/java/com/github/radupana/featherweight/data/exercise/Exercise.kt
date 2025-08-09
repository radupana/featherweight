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
    val name: String,
    val category: ExerciseCategory,
    val equipment: Equipment,
    val muscleGroup: String,
    val movementPattern: String,
    val type: ExerciseType = ExerciseType.STRENGTH,
    val difficulty: ExerciseDifficulty = ExerciseDifficulty.BEGINNER,
    val requiresWeight: Boolean = true,
    val instructions: String? = null,
    val usageCount: Int = 0,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)

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
    val confidence: Float = 1.0f,
    val exactMatchOnly: Boolean = false,
    val source: String = "common",
)
