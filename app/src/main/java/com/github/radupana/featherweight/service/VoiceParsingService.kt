package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.voice.ParsedExerciseData
import com.github.radupana.featherweight.data.voice.ParsedSetData
import com.github.radupana.featherweight.data.voice.ParsedVoiceWorkoutInput
import com.github.radupana.featherweight.model.WeightUnit
import com.github.radupana.featherweight.util.CloudLogger
import com.github.radupana.featherweight.util.ExceptionLogger
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

interface VoiceParser {
    suspend fun parseTranscription(
        transcription: String,
        preferredWeightUnit: WeightUnit,
    ): Result<ParsedVoiceWorkoutInput>
}

class VoiceParsingService(
    private val functionCaller: CloudFunctionCaller = FirebaseCloudFunctionCaller(),
) : VoiceParser {
    companion object {
        private const val TAG = "VoiceParsingService"
        private const val FUNCTION_NAME = "parseVoiceWorkout"
    }

    override suspend fun parseTranscription(
        transcription: String,
        preferredWeightUnit: WeightUnit,
    ): Result<ParsedVoiceWorkoutInput> =
        withContext(Dispatchers.IO) {
            CloudLogger.info(TAG, "Starting transcription parsing")

            if (transcription.isBlank()) {
                CloudLogger.error(TAG, "Empty transcription provided")
                return@withContext Result.failure(
                    IllegalArgumentException("Transcription cannot be empty"),
                )
            }

            try {
                val data =
                    hashMapOf<String, Any>(
                        "transcription" to transcription,
                        "preferredWeightUnit" to if (preferredWeightUnit == WeightUnit.KG) "kg" else "lbs",
                    )

                CloudLogger.debug(TAG, "Calling $FUNCTION_NAME")

                val resultData = functionCaller.call(FUNCTION_NAME, data)

                if (resultData == null) {
                    CloudLogger.error(TAG, "Null response from Cloud Function")
                    return@withContext Result.failure(
                        IOException("No response from server. Please try again."),
                    )
                }

                @Suppress("UNCHECKED_CAST")
                val responseMap = resultData as? Map<String, Any>
                val resultMap = responseMap?.get("result") as? Map<String, Any>

                if (resultMap == null) {
                    CloudLogger.error(TAG, "Invalid response format from Cloud Function")
                    return@withContext Result.failure(
                        IOException("Failed to parse response. Please try again."),
                    )
                }

                val parsed = parseResponse(resultMap, transcription)
                CloudLogger.info(TAG, "Parsing completed: ${parsed.exercises.size} exercises found")
                Result.success(parsed)
            } catch (e: FirebaseFunctionsException) {
                handleCloudFunctionError(e)
            } catch (e: IOException) {
                ExceptionLogger.logException(TAG, "Parsing failed - Network error", e)
                Result.failure(e)
            } catch (e: IllegalStateException) {
                ExceptionLogger.logException(TAG, "Parsing failed - Invalid state", e)
                Result.failure(IOException("Parsing failed. Please try again."))
            }
        }

    @Suppress("UNCHECKED_CAST")
    private fun parseResponse(
        resultMap: Map<String, Any>,
        originalTranscription: String,
    ): ParsedVoiceWorkoutInput {
        val exercisesList = resultMap["exercises"] as? List<Map<String, Any>> ?: emptyList()
        val exercises = mutableListOf<ParsedExerciseData>()

        for (exerciseMap in exercisesList) {
            val setsList = exerciseMap["sets"] as? List<Map<String, Any>> ?: emptyList()
            val sets = mutableListOf<ParsedSetData>()

            for (setMap in setsList) {
                sets.add(
                    ParsedSetData(
                        setNumber = (setMap["setNumber"] as? Number)?.toInt() ?: 1,
                        reps = (setMap["reps"] as? Number)?.toInt() ?: 0,
                        weight = (setMap["weight"] as? Number)?.toFloat() ?: 0f,
                        weightUnit = parseWeightUnit(setMap["unit"] as? String ?: "kg"),
                        rpe = (setMap["rpe"] as? Number)?.toFloat(),
                        isToFailure = setMap["isToFailure"] as? Boolean ?: false,
                        notes = setMap["notes"] as? String,
                    ),
                )
            }

            exercises.add(
                ParsedExerciseData(
                    spokenName = exerciseMap["spokenName"] as? String ?: "",
                    interpretedName = exerciseMap["interpretedName"] as? String ?: "",
                    matchedExerciseId = null,
                    matchedExerciseName = null,
                    sets = sets,
                    confidence = (exerciseMap["confidence"] as? Number)?.toFloat() ?: 0f,
                    notes = exerciseMap["notes"] as? String,
                ),
            )
        }

        val warningsList = resultMap["warnings"] as? List<String> ?: emptyList()

        return ParsedVoiceWorkoutInput(
            transcription = originalTranscription,
            exercises = exercises,
            confidence = (resultMap["overallConfidence"] as? Number)?.toFloat() ?: 0f,
            warnings = warningsList,
        )
    }

    private fun parseWeightUnit(unit: String): WeightUnit =
        when (unit.lowercase()) {
            "lbs", "lb", "pounds" -> WeightUnit.LBS
            else -> WeightUnit.KG
        }

    private fun handleCloudFunctionError(e: FirebaseFunctionsException): Result<ParsedVoiceWorkoutInput> {
        CloudLogger.error(TAG, "Cloud Function error: ${e.code} - ${e.message}")

        return when (e.code) {
            FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED -> {
                Result.failure(
                    IOException("Voice input quota exceeded. Please try again later."),
                )
            }

            FirebaseFunctionsException.Code.UNAUTHENTICATED -> {
                Result.failure(
                    IOException("Sign in required to use voice input."),
                )
            }

            FirebaseFunctionsException.Code.INVALID_ARGUMENT -> {
                Result.failure(
                    IOException(e.message ?: "Invalid input. Please try again."),
                )
            }

            else -> {
                Result.failure(
                    IOException("Parsing failed. Please try again."),
                )
            }
        }
    }
}
