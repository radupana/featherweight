package com.github.radupana.featherweight.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.github.radupana.featherweight.util.IdGenerator

@Entity(
    tableName = "template_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutTemplate::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["templateId"]),
        Index(value = ["exerciseVariationId"]),
        Index("userId"),
    ],
)
data class TemplateExercise(
    @PrimaryKey val id: String = IdGenerator.generateId(),
    val userId: String,
    val templateId: String,
    val exerciseVariationId: String,
    val exerciseOrder: Int,
    val supersetGroup: Int? = null,
    val notes: String? = null,
)
