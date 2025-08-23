package com.github.radupana.featherweight.data.programme

import android.util.Log
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Data classes for parsing programme workout JSON structures
 */

@Serializable
data class ProgrammeStructure(
    val weeks: List<ProgrammeWeekStructure>,
    val progression: ProgressionStructure,
)

@Serializable
data class ProgrammeWeekStructure(
    val weekNumber: Int,
    val name: String,
    val workouts: List<WorkoutStructure>,
)

@Serializable
data class WorkoutStructure(
    val day: Int,
    val name: String,
    val exercises: List<ExerciseStructure>,
    val estimatedDuration: Int? = null,
)

@Serializable
data class ExerciseStructure(
    val name: String,
    val sets: Int,
    val reps: RepsStructure,
    val progression: String = "linear",
    val intensity: List<Int>? = null, // Percentages for 1RM-based programs
    val customizable: Boolean = false,
    val category: String? = null, // For accessory categorization
    val note: String? = null,
    val suggestedWeight: Float? = null, // AI-suggested weight in kg
    val weightSource: String? = null, // Source of weight suggestion
)

@Serializable
data class ProgressionStructure(
    val type: String, // "linear", "531", "nsuns", etc.
    val increment: IncrementStructure,
    val cycle: Int? = null,
    val deloadThreshold: Int? = null,
    val note: String? = null,
)

// Flexible increment structure to handle both single values and per-exercise values
@Serializable(with = IncrementSerializer::class)
sealed class IncrementStructure {
    data class Single(
        val value: Float,
    ) : IncrementStructure()

    data class PerExercise(
        val values: Map<String, Float>,
    ) : IncrementStructure()
}

// Flexible reps structure to handle both single values, ranges, and AMRAP
@Serializable(with = RepsSerializer::class)
sealed class RepsStructure {
    data class Single(
        val value: Int,
    ) : RepsStructure()

    data class Range(
        val min: Int,
        val max: Int,
    ) : RepsStructure()

    data class RangeString(
        val value: String,
    ) : RepsStructure() // "8-12"

    data class PerSet(
        val values: List<String>,
    ) : RepsStructure() // [5, 3, "1+"]
}

/**
 * Helper functions for parsing programme structures
 */
object ProgrammeWorkoutParser {
    private const val TAG = "ProgrammeWorkoutParser"

    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    fun parseStructure(jsonString: String): ProgrammeStructure? =
        try {
            json.decodeFromString<ProgrammeStructure>(jsonString)
        } catch (e: kotlinx.serialization.SerializationException) {
            Log.w(TAG, "Failed to parse programme structure JSON", e)
            null
        }

    fun getWorkoutForWeekAndDay(
        structure: ProgrammeStructure,
        weekNumber: Int,
        dayNumber: Int,
    ): WorkoutStructure? {
        val week = structure.weeks.find { it.weekNumber == weekNumber } ?: return null
        return week.workouts.find { it.day == dayNumber }
    }

    fun getAllWorkoutsForWeek(
        structure: ProgrammeStructure,
        weekNumber: Int,
    ): List<WorkoutStructure> {
        val week = structure.weeks.find { it.weekNumber == weekNumber } ?: return emptyList()
        return week.workouts.sortedBy { it.day }
    }

    fun calculateWeight(
        exerciseName: String,
        intensity: Int?,
        userMaxes: Map<String, Float>,
        baseWeight: Float = 45f,
    ): Float {
        if (intensity == null) return baseWeight

        // Map exercise names to user max keys
        val maxKey =
            when {
                exerciseName.contains("Squat", ignoreCase = true) -> "squat"
                exerciseName.contains("Bench", ignoreCase = true) -> "bench"
                exerciseName.contains("Deadlift", ignoreCase = true) -> "deadlift"
                exerciseName.contains("Press", ignoreCase = true) && !exerciseName.contains("Bench", ignoreCase = true) -> "ohp"
                else -> null
            }

        val userMax = maxKey?.let { userMaxes[it] } ?: return baseWeight
        return (userMax * intensity / 100f).let { weight ->
            // Round to nearest 2.5kg
            (weight / 2.5f).toInt() * 2.5f
        }
    }

    fun parseReps(repsStructure: RepsStructure): String =
        when (repsStructure) {
            is RepsStructure.Single -> repsStructure.value.toString()
            is RepsStructure.Range -> "${repsStructure.min}-${repsStructure.max}"
            is RepsStructure.RangeString -> repsStructure.value
            is RepsStructure.PerSet -> repsStructure.values.joinToString(", ")
        }

    fun parseRepsForSet(
        repsStructure: RepsStructure,
        setIndex: Int,
    ): Int =
        when (repsStructure) {
            is RepsStructure.Single -> repsStructure.value
            is RepsStructure.Range -> (repsStructure.min + repsStructure.max) / 2 // Use average
            is RepsStructure.RangeString -> {
                // Parse "8-12" format
                val parts = repsStructure.value.split("-")
                if (parts.size == 2) {
                    val min = parts[0].toIntOrNull() ?: 8
                    val max = parts[1].toIntOrNull() ?: 12
                    (min + max) / 2
                } else {
                    repsStructure.value.toIntOrNull() ?: 8
                }
            }

            is RepsStructure.PerSet -> {
                // Handle per-set reps like [5, 3, "1+"]
                val setReps = repsStructure.values.getOrNull(setIndex) ?: repsStructure.values.lastOrNull() ?: "5"
                when {
                    setReps.endsWith("+") -> setReps.dropLast(1).toIntOrNull() ?: 1
                    else -> setReps.toIntOrNull() ?: 5
                }
            }
        }
}

// Custom serializers for flexible data structures
object IncrementSerializer : KSerializer<IncrementStructure> {
    override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

    override fun serialize(
        encoder: Encoder,
        value: IncrementStructure,
    ) {
        val jsonElement =
            when (value) {
                is IncrementStructure.Single -> JsonPrimitive(value.value)
                is IncrementStructure.PerExercise ->
                    JsonObject(
                        value.values.mapValues { JsonPrimitive(it.value) },
                    )
            }
        encoder.encodeSerializableValue(JsonElement.serializer(), jsonElement)
    }

    override fun deserialize(decoder: Decoder): IncrementStructure {
        val element = decoder.decodeSerializableValue(JsonElement.serializer())
        return when {
            element is JsonPrimitive && element.isString -> {
                IncrementStructure.Single(element.content.toFloatOrNull() ?: 2.5f)
            }

            element is JsonPrimitive -> {
                IncrementStructure.Single(element.content.toFloatOrNull() ?: 2.5f)
            }

            element is JsonObject -> {
                val values =
                    element.mapValues { (_, v) ->
                        (v as? JsonPrimitive)?.content?.toFloatOrNull() ?: 2.5f
                    }
                IncrementStructure.PerExercise(values)
            }

            else -> IncrementStructure.Single(2.5f)
        }
    }
}

object RepsSerializer : KSerializer<RepsStructure> {
    override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

    override fun serialize(
        encoder: Encoder,
        value: RepsStructure,
    ) {
        val jsonElement =
            when (value) {
                is RepsStructure.Single -> JsonPrimitive(value.value)
                is RepsStructure.Range -> JsonPrimitive("${value.min}-${value.max}")
                is RepsStructure.RangeString -> JsonPrimitive(value.value)
                is RepsStructure.PerSet -> JsonArray(value.values.map { JsonPrimitive(it) })
            }
        encoder.encodeSerializableValue(JsonElement.serializer(), jsonElement)
    }

    override fun deserialize(decoder: Decoder): RepsStructure {
        val element = decoder.decodeSerializableValue(JsonElement.serializer())
        return when {
            element is JsonPrimitive && element.isString -> {
                val content = element.content
                when {
                    content.contains("-") -> RepsStructure.RangeString(content)
                    content.toIntOrNull() != null -> RepsStructure.Single(content.toInt())
                    else -> RepsStructure.RangeString(content)
                }
            }

            element is JsonPrimitive -> {
                RepsStructure.Single(element.content.toIntOrNull() ?: 5)
            }

            element is JsonArray -> {
                val values =
                    element.map {
                        when (it) {
                            is JsonPrimitive -> it.content
                            else -> "5"
                        }
                    }
                RepsStructure.PerSet(values)
            }

            else -> RepsStructure.Single(5)
        }
    }
}

/**
 * Wrapper to hold workout structure with actual programme week number
 */
data class WorkoutStructureWithWeek(
    val workoutStructure: WorkoutStructure,
    val actualWeekNumber: Int,
)
