package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.exercise.ExerciseVariation
import com.github.radupana.featherweight.data.exercise.VariationAlias

/**
 * Service for matching AI-generated exercise names to our database exercises.
 * Implements a multi-layer matching strategy to maximize successful matches.
 */
class ExerciseMatchingService {
    data class ExerciseMatch(
        val exercise: ExerciseVariation,
        val confidence: Float, // 0.0 to 1.0
        val matchReasons: List<String>,
    )

    data class ExerciseComponents(
        val equipment: String?,
        val modifiers: List<String>,
        val movement: String,
        val muscleGroup: String?,
    )

    data class UnmatchedExercise(
        val aiSuggested: String,
        val weekNumber: Int,
        val workoutNumber: Int,
        val exerciseIndex: Int,
        val bestMatches: List<ExerciseMatch>,
        val searchHints: SearchHints,
    )

    data class SearchHints(
        val detectedEquipment: String?,
        val detectedMuscleGroup: String?,
        val detectedMovementPattern: String?,
    )

    companion object {
        // Equipment standardization mappings
        private val EQUIPMENT_MAPPINGS =
            mapOf(
                "db" to "dumbbell",
                "bb" to "barbell",
                "ez" to "ez bar",
                "smith" to "smith machine",
                "bodyweight" to "bodyweight",
                "bw" to "bodyweight",
                "body weight" to "bodyweight",
            )

        // Common movement patterns
        private val MOVEMENT_PATTERNS =
            listOf(
                "press",
                "row",
                "squat",
                "deadlift",
                "curl",
                "extension",
                "raise",
                "fly",
                "pulldown",
                "pull up",
                "push up",
                "dip",
                "lunge",
                "step up",
                "thrust",
                "bridge",
                "plank",
            )

        // Muscle groups
        private val MUSCLE_GROUPS =
            listOf(
                "chest",
                "back",
                "shoulder",
                "bicep",
                "tricep",
                "quad",
                "hamstring",
                "glute",
                "calf",
                "core",
                "abs",
            )

        // Position/angle modifiers
        private val MODIFIERS =
            listOf(
                "incline",
                "decline",
                "flat",
                "seated",
                "standing",
                "lying",
                "kneeling",
                "single arm",
                "single leg",
                "close grip",
                "wide grip",
                "neutral grip",
                "front",
                "back",
                "overhead",
                "reverse",
            )
    }

    /**
     * Find the best match for an AI-generated exercise name.
     * Returns null if no match found above minimum confidence threshold.
     */
    fun findExerciseMatch(
        aiName: String,
        exercises: List<ExerciseVariation>,
        aliases: List<VariationAlias>,
        minConfidence: Float = 0.5f,
    ): ExerciseMatch? {
        // Layer 1: Direct match
        directMatch(aiName, exercises, aliases)?.let { return it }

        // Layer 2: Normalized match
        val normalized = normalizeExerciseName(aiName)
        directMatch(normalized, exercises, aliases)?.let { return it }

        // Layer 3: Component-based matching
        val components = parseExerciseComponents(normalized)
        val componentMatches =
            findComponentMatches(components, exercises)
                .filter { it.confidence >= minConfidence }
                .sortedByDescending { it.confidence }
                .take(5)

        return componentMatches.firstOrNull()
    }

    /**
     * Find multiple matches for exercise selection UI.
     */
    fun findBestMatches(
        aiName: String,
        exercises: List<ExerciseVariation>,
        aliases: List<VariationAlias>,
        limit: Int = 10,
    ): List<ExerciseMatch> {
        val allMatches = mutableListOf<ExerciseMatch>()

        // Try direct match first
        directMatch(aiName, exercises, aliases)?.let {
            allMatches.add(it)
        }

        // Try normalized match
        val normalized = normalizeExerciseName(aiName)
        if (normalized != aiName) {
            directMatch(normalized, exercises, aliases)?.let {
                if (allMatches.none { it.exercise.id == it.exercise.id }) {
                    allMatches.add(it)
                }
            }
        }

        // Component-based matches
        val components = parseExerciseComponents(normalized)
        val componentMatches =
            findComponentMatches(components, exercises)
                .filter { match -> allMatches.none { it.exercise.id == match.exercise.id } }
                .sortedByDescending { it.confidence }

        allMatches.addAll(componentMatches)

        return allMatches.take(limit)
    }

    /**
     * Extract search hints from an exercise name for UI filters.
     */
    fun extractSearchHints(aiName: String): SearchHints {
        val normalized = normalizeExerciseName(aiName).lowercase()

        val detectedEquipment =
            EQUIPMENT_MAPPINGS.entries
                .find { (key, value) ->
                    normalized.contains(key) || normalized.contains(value)
                }?.value

        val detectedMuscle =
            MUSCLE_GROUPS
                .find { normalized.contains(it) }

        val detectedMovement =
            MOVEMENT_PATTERNS
                .find { normalized.contains(it) }

        return SearchHints(
            detectedEquipment = detectedEquipment,
            detectedMuscleGroup = detectedMuscle,
            detectedMovementPattern = detectedMovement,
        )
    }

    /**
     * Normalize exercise name for better matching.
     */
    private fun normalizeExerciseName(name: String): String =
        name
            .trim()
            .replace(Regex("-+"), " ") // Hyphens to spaces
            .replace(Regex("\\s+"), " ") // Multiple spaces to single
            .replace("One Arm", "") // Remove redundant descriptors
            .replace("One-Arm", "")
            .replace("Single-Arm", "Single Arm")
            .replace("Bent Over", "") // Simplify common variations
            .replace("Bent-Over", "")
            // Normalize plural muscle names to singular for exercise matching
            .replace("Triceps", "Tricep")
            .replace("Biceps", "Bicep")
            .replace("Delts", "Delt")
            .replace("Quads", "Quad")
            .replace("Calves", "Calf")
            .replace("Glutes", "Glute")
            .replace("Hamstrings", "Hamstring")
            // Normalize plural movements to singular
            .replace("Curls", "Curl")
            .replace("Rows", "Row")
            .replace("Raises", "Raise")
            .replace("Pushdowns", "Pushdown")
            .replace("Extensions", "Extension")
            .replace("Presses", "Press")
            .split(" ")
            .joinToString(" ") { word ->
                // Standardize equipment abbreviations
                when (word.lowercase()) {
                    "db" -> "Dumbbell"
                    "bb" -> "Barbell"
                    "ez" -> "EZ Bar"
                    else -> word.split(" ").joinToString(" ") { it.capitalize() }
                }
            }

    /**
     * Direct match against exercise names and aliases.
     */
    private fun directMatch(
        name: String,
        exercises: List<ExerciseVariation>,
        aliases: List<VariationAlias>,
    ): ExerciseMatch? {
        // Exact match
        exercises.find { it.name.equals(name, ignoreCase = true) }?.let {
            return ExerciseMatch(
                exercise = it,
                confidence = 1.0f,
                matchReasons = listOf("Exact name match"),
            )
        }

        // Alias match
        aliases.find { it.alias.equals(name, ignoreCase = true) }?.let { alias ->
            exercises.find { it.id == alias.variationId }?.let { exercise ->
                return ExerciseMatch(
                    exercise = exercise,
                    confidence = 0.95f,
                    matchReasons = listOf("Alias match: ${alias.alias}"),
                )
            }
        }

        return null
    }

    /**
     * Parse exercise name into components.
     */
    private fun parseExerciseComponents(name: String): ExerciseComponents {
        val words = name.split(" ").map { it.lowercase() }

        var equipment: String? = null
        val modifiers = mutableListOf<String>()
        val movementWords = mutableListOf<String>()
        var muscleGroup: String? = null

        for (word in words) {
            when {
                // Check equipment
                EQUIPMENT_MAPPINGS.containsKey(word) -> {
                    equipment = EQUIPMENT_MAPPINGS[word]
                }

                EQUIPMENT_MAPPINGS.containsValue(word) -> {
                    equipment = word
                }
                // Check modifiers
                MODIFIERS.any { it.equals(word, ignoreCase = true) } -> {
                    modifiers.add(word)
                }
                // Check muscle groups
                MUSCLE_GROUPS.any { it.equals(word, ignoreCase = true) } -> {
                    muscleGroup = word
                }
                // Otherwise it's part of the movement
                else -> {
                    movementWords.add(word)
                }
            }
        }

        return ExerciseComponents(
            equipment = equipment,
            modifiers = modifiers,
            movement = movementWords.joinToString(" "),
            muscleGroup = muscleGroup,
        )
    }

    /**
     * Find matches based on component similarity.
     */
    private fun findComponentMatches(
        components: ExerciseComponents,
        exercises: List<ExerciseVariation>,
    ): List<ExerciseMatch> =
        exercises.mapNotNull { exercise ->
            val score = calculateComponentScore(components, exercise)
            if (score > 0) {
                ExerciseMatch(
                    exercise = exercise,
                    confidence = score,
                    matchReasons = buildMatchReasons(components, exercise),
                )
            } else {
                null
            }
        }

    /**
     * Calculate similarity score based on components.
     */
    private fun calculateComponentScore(
        components: ExerciseComponents,
        exercise: ExerciseVariation,
    ): Float {
        val exerciseLower = exercise.name.lowercase()
        var score = 0f

        // Equipment match (40%)
        components.equipment?.let { equipment ->
            if (exerciseLower.contains(equipment)) {
                score += 0.4f
            }
        }

        // Movement pattern match (30%)
        if (components.movement.isNotBlank()) {
            val movementWords = components.movement.split(" ")
            val matchingWords = movementWords.count { exerciseLower.contains(it) }
            score += (matchingWords.toFloat() / movementWords.size) * 0.3f
        }

        // Muscle group match (20%)
        // Note: In new schema, muscles are in a separate table
        // This scoring logic may need to be updated when muscle data is available
        components.muscleGroup?.let { muscle ->
            if (exerciseLower.contains(muscle)) {
                score += 0.2f
            }
        }

        // String similarity (10%)
        val similarity =
            calculateStringSimilarity(
                components.movement,
                exercise.name.lowercase(),
            )
        score += similarity * 0.1f

        return score
    }

    /**
     * Build human-readable match reasons.
     */
    private fun buildMatchReasons(
        components: ExerciseComponents,
        exercise: ExerciseVariation,
    ): List<String> {
        val reasons = mutableListOf<String>()
        val exerciseLower = exercise.name.lowercase()

        components.equipment?.let { equipment ->
            if (exerciseLower.contains(equipment)) {
                reasons.add("Equipment match: $equipment")
            }
        }

        if (components.movement.isNotBlank() &&
            exerciseLower.contains(components.movement.lowercase())
        ) {
            reasons.add("Movement match: ${components.movement}")
        }

        components.muscleGroup?.let { muscle ->
            if (exerciseLower.contains(muscle)) {
                reasons.add("Muscle group match: $muscle")
            }
        }

        return reasons
    }

    /**
     * Calculate string similarity using Levenshtein distance.
     */
    private fun calculateStringSimilarity(
        s1: String,
        s2: String,
    ): Float {
        val maxLen = maxOf(s1.length, s2.length)
        if (maxLen == 0) return 1.0f

        val distance = levenshteinDistance(s1, s2)
        return 1.0f - (distance.toFloat() / maxLen)
    }

    /**
     * Levenshtein distance implementation.
     */
    private fun levenshteinDistance(
        s1: String,
        s2: String,
    ): Int {
        val m = s1.length
        val n = s2.length

        if (m == 0) return n
        if (n == 0) return m

        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] =
                    minOf(
                        dp[i - 1][j] + 1, // deletion
                        dp[i][j - 1] + 1, // insertion
                        dp[i - 1][j - 1] + cost, // substitution
                    )
            }
        }

        return dp[m][n]
    }
}
