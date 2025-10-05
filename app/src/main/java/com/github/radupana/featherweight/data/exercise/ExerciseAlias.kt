package com.github.radupana.featherweight.data.exercise

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.github.radupana.featherweight.util.IdGenerator

/**
 * Alternative names for exercise variations.
 * Used for exercise matching and search.
 */
@Entity(
    tableName = "exercise_aliases",
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
        Index(value = ["alias"]),
    ],
)
data class ExerciseAlias(
    @PrimaryKey
    val id: String = IdGenerator.generateId(),
    val exerciseId: String,
    val alias: String,
)
