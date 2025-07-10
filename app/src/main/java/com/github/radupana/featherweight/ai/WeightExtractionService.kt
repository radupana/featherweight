package com.github.radupana.featherweight.ai

import java.util.regex.Pattern

data class ExtractedWeight(
    val exerciseName: String,
    val weight: Float,
    val source: String, // "explicit", "1rm", "max", "pr"
)

class WeightExtractionService {
    private val exerciseAliases =
        mapOf(
            "squat" to listOf("back squat", "barbell squat", "squats"),
            "bench" to listOf("bench press", "barbell bench press", "bench pressing"),
            "deadlift" to listOf("conventional deadlift", "deadlifts", "deadlifting"),
            "press" to listOf("overhead press", "shoulder press", "military press", "ohp"),
            "row" to listOf("barbell row", "bent over row", "rows"),
            "curl" to listOf("barbell curl", "bicep curl", "curls"),
        )

    fun extractWeights(input: String): List<ExtractedWeight> {
        val results = mutableListOf<ExtractedWeight>()
        val normalizedInput = input.lowercase()

        // Extract explicit weights: "I can squat 100kg", "bench 80kg"
        results.addAll(extractExplicitWeights(normalizedInput))

        // Extract 1RM mentions: "squat max 140kg", "bench PR 100kg"
        results.addAll(extract1RMs(normalizedInput))

        // Extract max/PR mentions: "my squat max is 140kg"
        results.addAll(extractMaxes(normalizedInput))

        return results.distinctBy { "${it.exerciseName}_${it.source}" }
    }

    private fun extractExplicitWeights(input: String): List<ExtractedWeight> {
        val results = mutableListOf<ExtractedWeight>()

        // Pattern: "I can [exercise] [weight]" or "[exercise] [weight]"
        val patterns =
            listOf(
                "(?:i can |i do |i )?(?:do |perform )?([a-z ]+?)\\s+(?:at |with |for )?($WEIGHT_PATTERN)",
                "(?:my )?([a-z ]+?)\\s+(?:is |are |at )?($WEIGHT_PATTERN)",
                "($WEIGHT_PATTERN)\\s+([a-z ]+)",
            )

        patterns.forEach { pattern ->
            val regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE)
            val matcher = regex.matcher(input)

            while (matcher.find()) {
                val exercisePart =
                    if (pattern.contains("WEIGHT_PATTERN.*[a-z]")) {
                        matcher.group(2)
                    } else {
                        matcher.group(1)
                    }
                val weightPart =
                    if (pattern.contains("WEIGHT_PATTERN.*[a-z]")) {
                        matcher.group(1)
                    } else {
                        matcher.group(2)
                    }

                val exercise = findExerciseMatch(exercisePart?.trim() ?: "")
                val weight = parseWeight(weightPart ?: "")

                if (exercise != null && weight != null) {
                    results.add(ExtractedWeight(exercise, weight, "explicit"))
                }
            }
        }

        return results
    }

    private fun extract1RMs(input: String): List<ExtractedWeight> {
        val results = mutableListOf<ExtractedWeight>()

        // Pattern: "squat 1rm 140kg", "bench 1 rep max 100kg"
        val pattern = "([a-z ]+?)\\s+(?:1rm|1 rep max|one rep max)\\s+($WEIGHT_PATTERN)"
        val regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE)
        val matcher = regex.matcher(input)

        while (matcher.find()) {
            val exerciseName = matcher.group(1)?.trim()
            val weightStr = matcher.group(2)

            val exercise = findExerciseMatch(exerciseName ?: "")
            val weight = parseWeight(weightStr ?: "")

            if (exercise != null && weight != null) {
                results.add(ExtractedWeight(exercise, weight, "1rm"))
            }
        }

        return results
    }

    private fun extractMaxes(input: String): List<ExtractedWeight> {
        val results = mutableListOf<ExtractedWeight>()

        // Pattern: "squat max 140kg", "bench PR 100kg", "my deadlift personal best is 180kg"
        val patterns =
            listOf(
                "([a-z ]+?)\\s+(?:max|pr|personal best|pb)\\s+(?:is |of )?($WEIGHT_PATTERN)",
                "(?:my )?([a-z ]+?)\\s+(?:max|pr|personal best|pb)\\s+(?:is |of )?($WEIGHT_PATTERN)",
                "(?:max|pr|personal best|pb)\\s+([a-z ]+?)\\s+(?:is |of )?($WEIGHT_PATTERN)",
            )

        patterns.forEach { pattern ->
            val regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE)
            val matcher = regex.matcher(input)

            while (matcher.find()) {
                val exerciseName = matcher.group(1)?.trim()
                val weightStr = matcher.group(2)

                val exercise = findExerciseMatch(exerciseName ?: "")
                val weight = parseWeight(weightStr ?: "")

                if (exercise != null && weight != null) {
                    results.add(ExtractedWeight(exercise, weight, "max"))
                }
            }
        }

        return results
    }

    private fun findExerciseMatch(input: String): String? {
        val normalizedInput = input.lowercase().trim()

        // Direct match with aliases
        exerciseAliases.forEach { (canonical, aliases) ->
            if (aliases.any { alias -> normalizedInput.contains(alias) } || normalizedInput.contains(canonical)) {
                return when (canonical) {
                    "squat" -> "Back Squat"
                    "bench" -> "Bench Press"
                    "deadlift" -> "Conventional Deadlift"
                    "press" -> "Overhead Press"
                    "row" -> "Barbell Row"
                    "curl" -> "Barbell Curl"
                    else -> null
                }
            }
        }

        return null
    }

    private fun parseWeight(weightStr: String): Float? {
        val normalizedWeight = weightStr.lowercase().replace(" ", "")

        return when {
            // kg format
            normalizedWeight.contains("kg") -> {
                normalizedWeight.replace("kg", "").toFloatOrNull()
            }
            // lbs format (convert to kg)
            normalizedWeight.contains("lbs") || normalizedWeight.contains("lb") -> {
                val lbs = normalizedWeight.replace("lbs", "").replace("lb", "").toFloatOrNull()
                lbs?.let { it * 0.453592f }
            }
            // plates format (assume 20kg plates + 20kg bar = 45kg base)
            normalizedWeight.contains("plate") -> {
                val plates = normalizedWeight.replace("plates", "").replace("plate", "").toFloatOrNull()
                plates?.let { 20f + (it * 40f) } // bar + plates on both sides
            }
            // bodyweight
            normalizedWeight.contains("bodyweight") || normalizedWeight.contains("bw") -> {
                75f // assume average bodyweight
            }
            // plain number (assume kg)
            else -> weightStr.toFloatOrNull()
        }
    }

    companion object {
        private const val WEIGHT_PATTERN = """(\d+(?:\.\d+)?(?:\s*(?:kg|lbs?|plates?|plate|bodyweight|bw)))"""
    }
}
