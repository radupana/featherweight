package com.github.radupana.featherweight.data.exercise

import androidx.room.TypeConverter
import com.github.radupana.featherweight.data.ParseStatus
import com.github.radupana.featherweight.data.profile.OneRMType

/**
 * Type converters for the normalized exercise schema.
 * Handles lists, maps, and complex types for Room database.
 */
class ExerciseTypeConverters {
    // Existing simple enum converters
    @TypeConverter
    fun fromExerciseCategory(category: ExerciseCategory): String = category.name

    @TypeConverter
    fun toExerciseCategory(category: String): ExerciseCategory = ExerciseCategory.valueOf(category)

    @TypeConverter
    fun fromExerciseDifficulty(difficulty: ExerciseDifficulty): String = difficulty.name

    @TypeConverter
    fun toExerciseDifficulty(difficulty: String): ExerciseDifficulty = ExerciseDifficulty.valueOf(difficulty)

    @TypeConverter
    fun fromEquipment(equipment: Equipment): String = equipment.name

    @TypeConverter
    fun toEquipment(equipment: String): Equipment = Equipment.valueOf(equipment)

    @TypeConverter
    fun fromMovementPattern(pattern: MovementPattern): String = pattern.name

    @TypeConverter
    fun toMovementPattern(pattern: String): MovementPattern = MovementPattern.valueOf(pattern)

    @TypeConverter
    fun fromInstructionType(type: InstructionType): String = type.name

    @TypeConverter
    fun toInstructionType(type: String): InstructionType = InstructionType.valueOf(type)

    @TypeConverter
    fun fromExerciseRelationType(type: ExerciseRelationType): String = type.name

    @TypeConverter
    fun toExerciseRelationType(type: String): ExerciseRelationType = ExerciseRelationType.valueOf(type)

    @TypeConverter
    fun fromOneRMType(type: OneRMType): String = type.name

    @TypeConverter
    fun toOneRMType(type: String): OneRMType = OneRMType.valueOf(type)

    @TypeConverter
    fun fromParseStatus(status: ParseStatus): String = status.name

    @TypeConverter
    fun toParseStatus(status: String): ParseStatus = ParseStatus.valueOf(status)
}
