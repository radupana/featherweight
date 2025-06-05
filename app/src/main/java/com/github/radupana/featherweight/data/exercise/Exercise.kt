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
    val type: ExerciseType = ExerciseType.STRENGTH,
    val difficulty: ExerciseDifficulty = ExerciseDifficulty.BEGINNER,
    // Instructions and guidance
    val instructions: String? = null,
    val tips: String? = null,
    val commonMistakes: String? = null,
    val safetyNotes: String? = null,
    // Media
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val demonstrationUrl: String? = null,
    // Exercise relationships (stored as JSON arrays of IDs)
    val variations: String? = null, // JSON array: "[1,2,3]"
    val alternatives: String? = null, // JSON array: "[4,5,6]"
    val prerequisites: String? = null, // JSON array: "[7,8]"
    // Metadata
    val isCustom: Boolean = false,
    val createdBy: String? = null, // User ID for custom exercises
    val isPublic: Boolean = true, // For community sharing
    val tags: String? = null, // JSON array: "['beginner-friendly','home']"
    // Timestamps
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)

// Junction tables for many-to-many relationships
@Entity(
    tableName = "exercise_muscle_groups",
    primaryKeys = ["exerciseId", "muscleGroup", "isPrimary"],
    foreignKeys = [
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ExerciseMuscleGroup(
    val exerciseId: Long,
    val muscleGroup: MuscleGroup,
    val isPrimary: Boolean, // true for primary muscles, false for secondary
)

@Entity(
    tableName = "exercise_equipment",
    primaryKeys = ["exerciseId", "equipment"],
    foreignKeys = [
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ExerciseEquipment(
    val exerciseId: Long,
    val equipment: Equipment,
    val isRequired: Boolean = true, // false for optional equipment
    val isAlternative: Boolean = false, // true for alternative equipment options
)

@Entity(
    tableName = "exercise_movement_patterns",
    primaryKeys = ["exerciseId", "movementPattern"],
    foreignKeys = [
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ExerciseMovementPattern(
    val exerciseId: Long,
    val movementPattern: MovementPattern,
    val isPrimary: Boolean = true,
)

// Data classes for working with complete exercise data
data class ExerciseWithDetails(
    @Embedded val exercise: Exercise,
    @Relation(
        parentColumn = "id",
        entityColumn = "exerciseId",
    )
    val muscleGroups: List<ExerciseMuscleGroup>,
    @Relation(
        parentColumn = "id",
        entityColumn = "exerciseId",
    )
    val equipment: List<ExerciseEquipment>,
    @Relation(
        parentColumn = "id",
        entityColumn = "exerciseId",
    )
    val movementPatterns: List<ExerciseMovementPattern>,
) {
    val primaryMuscles: Set<MuscleGroup>
        get() = muscleGroups.filter { it.isPrimary }.map { it.muscleGroup }.toSet()

    val secondaryMuscles: Set<MuscleGroup>
        get() = muscleGroups.filter { !it.isPrimary }.map { it.muscleGroup }.toSet()

    val requiredEquipment: Set<Equipment>
        get() = equipment.filter { it.isRequired && !it.isAlternative }.map { it.equipment }.toSet()

    val optionalEquipment: Set<Equipment>
        get() = equipment.filter { !it.isRequired }.map { it.equipment }.toSet()

    val alternativeEquipment: Set<Equipment>
        get() = equipment.filter { it.isAlternative }.map { it.equipment }.toSet()

    val primaryMovements: Set<MovementPattern>
        get() = movementPatterns.filter { it.isPrimary }.map { it.movementPattern }.toSet()
}
