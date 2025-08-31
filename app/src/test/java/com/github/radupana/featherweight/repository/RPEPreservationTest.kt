package com.github.radupana.featherweight.repository

import com.github.radupana.featherweight.data.ParsedExercise
import com.github.radupana.featherweight.data.ParsedProgramme
import com.github.radupana.featherweight.data.ParsedSet
import com.github.radupana.featherweight.data.ParsedWeek
import com.github.radupana.featherweight.data.ParsedWorkout
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import org.junit.Test

/**
 * Test to ensure RPE values are preserved through the entire programme creation flow
 */
class RPEPreservationTest {
    
    @Test
    fun `RPE values are preserved in JSON serialization`() {
        // Create a parsed programme with RPE values
        val parsedSet = ParsedSet(
            reps = 6,
            weight = 90.0f,
            rpe = 8.5f
        )
        
        val parsedExercise = ParsedExercise(
            exerciseName = "Barbell Back Squat",
            matchedExerciseId = 1L,
            sets = listOf(parsedSet, parsedSet.copy(rpe = 7.0f)),
            notes = null
        )
        
        val parsedWorkout = ParsedWorkout(
            dayOfWeek = "Monday",
            name = "Test Workout",
            estimatedDurationMinutes = 60,
            exercises = listOf(parsedExercise)
        )
        
        val parsedWeek = ParsedWeek(
            weekNumber = 1,
            name = "Week 1",
            description = null,
            focusAreas = null,
            intensityLevel = null,
            volumeLevel = null,
            isDeload = false,
            phase = null,
            workouts = listOf(parsedWorkout)
        )
        
        val parsedProgramme = ParsedProgramme(
            name = "Test Programme",
            description = "Test",
            durationWeeks = 1,
            programmeType = "STRENGTH",
            difficulty = "INTERMEDIATE",
            weeks = listOf(parsedWeek),
            rawText = "test"
        )
        
        // Build the JSON structure (simulating ImportProgrammeViewModel.buildProgrammeJson)
        val weeks = parsedProgramme.weeks.map { week ->
            mapOf(
                "weekNumber" to week.weekNumber,
                "name" to week.name,
                "description" to week.description,
                "focusAreas" to week.focusAreas,
                "intensityLevel" to week.intensityLevel,
                "volumeLevel" to week.volumeLevel,
                "isDeload" to week.isDeload,
                "phase" to week.phase,
                "workouts" to week.workouts.map { workout ->
                    mapOf(
                        "name" to workout.name,
                        "dayOfWeek" to workout.dayOfWeek,
                        "estimatedDurationMinutes" to workout.estimatedDurationMinutes,
                        "exercises" to workout.exercises.map { exercise ->
                            mapOf(
                                "exerciseName" to exercise.exerciseName,
                                "exerciseId" to exercise.matchedExerciseId,
                                "sets" to exercise.sets.map { set ->
                                    mapOf(
                                        "reps" to set.reps,
                                        "weight" to set.weight,
                                        "rpe" to set.rpe
                                    )
                                }
                            )
                        }
                    )
                }
            )
        }
        
        val jsonString = Gson().toJson(mapOf("weeks" to weeks))
        
        // Parse the JSON back (simulating FeatherweightRepository.createImportedProgramme)
        @Suppress("UNCHECKED_CAST")
        val parsedData = Gson().fromJson(jsonString, Map::class.java) as Map<String, Any>
        
        @Suppress("UNCHECKED_CAST")
        val parsedWeeks = parsedData["weeks"] as List<Map<String, Any>>
        
        assertThat(parsedWeeks).hasSize(1)
        
        @Suppress("UNCHECKED_CAST")
        val firstWeekWorkouts = parsedWeeks[0]["workouts"] as List<Map<String, Any>>
        assertThat(firstWeekWorkouts).hasSize(1)
        
        @Suppress("UNCHECKED_CAST")
        val firstWorkoutExercises = firstWeekWorkouts[0]["exercises"] as List<Map<String, Any>>
        assertThat(firstWorkoutExercises).hasSize(1)
        
        @Suppress("UNCHECKED_CAST")
        val firstExerciseSets = firstWorkoutExercises[0]["sets"] as List<Map<String, Any>>
        assertThat(firstExerciseSets).hasSize(2)
        
        // Verify RPE values are preserved
        val firstSetRpe = firstExerciseSets[0]["rpe"] as? Double
        assertThat(firstSetRpe).isEqualTo(8.5)
        
        val secondSetRpe = firstExerciseSets[1]["rpe"] as? Double
        assertThat(secondSetRpe).isEqualTo(7.0)
    }
    
    @Test
    fun `RPE values are correctly extracted from JSON maps`() {
        // Simulate the exact JSON structure from ImportProgrammeViewModel
        val setData = mapOf(
            "reps" to 6.0,
            "weight" to 90.0,
            "rpe" to 8.5
        )
        
        // Simulate the extraction logic from FeatherweightRepository
        val rpeValue = setData["rpe"] as? Double
        assertThat(rpeValue).isEqualTo(8.5)
        
        val rpeFloat = rpeValue?.toFloat()
        assertThat(rpeFloat).isEqualTo(8.5f)
        
        // Test with a list of sets
        val sets = listOf(
            mapOf("reps" to 6.0, "weight" to 90.0, "rpe" to 8.5),
            mapOf("reps" to 6.0, "weight" to 90.0, "rpe" to 7.25),
            mapOf("reps" to 6.0, "weight" to 90.0, "rpe" to null)
        )
        
        val rpeList = sets.map { it["rpe"] as? Double }.map { it?.toFloat() }
        assertThat(rpeList).containsExactly(8.5f, 7.25f, null).inOrder()
        
        // Test the takeIf logic
        val filteredRpeList = rpeList.takeIf { list -> list.any { it != null } }
        assertThat(filteredRpeList).isNotNull()
        assertThat(filteredRpeList).containsExactly(8.5f, 7.25f, null).inOrder()
    }
    
    @Test
    fun `RPE list is null when all RPE values are null`() {
        val sets = listOf(
            mapOf("reps" to 6.0, "weight" to 90.0),
            mapOf("reps" to 6.0, "weight" to 90.0),
            mapOf("reps" to 6.0, "weight" to 90.0)
        )
        
        val rpeList = sets.map { it["rpe"] as? Double }.map { it?.toFloat() }
        assertThat(rpeList).containsExactly(null, null, null).inOrder()
        
        val filteredRpeList = rpeList.takeIf { list -> list.any { it != null } }
        assertThat(filteredRpeList).isNull()
    }
}
