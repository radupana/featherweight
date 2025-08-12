package com.github.radupana.featherweight.data.exercise

import androidx.room.TypeConverter
import com.github.radupana.featherweight.data.profile.OneRMType

/**
 * Enhanced type converters for the normalized exercise schema.
 * Handles lists, maps, and complex types for Room database.
 */
class ExerciseTypeConvertersNew {
    // Existing simple enum converters
    @TypeConverter
    fun fromExerciseCategory(category: ExerciseCategory): String = category.name

    @TypeConverter
    fun toExerciseCategory(category: String): ExerciseCategory = ExerciseCategory.valueOf(category)

    @TypeConverter
    fun fromExerciseType(type: ExerciseType): String = type.name

    @TypeConverter
    fun toExerciseType(type: String): ExerciseType = ExerciseType.valueOf(type)

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

    // List converters for muscle groups
    @TypeConverter
    fun fromMuscleGroupList(muscles: List<MuscleGroup>): String = muscles.joinToString(",") { it.name }

    @TypeConverter
    fun toMuscleGroupList(musclesString: String): List<MuscleGroup> {
        if (musclesString.isBlank()) return emptyList()
        return musclesString.split(",").mapNotNull {
            try {
                MuscleGroup.valueOf(it.trim())
            } catch (e: IllegalArgumentException) {
                null // Skip invalid entries
            }
        }
    }

    // List converters for equipment
    @TypeConverter
    fun fromEquipmentList(equipment: List<Equipment>): String = equipment.joinToString(",") { it.name }

    @TypeConverter
    fun toEquipmentList(equipmentString: String): List<Equipment> {
        if (equipmentString.isBlank()) return emptyList()
        return equipmentString.split(",").mapNotNull {
            try {
                Equipment.valueOf(it.trim())
            } catch (e: IllegalArgumentException) {
                null // Skip invalid entries
            }
        }
    }

    // Map converter for muscle emphasis changes
    @TypeConverter
    fun fromMuscleEmphasisMap(map: Map<MuscleGroup, Float>): String {
        if (map.isEmpty()) return ""
        return map.entries.joinToString(";") { "${it.key.name}:${it.value}" }
    }

    @TypeConverter
    fun toMuscleEmphasisMap(mapString: String): Map<MuscleGroup, Float> {
        if (mapString.isBlank()) return emptyMap()
        return mapString
            .split(";")
            .mapNotNull { entry ->
                val parts = entry.split(":")
                if (parts.size == 2) {
                    try {
                        val muscle = MuscleGroup.valueOf(parts[0].trim())
                        val emphasis = parts[1].trim().toFloat()
                        muscle to emphasis
                    } catch (e: Exception) {
                        null // Skip invalid entries
                    }
                } else {
                    null
                }
            }.toMap()
    }
}
