package com.github.radupana.featherweight.data.exercise

import androidx.room.*

@Dao
interface ExerciseDao {
    // Basic queries
    @Transaction
    @Query("SELECT * FROM exercises ORDER BY name ASC")
    suspend fun getAllExercisesWithDetails(): List<ExerciseWithDetails>

    @Transaction
    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExerciseWithDetails(id: Long): ExerciseWithDetails?

    @Query("SELECT * FROM exercises WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun searchExercises(query: String): List<Exercise>

    @Transaction
    @Query(
        """
        SELECT DISTINCT e.* FROM exercises e
        INNER JOIN exercise_muscle_groups emg ON e.id = emg.exerciseId
        WHERE emg.muscleGroup = :muscleGroup
        ORDER BY e.name ASC
    """,
    )
    suspend fun getExercisesByMuscleGroup(muscleGroup: MuscleGroup): List<ExerciseWithDetails>

    @Transaction
    @Query(
        """
        SELECT DISTINCT e.* FROM exercises e
        INNER JOIN exercise_equipment ee ON e.id = ee.exerciseId  
        WHERE ee.equipment IN (:equipment)
        ORDER BY e.name ASC
    """,
    )
    suspend fun getExercisesByEquipment(equipment: List<Equipment>): List<ExerciseWithDetails>

    @Query("SELECT * FROM exercises WHERE category = :category ORDER BY name ASC")
    suspend fun getExercisesByCategory(category: ExerciseCategory): List<Exercise>

    @Query("SELECT * FROM exercises WHERE difficulty <= :maxDifficulty ORDER BY difficulty ASC, name ASC")
    suspend fun getExercisesByMaxDifficulty(maxDifficulty: ExerciseDifficulty): List<Exercise>

    // Custom exercises
    @Query("SELECT * FROM exercises WHERE isCustom = 1 AND createdBy = :userId ORDER BY name ASC")
    suspend fun getCustomExercises(userId: String): List<Exercise>

    // Insert operations
    @Insert
    suspend fun insertExercise(exercise: Exercise): Long

    @Insert
    suspend fun insertMuscleGroups(muscleGroups: List<ExerciseMuscleGroup>)

    @Insert
    suspend fun insertEquipment(equipment: List<ExerciseEquipment>)

    @Insert
    suspend fun insertMovementPatterns(patterns: List<ExerciseMovementPattern>)

    // Transaction for creating complete exercise
    @Transaction
    suspend fun insertExerciseWithDetails(
        exercise: Exercise,
        muscleGroups: List<ExerciseMuscleGroup>,
        equipment: List<ExerciseEquipment>,
        movementPatterns: List<ExerciseMovementPattern>,
    ): Long {
        val exerciseId = insertExercise(exercise)
        insertMuscleGroups(muscleGroups.map { it.copy(exerciseId = exerciseId) })
        insertEquipment(equipment.map { it.copy(exerciseId = exerciseId) })
        insertMovementPatterns(movementPatterns.map { it.copy(exerciseId = exerciseId) })
        return exerciseId
    }

    // Update operations
    @Update
    suspend fun updateExercise(exercise: Exercise)

    @Query("DELETE FROM exercise_muscle_groups WHERE exerciseId = :exerciseId")
    suspend fun deleteMuscleGroups(exerciseId: Long)

    @Query("DELETE FROM exercise_equipment WHERE exerciseId = :exerciseId")
    suspend fun deleteEquipment(exerciseId: Long)

    @Query("DELETE FROM exercise_movement_patterns WHERE exerciseId = :exerciseId")
    suspend fun deleteMovementPatterns(exerciseId: Long)

    // Advanced filtering
    @Transaction
    @Query(
        """
        SELECT DISTINCT e.* FROM exercises e
        LEFT JOIN exercise_muscle_groups emg ON e.id = emg.exerciseId
        LEFT JOIN exercise_equipment ee ON e.id = ee.exerciseId
        WHERE 
            (:category IS NULL OR e.category = :category) AND
            (:muscleGroup IS NULL OR emg.muscleGroup = :muscleGroup) AND
            (:equipment IS NULL OR ee.equipment IN (:availableEquipment)) AND
            (:maxDifficulty IS NULL OR e.difficulty <= :maxDifficulty) AND
            (:includeCustom = 1 OR e.isCustom = 0) AND
            (:searchQuery = '' OR e.name LIKE '%' || :searchQuery || '%')
        ORDER BY e.name ASC
    """,
    )
    suspend fun getFilteredExercises(
        category: ExerciseCategory? = null,
        muscleGroup: MuscleGroup? = null,
        equipment: Equipment? = null,
        availableEquipment: List<Equipment> = emptyList(),
        maxDifficulty: ExerciseDifficulty? = null,
        includeCustom: Boolean = true,
        searchQuery: String = "",
    ): List<ExerciseWithDetails>
}

// Type converters for Room
class ExerciseTypeConverters {
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
    fun fromMuscleGroup(muscleGroup: MuscleGroup): String = muscleGroup.name

    @TypeConverter
    fun toMuscleGroup(muscleGroup: String): MuscleGroup = MuscleGroup.valueOf(muscleGroup)

    @TypeConverter
    fun fromEquipment(equipment: Equipment): String = equipment.name

    @TypeConverter
    fun toEquipment(equipment: String): Equipment = Equipment.valueOf(equipment)

    @TypeConverter
    fun fromMovementPattern(pattern: MovementPattern): String = pattern.name

    @TypeConverter
    fun toMovementPattern(pattern: String): MovementPattern = MovementPattern.valueOf(pattern)
}
