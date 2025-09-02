package com.github.radupana.featherweight.data.programme

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test

class ProgrammeSerializersTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    @Test
    fun incrementSerializer_withSingleValue_shouldSerializeAndDeserialize() {
        val increment = IncrementStructure.Single(2.5f)

        val jsonString = json.encodeToString(IncrementSerializer, increment)
        val deserialized = json.decodeFromString(IncrementSerializer, jsonString)

        assertThat(jsonString).isEqualTo("2.5")
        assertThat(deserialized).isInstanceOf(IncrementStructure.Single::class.java)
        assertThat((deserialized as IncrementStructure.Single).value).isEqualTo(2.5f)
    }

    @Test
    fun incrementSerializer_withPerExerciseValues_shouldSerializeAndDeserialize() {
        val increment =
            IncrementStructure.PerExercise(
                mapOf("squat" to 5f, "bench" to 2.5f, "deadlift" to 10f),
            )

        val jsonString = json.encodeToString(IncrementSerializer, increment)
        val deserialized = json.decodeFromString(IncrementSerializer, jsonString)

        assertThat(jsonString).contains("\"squat\":5.0")
        assertThat(jsonString).contains("\"bench\":2.5")
        assertThat(deserialized).isInstanceOf(IncrementStructure.PerExercise::class.java)

        val perExercise = deserialized as IncrementStructure.PerExercise
        assertThat(perExercise.values["squat"]).isEqualTo(5f)
        assertThat(perExercise.values["bench"]).isEqualTo(2.5f)
        assertThat(perExercise.values["deadlift"]).isEqualTo(10f)
    }

    @Test
    fun incrementSerializer_deserializeStringPrimitive_shouldHandleAsFloat() {
        val jsonString = "\"5.0\""

        val deserialized = json.decodeFromString(IncrementSerializer, jsonString)

        assertThat(deserialized).isInstanceOf(IncrementStructure.Single::class.java)
        assertThat((deserialized as IncrementStructure.Single).value).isEqualTo(5f)
    }

    @Test
    fun incrementSerializer_deserializeInvalidValue_shouldUseDefault() {
        val jsonString = "\"invalid\""

        val deserialized = json.decodeFromString(IncrementSerializer, jsonString)

        assertThat(deserialized).isInstanceOf(IncrementStructure.Single::class.java)
        assertThat((deserialized as IncrementStructure.Single).value).isEqualTo(2.5f)
    }

    @Test
    fun incrementSerializer_deserializeObject_shouldParseAsPerExercise() {
        val jsonString = """{"squat": 5, "bench": "2.5"}"""

        val deserialized = json.decodeFromString(IncrementSerializer, jsonString)

        assertThat(deserialized).isInstanceOf(IncrementStructure.PerExercise::class.java)
        val perExercise = deserialized as IncrementStructure.PerExercise
        assertThat(perExercise.values["squat"]).isEqualTo(5f)
        assertThat(perExercise.values["bench"]).isEqualTo(2.5f)
    }

    @Test
    fun repsSerializer_withSingleValue_shouldSerializeAndDeserialize() {
        val reps = RepsStructure.Single(5)

        val jsonString = json.encodeToString(RepsSerializer, reps)
        val deserialized = json.decodeFromString(RepsSerializer, jsonString)

        assertThat(jsonString).isEqualTo("5")
        assertThat(deserialized).isInstanceOf(RepsStructure.Single::class.java)
        assertThat((deserialized as RepsStructure.Single).value).isEqualTo(5)
    }

    @Test
    fun repsSerializer_withRange_shouldSerializeAsString() {
        val reps = RepsStructure.Range(8, 12)

        val jsonString = json.encodeToString(RepsSerializer, reps)
        val deserialized = json.decodeFromString(RepsSerializer, jsonString)

        assertThat(jsonString).isEqualTo("\"8-12\"")
        assertThat(deserialized).isInstanceOf(RepsStructure.RangeString::class.java)
        assertThat((deserialized as RepsStructure.RangeString).value).isEqualTo("8-12")
    }

    @Test
    fun repsSerializer_withRangeString_shouldSerializeAndDeserialize() {
        val reps = RepsStructure.RangeString("10-15")

        val jsonString = json.encodeToString(RepsSerializer, reps)
        val deserialized = json.decodeFromString(RepsSerializer, jsonString)

        assertThat(jsonString).isEqualTo("\"10-15\"")
        assertThat(deserialized).isInstanceOf(RepsStructure.RangeString::class.java)
        assertThat((deserialized as RepsStructure.RangeString).value).isEqualTo("10-15")
    }

    @Test
    fun repsSerializer_withPerSet_shouldSerializeAsArray() {
        val reps = RepsStructure.PerSet(listOf("5", "3", "1+"))

        val jsonString = json.encodeToString(RepsSerializer, reps)
        val deserialized = json.decodeFromString(RepsSerializer, jsonString)

        assertThat(jsonString).isEqualTo("[\"5\",\"3\",\"1+\"]")
        assertThat(deserialized).isInstanceOf(RepsStructure.PerSet::class.java)

        val perSet = deserialized as RepsStructure.PerSet
        assertThat(perSet.values).containsExactly("5", "3", "1+")
    }

    @Test
    fun repsSerializer_deserializeStringWithDash_shouldParseAsRangeString() {
        val jsonString = "\"8-10\""

        val deserialized = json.decodeFromString(RepsSerializer, jsonString)

        assertThat(deserialized).isInstanceOf(RepsStructure.RangeString::class.java)
        assertThat((deserialized as RepsStructure.RangeString).value).isEqualTo("8-10")
    }

    @Test
    fun repsSerializer_deserializeStringNumber_shouldParseAsSingle() {
        val jsonString = "\"5\""

        val deserialized = json.decodeFromString(RepsSerializer, jsonString)

        assertThat(deserialized).isInstanceOf(RepsStructure.Single::class.java)
        assertThat((deserialized as RepsStructure.Single).value).isEqualTo(5)
    }

    @Test
    fun repsSerializer_deserializeAmrapNotation_shouldKeepAsString() {
        val jsonString = "\"5+\""

        val deserialized = json.decodeFromString(RepsSerializer, jsonString)

        assertThat(deserialized).isInstanceOf(RepsStructure.RangeString::class.java)
        assertThat((deserialized as RepsStructure.RangeString).value).isEqualTo("5+")
    }

    @Test
    fun repsSerializer_deserializeArray_shouldParseAsPerSet() {
        val jsonString = "[5, \"3\", \"1+\"]"

        val deserialized = json.decodeFromString(RepsSerializer, jsonString)

        assertThat(deserialized).isInstanceOf(RepsStructure.PerSet::class.java)
        val perSet = deserialized as RepsStructure.PerSet
        assertThat(perSet.values).containsExactly("5", "3", "1+")
    }

    @Test
    fun repsSerializer_deserializeInvalidString_shouldUseDefault() {
        val jsonString = "null"

        val deserialized = json.decodeFromString(RepsSerializer, jsonString)

        assertThat(deserialized).isInstanceOf(RepsStructure.Single::class.java)
        assertThat((deserialized as RepsStructure.Single).value).isEqualTo(5)
    }

    @Test
    fun programmeStructure_serialization_shouldWorkCorrectly() {
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
                                        name = "Upper Power",
                                        exercises =
                                            listOf(
                                                ExerciseStructure(
                                                    name = "Bench Press",
                                                    sets = 5,
                                                    reps = RepsStructure.Single(5),
                                                    progression = "linear",
                                                    intensity = listOf(70, 80, 90),
                                                    customizable = false,
                                                    category = "compound",
                                                    note = null,
                                                    weightSource = "80% 1RM",
                                                    exerciseId = 1L,
                                                    weights = listOf(100f),
                                                    rpeValues = null,
                                                ),
                                            ),
                                        estimatedDuration = 60,
                                    ),
                                ),
                        ),
                    ),
                progression =
                    ProgressionStructure(
                        type = "linear",
                        increment = IncrementStructure.Single(2.5f),
                        cycle = null,
                        deloadThreshold = 3,
                        note = "Increase when all sets completed",
                    ),
            )

        val jsonString = json.encodeToString(ProgrammeStructure.serializer(), structure)
        val deserialized = json.decodeFromString(ProgrammeStructure.serializer(), jsonString)

        assertThat(deserialized.weeks).hasSize(1)
        assertThat(deserialized.weeks[0].weekNumber).isEqualTo(1)
        assertThat(deserialized.weeks[0].workouts).hasSize(1)
        assertThat(deserialized.weeks[0].workouts[0].exercises).hasSize(1)
        assertThat(deserialized.progression.type).isEqualTo("linear")
    }

    @Test
    fun exerciseStructure_withNullableFields_shouldSerializeCorrectly() {
        val exercise =
            ExerciseStructure(
                name = "Squat",
                sets = 3,
                reps = RepsStructure.Single(5),
                progression = "linear",
                intensity = null,
                customizable = false,
                category = null,
                note = null,
                weightSource = null,
                exerciseId = null,
                weights = null,
                rpeValues = null,
            )

        val jsonString = json.encodeToString(ExerciseStructure.serializer(), exercise)
        val deserialized = json.decodeFromString(ExerciseStructure.serializer(), jsonString)

        assertThat(deserialized.name).isEqualTo("Squat")
        assertThat(deserialized.sets).isEqualTo(3)
        assertThat(deserialized.intensity).isNull()
        assertThat(deserialized.category).isNull()
        assertThat(deserialized.note).isNull()
        assertThat(deserialized.weights).isNull()
    }

    @Test
    fun workoutStructure_withOptionalDuration_shouldSerializeCorrectly() {
        val workout1 =
            WorkoutStructure(
                day = 1,
                name = "Quick Workout",
                exercises = emptyList(),
                estimatedDuration = null,
            )

        val workout2 =
            WorkoutStructure(
                day = 2,
                name = "Long Workout",
                exercises = emptyList(),
                estimatedDuration = 90,
            )

        val json1 = json.encodeToString(WorkoutStructure.serializer(), workout1)
        val json2 = json.encodeToString(WorkoutStructure.serializer(), workout2)

        val deserialized1 = json.decodeFromString(WorkoutStructure.serializer(), json1)
        val deserialized2 = json.decodeFromString(WorkoutStructure.serializer(), json2)

        assertThat(deserialized1.estimatedDuration).isNull()
        assertThat(deserialized2.estimatedDuration).isEqualTo(90)
    }

    @Test
    fun incrementStructure_roundTrip_shouldPreserveValues() {
        val single = IncrementStructure.Single(5f)
        val perExercise =
            IncrementStructure.PerExercise(
                mapOf("squat" to 5f, "bench" to 2.5f),
            )

        val singleJson = json.encodeToString(IncrementSerializer, single)
        val perExerciseJson = json.encodeToString(IncrementSerializer, perExercise)

        val singleDeserialized = json.decodeFromString(IncrementSerializer, singleJson)
        val perExerciseDeserialized = json.decodeFromString(IncrementSerializer, perExerciseJson)

        assertThat((singleDeserialized as IncrementStructure.Single).value).isEqualTo(5f)
        assertThat((perExerciseDeserialized as IncrementStructure.PerExercise).values).hasSize(2)
    }

    @Test
    fun repsStructure_allTypes_shouldSerializeCorrectly() {
        val single = RepsStructure.Single(5)
        val range = RepsStructure.Range(8, 12)
        val rangeString = RepsStructure.RangeString("10-15")
        val perSet = RepsStructure.PerSet(listOf("5", "3", "1+"))

        val singleJson = json.encodeToString(RepsSerializer, single)
        val rangeJson = json.encodeToString(RepsSerializer, range)
        val rangeStringJson = json.encodeToString(RepsSerializer, rangeString)
        val perSetJson = json.encodeToString(RepsSerializer, perSet)

        assertThat(singleJson).isEqualTo("5")
        assertThat(rangeJson).isEqualTo("\"8-12\"")
        assertThat(rangeStringJson).isEqualTo("\"10-15\"")
        assertThat(perSetJson).isEqualTo("[\"5\",\"3\",\"1+\"]")
    }

    @Test
    fun progressionStructure_withNullableFields_shouldSerializeCorrectly() {
        val progression1 =
            ProgressionStructure(
                type = "linear",
                increment = IncrementStructure.Single(2.5f),
                cycle = null,
                deloadThreshold = null,
                note = null,
            )

        val progression2 =
            ProgressionStructure(
                type = "wave",
                increment = IncrementStructure.PerExercise(mapOf("all" to 5f)),
                cycle = 3,
                deloadThreshold = 2,
                note = "Deload on week 4",
            )

        val json1 = json.encodeToString(ProgressionStructure.serializer(), progression1)
        val json2 = json.encodeToString(ProgressionStructure.serializer(), progression2)

        val deserialized1 = json.decodeFromString(ProgressionStructure.serializer(), json1)
        val deserialized2 = json.decodeFromString(ProgressionStructure.serializer(), json2)

        assertThat(deserialized1.cycle).isNull()
        assertThat(deserialized1.note).isNull()
        assertThat(deserialized2.cycle).isEqualTo(3)
        assertThat(deserialized2.note).isEqualTo("Deload on week 4")
    }
}
