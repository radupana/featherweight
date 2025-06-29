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
    // Usage tracking
    val usageCount: Int = 0,
    // Timestamps
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    // wger.de integration fields
    val wgerId: Int? = null,
    val wgerUuid: String? = null,
    val wgerCategoryId: Int? = null,
    val wgerLicenseId: Int? = null,
    val wgerLicenseAuthor: String? = null,
    val wgerStatus: String? = null,
    val wgerCreationDate: String? = null,
    val wgerUpdateDate: String? = null,
    val lastSyncedAt: LocalDateTime? = null,
    val syncSource: String = "local",
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
    val source: String = "common"
)

@Entity(tableName = "wger_muscles")
data class WgerMuscle(
    @PrimaryKey val id: Int,
    val name: String,
    val nameEn: String,
    val isFront: Boolean,
    val imageUrlMain: String?,
    val imageUrlSecondary: String?,
    val mappedMuscleGroup: MuscleGroup?
)

@Entity(tableName = "wger_categories")
data class WgerCategory(
    @PrimaryKey val id: Int,
    val name: String,
    val mappedCategory: ExerciseCategory?
)

@Entity(
    tableName = "wger_exercise_muscles",
    primaryKeys = ["exerciseId", "muscleId"],
    foreignKeys = [
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = WgerMuscle::class,
            parentColumns = ["id"],
            childColumns = ["muscleId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class WgerExerciseMuscle(
    val exerciseId: Long,
    val muscleId: Int,
    val isPrimary: Boolean
)
