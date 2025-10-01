package com.github.radupana.featherweight.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.github.radupana.featherweight.util.IdGenerator

@Entity(
    tableName = "template_sets",
    foreignKeys = [
        ForeignKey(
            entity = TemplateExercise::class,
            parentColumns = ["id"],
            childColumns = ["templateExerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["templateExerciseId"]),
        Index("userId"),
    ],
)
data class TemplateSet(
    @PrimaryKey val id: String = IdGenerator.generateId(),
    val userId: String,
    val templateExerciseId: String,
    val setOrder: Int,
    val targetReps: Int,
    val targetWeight: Float? = null,
    val targetRpe: Float? = null,
    val notes: String? = null,
)
