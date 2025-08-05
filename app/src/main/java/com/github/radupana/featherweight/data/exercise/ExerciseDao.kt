package com.github.radupana.featherweight.data.exercise

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.TypeConverter
import androidx.room.Update
import com.github.radupana.featherweight.data.profile.OneRMType

@Dao
interface ExerciseDao {
    // Basic queries
    @Transaction
    @Query("SELECT * FROM exercises ORDER BY usageCount DESC, name ASC")
    suspend fun getAllExercisesWithDetails(): List<ExerciseWithDetails>

    @Transaction
    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExerciseWithDetails(id: Long): ExerciseWithDetails?

    @Query("SELECT * FROM exercises WHERE name LIKE '%' || :query || '%' ORDER BY usageCount DESC, name ASC")
    suspend fun searchExercises(query: String): List<Exercise>

    @Query("SELECT * FROM exercises WHERE muscleGroup = :muscleGroup ORDER BY usageCount DESC, name ASC")
    suspend fun getExercisesByMuscleGroup(muscleGroup: String): List<Exercise>

    @Query("SELECT * FROM exercises WHERE equipment = :equipment ORDER BY usageCount DESC, name ASC")
    suspend fun getExercisesByEquipment(equipment: Equipment): List<Exercise>

    @Query("SELECT * FROM exercises WHERE category = :category ORDER BY usageCount DESC, name ASC")
    suspend fun getExercisesByCategory(category: ExerciseCategory): List<Exercise>

    @Query("SELECT * FROM exercises WHERE difficulty <= :maxDifficulty ORDER BY difficulty ASC, name ASC")
    suspend fun getExercisesByMaxDifficulty(maxDifficulty: ExerciseDifficulty): List<Exercise>

    // Insert operations
    @Insert
    suspend fun insertExercise(exercise: Exercise): Long

    // Update operations
    @Update
    suspend fun updateExercise(exercise: Exercise)

    @Query("UPDATE exercises SET usageCount = usageCount + 1 WHERE id = :exerciseId")
    suspend fun incrementUsageCount(exerciseId: Long)

    // Alias operations
    @Insert
    suspend fun insertAliases(aliases: List<ExerciseAlias>)

    @Query("SELECT * FROM exercise_aliases WHERE exerciseId = :exerciseId")
    suspend fun getAliasesForExercise(exerciseId: Long): List<ExerciseAlias>

    @Query("SELECT * FROM exercise_aliases WHERE alias = :alias")
    suspend fun getExerciseByAlias(alias: String): ExerciseAlias?

    @Query(
        """
        SELECT e.* FROM exercises e
        INNER JOIN exercise_aliases ea ON e.id = ea.exerciseId
        WHERE LOWER(ea.alias) = LOWER(:alias)
        LIMIT 1
    """,
    )
    suspend fun findExerciseByAlias(alias: String): Exercise?

    @Query(
        """
        SELECT e.* FROM exercises e
        WHERE LOWER(e.name) = LOWER(:name)
        LIMIT 1
    """,
    )
    suspend fun findExerciseByExactName(name: String): Exercise?

    @Query(
        """
        SELECT e.* FROM exercises e
        LEFT JOIN exercise_aliases ea ON e.id = ea.exerciseId
        WHERE LOWER(e.name) = LOWER(:searchTerm) OR LOWER(ea.alias) = LOWER(:searchTerm)
        LIMIT 1
    """,
    )
    suspend fun findExerciseByNameOrAlias(searchTerm: String): Exercise?

    @Query(
        """
        SELECT * FROM exercises 
        WHERE name IN ('Barbell Back Squat', 'Barbell Deadlift', 'Barbell Bench Press', 'Barbell Overhead Press')
        ORDER BY 
            CASE name
                WHEN 'Barbell Back Squat' THEN 1
                WHEN 'Barbell Deadlift' THEN 2
                WHEN 'Barbell Bench Press' THEN 3
                WHEN 'Barbell Overhead Press' THEN 4
            END
    """,
    )
    suspend fun getBig4Exercises(): List<Exercise>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExerciseById(id: Long): Exercise?
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
    fun fromEquipment(equipment: Equipment): String = equipment.name

    @TypeConverter
    fun toEquipment(equipment: String): Equipment = Equipment.valueOf(equipment)

    @TypeConverter
    fun fromOneRMType(type: OneRMType): String = type.name

    @TypeConverter
    fun toOneRMType(type: String): OneRMType = OneRMType.valueOf(type)
}
