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
    // false for bodyweight exercises like ab roll
    val requiresWeight: Boolean = true,
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
    // JSON array: "[1,2,3]"
    val variations: String? = null,
    // JSON array: "[4,5,6]"
    val alternatives: String? = null,
    // JSON array: "[7,8]"
    val prerequisites: String? = null,
    // Metadata
    val isCustom: Boolean = false,
    // User ID for custom exercises
    val createdBy: String? = null,
    // For community sharing
    val isPublic: Boolean = true,
    // JSON array: "['beginner-friendly','home']"
    val tags: String? = null,
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
    // true for primary muscles, false for secondary
    val isPrimary: Boolean,
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
    // false for optional equipment
    val isRequired: Boolean = true,
    // true for alternative equipment options
    val isAlternative: Boolean = false,
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
