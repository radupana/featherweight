package com.github.radupana.featherweight.data.exercise

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

/**
 * DAO for exercise operations.
 * This DAO now works with the normalized schema where ExerciseVariation is the central entity.
 */
@Dao
interface ExerciseDao {
    // ============== ExerciseVariation operations ==============

    @Query("SELECT * FROM exercise_variations ORDER BY usageCount DESC, name ASC")
    suspend fun getAllExercises(): List<ExerciseVariation>

    @Query("SELECT * FROM exercise_variations WHERE id = :id")
    suspend fun getExerciseById(id: Long): ExerciseVariation?

    @Query("SELECT * FROM exercise_variations WHERE id = :id")
    suspend fun getExerciseVariationById(id: Long): ExerciseVariation?

    @Query("SELECT * FROM exercise_variations WHERE name LIKE '%' || :query || '%' ORDER BY usageCount DESC, name ASC")
    suspend fun searchExercises(query: String): List<ExerciseVariation>

    @Query("SELECT * FROM exercise_variations WHERE name LIKE '%' || :query || '%' ORDER BY usageCount DESC, name ASC")
    suspend fun searchVariations(query: String): List<ExerciseVariation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseVariation): Long

    @Update
    suspend fun updateExercise(exercise: ExerciseVariation)

    @Query("UPDATE exercise_variations SET usageCount = usageCount + 1 WHERE id = :exerciseId")
    suspend fun incrementUsageCount(exerciseId: Long)

    @Query("UPDATE exercise_variations SET usageCount = 0")
    suspend fun resetAllUsageCounts()

    // ============== Find exercise by name or alias ==============

    @Query("SELECT * FROM exercise_variations WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun findExerciseByExactName(name: String): ExerciseVariation?

    @Query("SELECT * FROM exercise_variations WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun findVariationByExactName(name: String): ExerciseVariation?

    @Query(
        """
        SELECT v.* FROM exercise_variations v
        INNER JOIN variation_aliases a ON v.id = a.variationId
        WHERE LOWER(a.alias) = LOWER(:alias)
        LIMIT 1
    """,
    )
    suspend fun findExerciseByAlias(alias: String): ExerciseVariation?

    @Query(
        """
        SELECT v.* FROM exercise_variations v
        INNER JOIN variation_aliases a ON v.id = a.variationId
        WHERE LOWER(a.alias) = LOWER(:alias)
        LIMIT 1
    """,
    )
    suspend fun findVariationByAlias(alias: String): ExerciseVariation?

    @Query(
        """
        SELECT v.* FROM exercise_variations v
        LEFT JOIN variation_aliases a ON v.id = a.variationId
        WHERE LOWER(v.name) = LOWER(:searchTerm) OR LOWER(a.alias) = LOWER(:searchTerm)
        LIMIT 1
    """,
    )
    suspend fun findExerciseByNameOrAlias(searchTerm: String): ExerciseVariation?

    // ============== Big 4 exercises ==============

    @Query(
        """
        SELECT v.* FROM exercise_variations v
        WHERE v.name IN ('Barbell Back Squat', 'Barbell Deadlift', 'Barbell Bench Press', 'Barbell Overhead Press')
        ORDER BY 
            CASE v.name
                WHEN 'Barbell Back Squat' THEN 1
                WHEN 'Barbell Deadlift' THEN 2
                WHEN 'Barbell Bench Press' THEN 3
                WHEN 'Barbell Overhead Press' THEN 4
            END
    """,
    )
    suspend fun getBig4Exercises(): List<ExerciseVariation>

    // ============== Muscle group queries ==============

    @Query(
        """
        SELECT DISTINCT v.* FROM exercise_variations v
        INNER JOIN variation_muscles m ON v.id = m.variationId
        WHERE m.muscle = :muscleGroup
        ORDER BY v.usageCount DESC, v.name ASC
    """,
    )
    suspend fun getExercisesByMuscleGroup(muscleGroup: MuscleGroup): List<ExerciseVariation>

    @Query(
        """
        SELECT DISTINCT v.* FROM exercise_variations v
        INNER JOIN variation_muscles m ON v.id = m.variationId
        WHERE m.muscle = :muscleGroup
        ORDER BY v.usageCount DESC, v.name ASC
    """,
    )
    suspend fun getVariationsByMuscleGroup(muscleGroup: String): List<ExerciseVariation>

    @Query(
        """
        SELECT DISTINCT v.* FROM exercise_variations v
        INNER JOIN variation_muscles m ON v.id = m.variationId
        WHERE m.muscle = :muscleGroup AND m.isPrimary = 1
        ORDER BY v.usageCount DESC, v.name ASC
    """,
    )
    suspend fun getExercisesByPrimaryMuscleGroup(muscleGroup: MuscleGroup): List<ExerciseVariation>

    // ============== Equipment queries ==============

    @Query("SELECT * FROM exercise_variations WHERE equipment = :equipment ORDER BY usageCount DESC, name ASC")
    suspend fun getExercisesByEquipment(equipment: Equipment): List<ExerciseVariation>

    @Query("SELECT * FROM exercise_variations WHERE equipment = :equipment ORDER BY usageCount DESC, name ASC")
    suspend fun getVariationsByEquipment(equipment: Equipment): List<ExerciseVariation>

    // ============== Category queries ==============

    @Query(
        """
        SELECT v.* FROM exercise_variations v
        INNER JOIN exercise_cores c ON v.coreExerciseId = c.id
        WHERE c.category = :category
        ORDER BY v.usageCount DESC, v.name ASC
    """,
    )
    suspend fun getExercisesByCategory(category: ExerciseCategory): List<ExerciseVariation>

    @Query(
        """
        SELECT v.* FROM exercise_variations v
        INNER JOIN exercise_cores c ON v.coreExerciseId = c.id
        WHERE c.category = :category
        ORDER BY v.usageCount DESC, v.name ASC
    """,
    )
    suspend fun getVariationsByCategory(category: ExerciseCategory): List<ExerciseVariation>

    // ============== Difficulty queries ==============

    @Query("SELECT * FROM exercise_variations WHERE difficulty <= :maxDifficulty ORDER BY difficulty ASC, name ASC")
    suspend fun getExercisesByMaxDifficulty(maxDifficulty: ExerciseDifficulty): List<ExerciseVariation>

    // ============== Alias operations ==============

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAliases(aliases: List<VariationAlias>)

    @Query("SELECT * FROM variation_aliases WHERE variationId = :exerciseId")
    suspend fun getAliasesForExercise(exerciseId: Long): List<VariationAlias>

    @Query("SELECT * FROM variation_aliases WHERE variationId = :variationId")
    suspend fun getAliasesForVariation(variationId: Long): List<VariationAlias>

    @Query("SELECT * FROM variation_aliases")
    suspend fun getAllAliases(): List<VariationAlias>

    // ============== Comprehensive exercise details ==============

    @Transaction
    suspend fun getExerciseWithDetails(id: Long): ExerciseWithDetails? {
        val variation = getExerciseById(id) ?: return null
        val muscles = getMusclesForVariation(id)
        val aliases = getAliasesForExercise(id)
        val instructions = getInstructionsForVariation(id)

        return ExerciseWithDetails(
            variation = variation,
            muscles = muscles,
            aliases = aliases,
            instructions = instructions,
        )
    }

    @Transaction
    suspend fun getAllExercisesWithDetails(): List<ExerciseWithDetails> =
        getAllExercises().map { variation ->
            ExerciseWithDetails(
                variation = variation,
                muscles = getMusclesForVariation(variation.id),
                aliases = getAliasesForExercise(variation.id),
                instructions = getInstructionsForVariation(variation.id),
            )
        }

    // Helper queries for comprehensive details
    @Query("SELECT * FROM variation_muscles WHERE variationId = :variationId")
    suspend fun getMusclesForVariation(variationId: Long): List<VariationMuscle>

    @Query("SELECT * FROM variation_instructions WHERE variationId = :variationId ORDER BY instructionType, orderIndex")
    suspend fun getInstructionsForVariation(variationId: Long): List<VariationInstruction>
}
