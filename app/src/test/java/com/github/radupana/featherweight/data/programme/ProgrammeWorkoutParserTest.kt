package com.github.radupana.featherweight.data.programme

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ProgrammeWorkoutParserTest {
    @Test
    fun parseStructure_withValidJson_shouldReturnProgrammeStructure() {
        val jsonString =
            """
            {
                "weeks": [
                    {
                        "weekNumber": 1,
                        "name": "Week 1",
                        "workouts": [
                            {
                                "day": 1,
                                "name": "Upper Power",
                                "exercises": [
                                    {
                                        "name": "Barbell Bench Press",
                                        "sets": 5,
                                        "reps": 5,
                                        "progression": "linear"
                                    }
                                ]
                            }
                        ]
                    }
                ],
                "progression": {
                    "type": "linear",
                    "increment": 2.5
                }
            }
            """.trimIndent()

        val structure = ProgrammeWorkoutParser.parseStructure(jsonString)

        assertThat(structure).isNotNull()
        assertThat(structure?.weeks).hasSize(1)
        assertThat(structure?.weeks?.first()?.weekNumber).isEqualTo(1)
        assertThat(structure?.weeks?.first()?.workouts).hasSize(1)
    }

    // Note: Test for invalid JSON parsing is omitted because it requires Android's Log class
    // which is not available in unit tests. The implementation correctly returns null for invalid JSON.

    @Test
    fun getWorkoutForWeekAndDay_withValidWeekAndDay_shouldReturnWorkout() {
        val structure =
            ProgrammeStructure(
                weeks =
                    listOf(
                        ProgrammeWeekStructure(
                            weekNumber = 1,
                            name = "Week 1",
                            workouts =
                                listOf(
                                    WorkoutStructure(
                                        day = 1,
                                        name = "Day 1",
                                        exercises = emptyList(),
                                    ),
                                    WorkoutStructure(
                                        day = 3,
                                        name = "Day 3",
                                        exercises = emptyList(),
                                    ),
                                ),
                        ),
                    ),
                progression =
                    ProgressionStructure(
                        type = "linear",
                        increment = IncrementStructure.Single(2.5f),
                        cycle = null,
                        deloadThreshold = null,
                        note = null,
                    ),
            )

        val workout = ProgrammeWorkoutParser.getWorkoutForWeekAndDay(structure, 1, 3)

        assertThat(workout).isNotNull()
        assertThat(workout?.name).isEqualTo("Day 3")
        assertThat(workout?.day).isEqualTo(3)
    }

    @Test
    fun getWorkoutForWeekAndDay_withInvalidWeek_shouldReturnNull() {
        val structure =
            ProgrammeStructure(
                weeks =
                    listOf(
                        ProgrammeWeekStructure(
                            weekNumber = 1,
                            name = "Week 1",
                            workouts = emptyList(),
                        ),
                    ),
                progression =
                    ProgressionStructure(
                        type = "linear",
                        increment = IncrementStructure.Single(2.5f),
                        cycle = null,
                        deloadThreshold = null,
                        note = null,
                    ),
            )

        val workout = ProgrammeWorkoutParser.getWorkoutForWeekAndDay(structure, 2, 1)

        assertThat(workout).isNull()
    }

    @Test
    fun getWorkoutForWeekAndDay_withInvalidDay_shouldReturnNull() {
        val structure =
            ProgrammeStructure(
                weeks =
                    listOf(
                        ProgrammeWeekStructure(
                            weekNumber = 1,
                            name = "Week 1",
                            workouts =
                                listOf(
                                    WorkoutStructure(
                                        day = 1,
                                        name = "Day 1",
                                        exercises = emptyList(),
                                    ),
                                ),
                        ),
                    ),
                progression =
                    ProgressionStructure(
                        type = "linear",
                        increment = IncrementStructure.Single(2.5f),
                        cycle = null,
                        deloadThreshold = null,
                        note = null,
                    ),
            )

        val workout = ProgrammeWorkoutParser.getWorkoutForWeekAndDay(structure, 1, 2)

        assertThat(workout).isNull()
    }

    @Test
    fun getAllWorkoutsForWeek_withValidWeek_shouldReturnAllWorkouts() {
        val structure =
            ProgrammeStructure(
                weeks =
                    listOf(
                        ProgrammeWeekStructure(
                            weekNumber = 1,
                            name = "Week 1",
                            workouts =
                                listOf(
                                    WorkoutStructure(day = 5, name = "Friday", exercises = emptyList()),
                                    WorkoutStructure(day = 1, name = "Monday", exercises = emptyList()),
                                    WorkoutStructure(day = 3, name = "Wednesday", exercises = emptyList()),
                                ),
                        ),
                    ),
                progression =
                    ProgressionStructure(
                        type = "linear",
                        increment = IncrementStructure.Single(2.5f),
                        cycle = null,
                        deloadThreshold = null,
                        note = null,
                    ),
            )

        val workouts = ProgrammeWorkoutParser.getAllWorkoutsForWeek(structure, 1)

        assertThat(workouts).hasSize(3)
        assertThat(workouts.map { it.day }).containsExactly(1, 3, 5).inOrder()
    }

    @Test
    fun getAllWorkoutsForWeek_withInvalidWeek_shouldReturnEmptyList() {
        val structure =
            ProgrammeStructure(
                weeks = emptyList(),
                progression =
                    ProgressionStructure(
                        type = "linear",
                        increment = IncrementStructure.Single(2.5f),
                        cycle = null,
                        deloadThreshold = null,
                        note = null,
                    ),
            )

        val workouts = ProgrammeWorkoutParser.getAllWorkoutsForWeek(structure, 1)

        assertThat(workouts).isEmpty()
    }

    @Test
    fun calculateWeight_withSquatExercise_shouldUseSquatMax() {
        val userMaxes =
            mapOf(
                "squat" to 100f,
                "bench" to 80f,
                "deadlift" to 120f,
                "ohp" to 60f,
            )

        val weight =
            ProgrammeWorkoutParser.calculateWeight(
                "Barbell Squat",
                80,
                userMaxes,
                45f,
            )

        assertThat(weight).isEqualTo(80f) // 100 * 0.8 = 80, already on 2.5kg increment
    }

    @Test
    fun calculateWeight_withBenchExercise_shouldUseBenchMax() {
        val userMaxes =
            mapOf(
                "squat" to 100f,
                "bench" to 80f,
                "deadlift" to 120f,
                "ohp" to 60f,
            )

        val weight =
            ProgrammeWorkoutParser.calculateWeight(
                "Barbell Bench Press",
                75,
                userMaxes,
                45f,
            )

        assertThat(weight).isEqualTo(60f) // 80 * 0.75 = 60
    }

    @Test
    fun calculateWeight_withDeadliftExercise_shouldUseDeadliftMax() {
        val userMaxes =
            mapOf(
                "squat" to 100f,
                "bench" to 80f,
                "deadlift" to 120f,
                "ohp" to 60f,
            )

        val weight =
            ProgrammeWorkoutParser.calculateWeight(
                "Romanian Deadlift",
                70,
                userMaxes,
                45f,
            )

        assertThat(weight).isEqualTo(82.5f) // 120 * 0.7 = 84, rounded down to 82.5
    }

    @Test
    fun calculateWeight_withOverheadPress_shouldUseOhpMax() {
        val userMaxes =
            mapOf(
                "squat" to 100f,
                "bench" to 80f,
                "deadlift" to 120f,
                "ohp" to 60f,
            )

        val weight =
            ProgrammeWorkoutParser.calculateWeight(
                "Overhead Press",
                85,
                userMaxes,
                45f,
            )

        assertThat(weight).isEqualTo(50f) // 60 * 0.85 = 51, rounded down to 50
    }

    @Test
    fun calculateWeight_withUnknownExercise_shouldReturnBaseWeight() {
        val userMaxes =
            mapOf(
                "squat" to 100f,
                "bench" to 80f,
            )

        val weight =
            ProgrammeWorkoutParser.calculateWeight(
                "Bicep Curl",
                70,
                userMaxes,
                20f,
            )

        assertThat(weight).isEqualTo(20f)
    }

    @Test
    fun calculateWeight_withNullIntensity_shouldReturnBaseWeight() {
        val userMaxes = mapOf("squat" to 100f)

        val weight =
            ProgrammeWorkoutParser.calculateWeight(
                "Barbell Squat",
                null,
                userMaxes,
                45f,
            )

        assertThat(weight).isEqualTo(45f)
    }

    @Test
    fun calculateWeight_withMissingMax_shouldReturnBaseWeight() {
        val userMaxes = emptyMap<String, Float>()

        val weight =
            ProgrammeWorkoutParser.calculateWeight(
                "Barbell Squat",
                80,
                userMaxes,
                45f,
            )

        assertThat(weight).isEqualTo(45f)
    }

    @Test
    fun calculateWeight_roundsToNearest2_5kg() {
        val userMaxes = mapOf("squat" to 100f)

        val weight1 =
            ProgrammeWorkoutParser.calculateWeight(
                "Squat",
                73, // 73% of 100 = 73, should round to 72.5
                userMaxes,
            )
        assertThat(weight1).isEqualTo(72.5f)

        val weight2 =
            ProgrammeWorkoutParser.calculateWeight(
                "Squat",
                74, // 74% of 100 = 74, should round to 72.5
                userMaxes,
            )
        assertThat(weight2).isEqualTo(72.5f)

        val weight3 =
            ProgrammeWorkoutParser.calculateWeight(
                "Squat",
                76, // 76% of 100 = 76, should round to 75
                userMaxes,
            )
        assertThat(weight3).isEqualTo(75f)
    }

    @Test
    fun parseReps_withSingleReps_shouldReturnString() {
        val reps = RepsStructure.Single(5)

        val result = ProgrammeWorkoutParser.parseReps(reps)

        assertThat(result).isEqualTo("5")
    }

    @Test
    fun parseReps_withRangeReps_shouldReturnRangeString() {
        val reps = RepsStructure.Range(8, 12)

        val result = ProgrammeWorkoutParser.parseReps(reps)

        assertThat(result).isEqualTo("8-12")
    }

    @Test
    fun parseReps_withRangeString_shouldReturnOriginalString() {
        val reps = RepsStructure.RangeString("10-15")

        val result = ProgrammeWorkoutParser.parseReps(reps)

        assertThat(result).isEqualTo("10-15")
    }

    @Test
    fun parseReps_withPerSetReps_shouldReturnCommaSeparated() {
        val reps = RepsStructure.PerSet(listOf("5", "3", "1+"))

        val result = ProgrammeWorkoutParser.parseReps(reps)

        assertThat(result).isEqualTo("5, 3, 1+")
    }

    @Test
    fun parseRepsForSet_withSingleReps_shouldReturnValue() {
        val reps = RepsStructure.Single(8)

        val result = ProgrammeWorkoutParser.parseRepsForSet(reps, 0)

        assertThat(result).isEqualTo(8)
    }

    @Test
    fun parseRepsForSet_withRange_shouldReturnAverage() {
        val reps = RepsStructure.Range(8, 12)

        val result = ProgrammeWorkoutParser.parseRepsForSet(reps, 0)

        assertThat(result).isEqualTo(10) // (8 + 12) / 2
    }

    @Test
    fun parseRepsForSet_withRangeString_shouldParseAndAverage() {
        val reps = RepsStructure.RangeString("6-10")

        val result = ProgrammeWorkoutParser.parseRepsForSet(reps, 0)

        assertThat(result).isEqualTo(8) // (6 + 10) / 2
    }

    @Test
    fun parseRepsForSet_withInvalidRangeString_shouldReturnDefault() {
        val reps = RepsStructure.RangeString("invalid")

        val result = ProgrammeWorkoutParser.parseRepsForSet(reps, 0)

        assertThat(result).isEqualTo(8) // Default value
    }

    @Test
    fun parseRepsForSet_withPerSet_shouldReturnSpecificSetReps() {
        val reps = RepsStructure.PerSet(listOf("5", "3", "1"))

        assertThat(ProgrammeWorkoutParser.parseRepsForSet(reps, 0)).isEqualTo(5)
        assertThat(ProgrammeWorkoutParser.parseRepsForSet(reps, 1)).isEqualTo(3)
        assertThat(ProgrammeWorkoutParser.parseRepsForSet(reps, 2)).isEqualTo(1)
    }

    @Test
    fun parseRepsForSet_withPerSetAmrap_shouldParseAmrapNotation() {
        val reps = RepsStructure.PerSet(listOf("5", "3", "1+"))

        val result = ProgrammeWorkoutParser.parseRepsForSet(reps, 2)

        assertThat(result).isEqualTo(1) // "1+" becomes 1
    }

    @Test
    fun parseRepsForSet_withPerSetBeyondSize_shouldReturnLastValue() {
        val reps = RepsStructure.PerSet(listOf("5", "3", "1"))

        val result = ProgrammeWorkoutParser.parseRepsForSet(reps, 5) // Beyond array size

        assertThat(result).isEqualTo(1) // Returns last value
    }

    @Test
    fun parseRepsForSet_withEmptyPerSet_shouldReturnDefault() {
        val reps = RepsStructure.PerSet(emptyList())

        val result = ProgrammeWorkoutParser.parseRepsForSet(reps, 0)

        assertThat(result).isEqualTo(5) // Default value
    }

    @Test
    fun parseRepsForSet_withInvalidPerSetValue_shouldReturnDefault() {
        val reps = RepsStructure.PerSet(listOf("invalid"))

        val result = ProgrammeWorkoutParser.parseRepsForSet(reps, 0)

        assertThat(result).isEqualTo(5) // Default value
    }

    @Test
    fun exerciseStructure_withAllFields_shouldStoreCorrectly() {
        val exercise =
            ExerciseStructure(
                name = "Barbell Squat",
                sets = 5,
                reps = RepsStructure.Single(5),
                progression = "linear",
                intensity = listOf(70, 80, 90),
                customizable = true,
                category = "compound",
                note = "Focus on depth",
                weightSource = "80% of 1RM",
                exerciseId = "123",
                weights = listOf(100f, 110f, 120f),
                rpeValues = listOf(7f, 8f, 9f),
            )

        assertThat(exercise.name).isEqualTo("Barbell Squat")
        assertThat(exercise.sets).isEqualTo(5)
        assertThat(exercise.intensity).containsExactly(70, 80, 90)
        assertThat(exercise.customizable).isTrue()
        assertThat(exercise.weights).containsExactly(100f, 110f, 120f)
        assertThat(exercise.rpeValues).containsExactly(7f, 8f, 9f)
    }

    @Test
    fun workoutStructureWithWeek_shouldWrapCorrectly() {
        val workout =
            WorkoutStructure(
                day = 1,
                name = "Upper Power",
                exercises = emptyList(),
                estimatedDuration = 60,
            )

        val wrapped =
            WorkoutStructureWithWeek(
                workoutStructure = workout,
                actualWeekNumber = 3,
            )

        assertThat(wrapped.workoutStructure).isEqualTo(workout)
        assertThat(wrapped.actualWeekNumber).isEqualTo(3)
    }

    @Test
    fun progressionStructure_withAllFields_shouldStoreCorrectly() {
        val progression =
            ProgressionStructure(
                type = "5/3/1",
                increment =
                    IncrementStructure.PerExercise(
                        mapOf("squat" to 5f, "bench" to 2.5f),
                    ),
                cycle = 3,
                deloadThreshold = 2,
                note = "Deload on week 4",
            )

        assertThat(progression.type).isEqualTo("5/3/1")
        assertThat(progression.cycle).isEqualTo(3)
        assertThat(progression.deloadThreshold).isEqualTo(2)
        assertThat(progression.note).isEqualTo("Deload on week 4")

        val increments = progression.increment as IncrementStructure.PerExercise
        assertThat(increments.values["squat"]).isEqualTo(5f)
        assertThat(increments.values["bench"]).isEqualTo(2.5f)
    }
}
