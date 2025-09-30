package com.github.radupana.featherweight.repository

import com.github.radupana.featherweight.data.ParsedExercise
import com.github.radupana.featherweight.data.ParsedProgramme
import com.github.radupana.featherweight.data.ParsedSet
import com.github.radupana.featherweight.data.ParsedWeek
import com.github.radupana.featherweight.data.ParsedWorkout
import com.github.radupana.featherweight.data.programme.ExerciseStructure
import com.github.radupana.featherweight.data.programme.RepsStructure
import com.github.radupana.featherweight.data.programme.WorkoutStructure
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import kotlinx.serialization.json.Json
import org.junit.Test

class ProgrammeImportExerciseMatchingTest {
    @Test
    fun `exercise IDs are correctly preserved through programme import as Strings`() {
        val exerciseId1 = "2056159193"
        val exerciseId2 = "550361372"

        val parsedProgramme = createTestProgrammeWithMatchedExercises(exerciseId1, exerciseId2)
        val jsonStructure = buildProgrammeJson(parsedProgramme)

        val gson = Gson()
        val parsedData = gson.fromJson(jsonStructure, Map::class.java)

        @Suppress("UNCHECKED_CAST")
        val weeks = parsedData["weeks"] as List<Map<String, Any>>

        @Suppress("UNCHECKED_CAST")
        val workouts = weeks[0]["workouts"] as List<Map<String, Any>>

        @Suppress("UNCHECKED_CAST")
        val exercises = workouts[0]["exercises"] as List<Map<String, Any>>

        assertThat(exercises[0]["exerciseId"]).isEqualTo(exerciseId1)
        assertThat(exercises[1]["exerciseId"]).isEqualTo(exerciseId2)
    }

    @Test
    fun `workout structure correctly parses exercise IDs as Strings from JSON`() {
        val exerciseId = "test-uuid-123"
        val workoutJson =
            """
            {
                "day": 1,
                "name": "Test Workout",
                "exercises": [
                    {
                        "name": "Barbell Overhead Press",
                        "sets": 3,
                        "reps": {"value": 5},
                        "exerciseId": "$exerciseId",
                        "weights": [60.0, 60.0, 60.0],
                        "rpeValues": [8.0, 8.0, 8.0]
                    }
                ]
            }
            """.trimIndent()

        val json =
            Json {
                ignoreUnknownKeys = true
                isLenient = true
            }

        val workoutStructure = json.decodeFromString<WorkoutStructure>(workoutJson)

        assertThat(workoutStructure.exercises).hasSize(1)
        assertThat(workoutStructure.exercises[0].exerciseId).isEqualTo(exerciseId)
        assertThat(workoutStructure.exercises[0].name).isEqualTo("Barbell Overhead Press")
    }

    @Test
    fun `exercise IDs are extracted correctly as Strings when parsing imported programme JSON`() {
        val exerciseId = "abc-def-123"
        val programmeJson = createProgrammeJsonWithExerciseId(exerciseId)

        val gson = Gson()

        @Suppress("UNCHECKED_CAST")
        val parsedData = gson.fromJson(programmeJson, Map::class.java) as Map<String, Any>

        @Suppress("UNCHECKED_CAST")
        val weeks = parsedData["weeks"] as List<Map<String, Any>>

        @Suppress("UNCHECKED_CAST")
        val workouts = weeks[0]["workouts"] as List<Map<String, Any>>

        @Suppress("UNCHECKED_CAST")
        val exercises = workouts[0]["exercises"] as List<Map<String, Any>>

        val extractedId = exercises[0]["exerciseId"] as? String

        assertThat(extractedId).isEqualTo(exerciseId)
        assertThat(extractedId).isInstanceOf(String::class.java)
    }

    @Test
    fun `null exercise IDs are handled correctly`() {
        val parsedProgramme = createTestProgrammeWithMatchedExercises(null, "123")
        val jsonStructure = buildProgrammeJson(parsedProgramme)

        val gson = Gson()
        val parsedData = gson.fromJson(jsonStructure, Map::class.java)

        @Suppress("UNCHECKED_CAST")
        val weeks = parsedData["weeks"] as List<Map<String, Any>>

        @Suppress("UNCHECKED_CAST")
        val workouts = weeks[0]["workouts"] as List<Map<String, Any>>

        @Suppress("UNCHECKED_CAST")
        val exercises = workouts[0]["exercises"] as List<Map<String, Any>>

        assertThat(exercises[0]["exerciseId"]).isNull()
        assertThat(exercises[1]["exerciseId"]).isEqualTo("123")
    }

    @Test
    fun `ExerciseStructure correctly stores String exercise IDs`() {
        val exerciseId = "uuid-test-456"
        val structure =
            ExerciseStructure(
                name = "Test Exercise",
                sets = 3,
                reps = RepsStructure.Single(5),
                exerciseId = exerciseId,
                weights = listOf(100f, 100f, 100f),
                rpeValues = null,
            )

        assertThat(structure.exerciseId).isEqualTo(exerciseId)
        assertThat(structure.exerciseId).isInstanceOf(String::class.java)
    }

    private fun createTestProgrammeWithMatchedExercises(
        exerciseId1: String?,
        exerciseId2: String?,
    ): ParsedProgramme {
        val exercise1 =
            ParsedExercise(
                exerciseName = "Barbell Overhead Press",
                matchedExerciseId = exerciseId1,
                sets =
                    listOf(
                        ParsedSet(reps = 5, weight = 60f, rpe = 8f),
                        ParsedSet(reps = 5, weight = 60f, rpe = 8f),
                        ParsedSet(reps = 5, weight = 60f, rpe = 8f),
                    ),
                notes = null,
            )

        val exercise2 =
            ParsedExercise(
                exerciseName = "Leg Extensions",
                matchedExerciseId = exerciseId2,
                sets =
                    listOf(
                        ParsedSet(reps = 10, weight = 50f, rpe = 9f),
                        ParsedSet(reps = 10, weight = 50f, rpe = 9f),
                        ParsedSet(reps = 10, weight = 50f, rpe = 9f),
                        ParsedSet(reps = 10, weight = 50f, rpe = 9f),
                    ),
                notes = null,
            )

        val workout =
            ParsedWorkout(
                dayOfWeek = null,
                name = "Workout",
                estimatedDurationMinutes = 60,
                exercises = listOf(exercise1, exercise2),
            )

        val week =
            ParsedWeek(
                weekNumber = 1,
                name = "Week 1",
                description = null,
                focusAreas = null,
                intensityLevel = null,
                volumeLevel = null,
                isDeload = false,
                phase = null,
                workouts = listOf(workout),
            )

        return ParsedProgramme(
            name = "Test Programme",
            description = "Test",
            durationWeeks = 1,
            programmeType = "STRENGTH",
            difficulty = "INTERMEDIATE",
            weeks = listOf(week),
            rawText = "test",
        )
    }

    private fun buildProgrammeJson(programme: ParsedProgramme): String {
        val weeks =
            programme.weeks.map { week ->
                mapOf(
                    "weekNumber" to week.weekNumber,
                    "name" to week.name,
                    "description" to week.description,
                    "focusAreas" to week.focusAreas,
                    "intensityLevel" to week.intensityLevel,
                    "volumeLevel" to week.volumeLevel,
                    "isDeload" to week.isDeload,
                    "phase" to week.phase,
                    "workouts" to
                        week.workouts.map { workout ->
                            mapOf(
                                "name" to workout.name,
                                "dayOfWeek" to workout.dayOfWeek,
                                "estimatedDurationMinutes" to workout.estimatedDurationMinutes,
                                "exercises" to
                                    workout.exercises.map { exercise ->
                                        mapOf(
                                            "exerciseName" to exercise.exerciseName,
                                            "exerciseId" to exercise.matchedExerciseId,
                                            "sets" to
                                                exercise.sets.map { set ->
                                                    mapOf(
                                                        "reps" to set.reps,
                                                        "weight" to set.weight,
                                                        "rpe" to set.rpe,
                                                    )
                                                },
                                        )
                                    },
                            )
                        },
                )
            }
        return Gson().toJson(mapOf("weeks" to weeks))
    }

    private fun createProgrammeJsonWithExerciseId(exerciseId: String): String =
        """
        {
            "weeks": [
                {
                    "weekNumber": 1,
                    "name": "Week 1",
                    "workouts": [
                        {
                            "name": "Test Workout",
                            "exercises": [
                                {
                                    "exerciseName": "Test Exercise",
                                    "exerciseId": "$exerciseId",
                                    "sets": [
                                        {"reps": 5, "weight": 100.0, "rpe": 7.0}
                                    ]
                                }
                            ]
                        }
                    ]
                }
            ]
        }
        """.trimIndent()
}
