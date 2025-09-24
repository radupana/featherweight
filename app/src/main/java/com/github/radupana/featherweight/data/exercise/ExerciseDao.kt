package com.github.radupana.featherweight.data.exercise

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction

/**
 * DAO for exercise operations.
 * This DAO now works with the normalized schema where ExerciseVariation is the central entity.
 */
@Dao
interface ExerciseDao {
    // ============== ExerciseVariation operations ==============

    @Query("SELECT * FROM exercise_variations ORDER BY name ASC")
    suspend fun getAllExercises(): List<ExerciseVariation>

    @Query("SELECT * FROM exercise_variations WHERE id = :id")
    suspend fun getExerciseById(id: Long): ExerciseVariation?

    @Query("SELECT * FROM exercise_variations WHERE id = :id")
    suspend fun getExerciseVariationById(id: Long): ExerciseVariation?

    @Query("SELECT * FROM exercise_variations WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun searchVariations(query: String): List<ExerciseVariation>

    // ============== Find exercise by name or alias ==============

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
    suspend fun findVariationByAlias(alias: String): ExerciseVariation?

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
        ORDER BY v.name ASC
    """,
    )
    suspend fun getVariationsByMuscleGroup(muscleGroup: String): List<ExerciseVariation>

    // ============== Equipment queries ==============

    @Query("SELECT * FROM exercise_variations WHERE equipment = :equipment ORDER BY name ASC")
    suspend fun getVariationsByEquipment(equipment: Equipment): List<ExerciseVariation>

    // ============== Category queries ==============

    @Query(
        """
        SELECT v.* FROM exercise_variations v
        INNER JOIN exercise_cores c ON v.coreExerciseId = c.id
        WHERE c.category = :category
        ORDER BY v.name ASC
    """,
    )
    suspend fun getVariationsByCategory(category: ExerciseCategory): List<ExerciseVariation>

    // ============== Alias operations ==============

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
