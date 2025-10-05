package com.github.radupana.featherweight.service

import android.util.Log
import com.github.radupana.featherweight.data.profile.ExerciseMaxTrackingDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service to fix corrupted OneRM data where mostWeightLifted was incorrectly
 * set to the oneRMEstimate instead of the actual weight lifted.
 *
 * This corruption occurred when syncing from FirestoreOneRMHistory which only
 * had the oneRMEstimate and not the actual weight data.
 */
class OneRMDataFixService(
    private val exerciseMaxTrackingDao: ExerciseMaxTrackingDao,
) {
    companion object {
        private const val TAG = "OneRMDataFixService"

        // Regex patterns to parse weight from context strings
        // Matches patterns like: "110kg × 4 @ RPE 9" or "225lb x 5"
        private val WEIGHT_PATTERN = Regex("""(\d+(?:\.\d+)?)\s*(?:kg|lb|lbs)?\s*[×x]\s*(\d+)""")

        // Alternative pattern for "4×110kg" format
        private val REVERSED_PATTERN = Regex("""(\d+)\s*[×x]\s*(\d+(?:\.\d+)?)\s*(?:kg|lb|lbs)?""")
    }

    /**
     * Fixes corrupted ExerciseMaxTracking records where mostWeightLifted
     * was incorrectly set to oneRMEstimate.
     *
     * @return The number of records fixed
     */
    suspend fun fixCorruptedMaxTracking(): Int =
        withContext(Dispatchers.IO) {
            var fixedCount = 0

            try {
                // Get all max tracking records for all users
                val allMaxes = exerciseMaxTrackingDao.getAll()

                allMaxes.forEach { max ->
                    // Check if this record is likely corrupted:
                    // 1. mostWeightLifted equals oneRMEstimate (suspicious)
                    // 2. Context contains weight information (can be parsed)
                    if (max.mostWeightLifted == max.oneRMEstimate && max.context.isNotBlank()) {
                        val actualWeight = parseWeightFromContext(max.context)

                        if (actualWeight != null && actualWeight < max.oneRMEstimate) {
                            // This confirms corruption - actual weight should be less than 1RM estimate
                            // (unless it was an actual 1RM lift)
                            Log.i(TAG, "Fixing corrupted record for exercise ${max.exerciseId}:")
                            Log.i(TAG, "  Context: ${max.context}")
                            Log.i(TAG, "  Corrupted mostWeightLifted: ${max.mostWeightLifted}kg")
                            Log.i(TAG, "  Correct mostWeightLifted: ${actualWeight}kg")

                            // Also try to parse reps from context
                            val actualReps = parseRepsFromContext(max.context)

                            // Update the record with correct values
                            val fixedRecord =
                                max.copy(
                                    mostWeightLifted = actualWeight,
                                    mostWeightReps = actualReps ?: max.mostWeightReps,
                                )

                            exerciseMaxTrackingDao.update(fixedRecord)
                            fixedCount++
                        }
                    }
                }

                Log.i(TAG, "Data fix complete. Fixed $fixedCount corrupted records.")
            } catch (e: Exception) {
                Log.e(TAG, "Error fixing corrupted data", e)
            }

            fixedCount
        }

    /**
     * Parses the actual weight from a context string.
     *
     * Examples:
     * - "110kg × 4 @ RPE 9" -> 110.0
     * - "225lb x 5" -> 225.0
     * - "80kg × 10 @ RPE 9" -> 80.0
     *
     * @param context The context string from the ExerciseMaxTracking record
     * @return The actual weight lifted, or null if unable to parse
     */
    private fun parseWeightFromContext(context: String): Float? {
        // Try standard pattern first: "110kg × 4"
        WEIGHT_PATTERN.find(context)?.let { match ->
            val weight = match.groupValues[1].toFloatOrNull()
            if (weight != null && weight > 0) {
                return weight
            }
        }

        // Try reversed pattern: "4×110kg"
        REVERSED_PATTERN.find(context)?.let { match ->
            val weight = match.groupValues[2].toFloatOrNull()
            if (weight != null && weight > 0) {
                return weight
            }
        }

        // If no pattern matches, log for debugging
        Log.w(TAG, "Unable to parse weight from context: $context")
        return null
    }

    /**
     * Parses the number of reps from a context string.
     *
     * Examples:
     * - "110kg × 4 @ RPE 9" -> 4
     * - "225lb x 5" -> 5
     * - "80kg × 10 @ RPE 9" -> 10
     *
     * @param context The context string from the ExerciseMaxTracking record
     * @return The number of reps, or null if unable to parse
     */
    private fun parseRepsFromContext(context: String): Int? {
        // Try standard pattern first: "110kg × 4"
        WEIGHT_PATTERN.find(context)?.let { match ->
            val reps = match.groupValues[2].toIntOrNull()
            if (reps != null && reps > 0) {
                return reps
            }
        }

        // Try reversed pattern: "4×110kg"
        REVERSED_PATTERN.find(context)?.let { match ->
            val reps = match.groupValues[1].toIntOrNull()
            if (reps != null && reps > 0) {
                return reps
            }
        }

        return null
    }
}
