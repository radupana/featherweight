package com.github.radupana.featherweight.service

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class ExerciseMatch(
    val exerciseName: String,
    val confidence: Float,
    val reason: String
)

class ExerciseNameMatcher {
    
    companion object {
        // Common exercise aliases and abbreviations
        private val EXERCISE_ALIASES = mapOf(
            // Bench variations
            "bench" to "Barbell Bench Press",
            "bench press" to "Barbell Bench Press",
            "bb bench" to "Barbell Bench Press",
            "db bench" to "Dumbbell Bench Press",
            "incline bench" to "Incline Barbell Bench Press",
            "decline bench" to "Decline Barbell Bench Press",
            
            // Squat variations
            "squat" to "Barbell Squat",
            "back squat" to "Barbell Squat",
            "front squat" to "Front Squat",
            "goblet squat" to "Goblet Squat",
            "hack squat" to "Hack Squat",
            "bulgarian split squat" to "Bulgarian Split Squat",
            "bss" to "Bulgarian Split Squat",
            
            // Deadlift variations
            "deadlift" to "Barbell Deadlift",
            "conventional deadlift" to "Barbell Deadlift",
            "sumo deadlift" to "Sumo Deadlift",
            "rdl" to "Romanian Deadlift",
            "romanian deadlift" to "Romanian Deadlift",
            "sldl" to "Stiff Leg Deadlift",
            "trap bar deadlift" to "Trap Bar Deadlift",
            
            // Press variations
            "ohp" to "Barbell Overhead Press",
            "overhead press" to "Barbell Overhead Press",
            "military press" to "Barbell Overhead Press",
            "shoulder press" to "Dumbbell Shoulder Press",
            "db shoulder press" to "Dumbbell Shoulder Press",
            
            // Row variations
            "bb row" to "Barbell Row",
            "bent over row" to "Barbell Row",
            "db row" to "Dumbbell Row",
            "cable row" to "Cable Row",
            "t-bar row" to "T-Bar Row",
            
            // Pull variations
            "pullup" to "Pull-up",
            "pull up" to "Pull-up",
            "chinup" to "Chin-up",
            "chin up" to "Chin-up",
            "lat pulldown" to "Lat Pulldown",
            "pulldown" to "Lat Pulldown",
            
            // Curl variations
            "bb curl" to "Barbell Curl",
            "db curl" to "Dumbbell Curl",
            "hammer curl" to "Hammer Curl",
            "preacher curl" to "Preacher Curl",
            "ez bar curl" to "EZ Bar Curl",
            
            // Tricep variations
            "tricep extension" to "Tricep Extension",
            "overhead tricep extension" to "Overhead Tricep Extension",
            "tricep pushdown" to "Tricep Pushdown",
            "close grip bench" to "Close Grip Bench Press",
            "cgbp" to "Close Grip Bench Press",
            
            // Core
            "abs" to "Ab Wheel",
            "plank" to "Plank",
            "side plank" to "Side Plank",
            "crunch" to "Crunch",
            "situp" to "Sit-up",
            "sit up" to "Sit-up",
            
            // Legs
            "leg press" to "Leg Press",
            "leg curl" to "Leg Curl",
            "leg extension" to "Leg Extension",
            "calf raise" to "Calf Raise",
            "lunge" to "Barbell Lunge"
        )
        
        private const val EXACT_MATCH_CONFIDENCE = 1.0f
        private const val ALIAS_MATCH_CONFIDENCE = 0.95f
        private const val HIGH_FUZZY_CONFIDENCE = 0.8f
        private const val MEDIUM_FUZZY_CONFIDENCE = 0.6f
        private const val LOW_FUZZY_CONFIDENCE = 0.4f
    }
    
    fun findBestMatches(
        userInput: String,
        exerciseDatabase: List<String>,
        topN: Int = 3
    ): List<ExerciseMatch> {
        val normalizedInput = userInput.trim().lowercase()
        val matches = mutableListOf<ExerciseMatch>()
        
        // 1. Check for exact match
        exerciseDatabase.forEach { exercise ->
            if (exercise.lowercase() == normalizedInput) {
                matches.add(
                    ExerciseMatch(
                        exerciseName = exercise,
                        confidence = EXACT_MATCH_CONFIDENCE,
                        reason = "Exact match"
                    )
                )
                return listOf(matches.first())
            }
        }
        
        // 2. Check aliases
        EXERCISE_ALIASES[normalizedInput]?.let { aliasMatch ->
            exerciseDatabase.find { it.equals(aliasMatch, ignoreCase = true) }?.let { exercise ->
                matches.add(
                    ExerciseMatch(
                        exerciseName = exercise,
                        confidence = ALIAS_MATCH_CONFIDENCE,
                        reason = "Known alias"
                    )
                )
            }
        }
        
        // 3. Fuzzy matching
        exerciseDatabase.forEach { exercise ->
            val similarity = calculateSimilarity(normalizedInput, exercise.lowercase())
            
            if (similarity > LOW_FUZZY_CONFIDENCE) {
                val confidence = when {
                    similarity > 0.85 -> HIGH_FUZZY_CONFIDENCE
                    similarity > 0.7 -> MEDIUM_FUZZY_CONFIDENCE
                    else -> LOW_FUZZY_CONFIDENCE
                }
                
                val reason = when {
                    exercise.lowercase().contains(normalizedInput) -> "Contains search term"
                    normalizedInput.contains(exercise.lowercase()) -> "Search contains exercise"
                    else -> "Similar spelling"
                }
                
                matches.add(
                    ExerciseMatch(
                        exerciseName = exercise,
                        confidence = confidence * similarity,
                        reason = reason
                    )
                )
            }
        }
        
        // Sort by confidence and return top N
        return matches
            .sortedByDescending { it.confidence }
            .take(topN)
    }
    
    private fun calculateSimilarity(s1: String, s2: String): Float {
        // Combine multiple similarity metrics
        val levenshteinSim = 1f - (levenshteinDistance(s1, s2).toFloat() / max(s1.length, s2.length))
        val containsSim = if (s1.contains(s2) || s2.contains(s1)) 0.8f else 0f
        val wordMatchSim = calculateWordMatchSimilarity(s1, s2)
        
        // Weighted average
        return (levenshteinSim * 0.4f + containsSim * 0.3f + wordMatchSim * 0.3f)
    }
    
    private fun calculateWordMatchSimilarity(s1: String, s2: String): Float {
        val words1 = s1.split(" ", "-", "_").filter { it.isNotEmpty() }.toSet()
        val words2 = s2.split(" ", "-", "_").filter { it.isNotEmpty() }.toSet()
        
        if (words1.isEmpty() || words2.isEmpty()) return 0f
        
        val intersection = words1.intersect(words2).size
        val union = words1.union(words2).size
        
        return intersection.toFloat() / union.toFloat()
    }
    
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        
        for (i in 0..s1.length) {
            dp[i][0] = i
        }
        
        for (j in 0..s2.length) {
            dp[0][j] = j
        }
        
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,     // deletion
                    dp[i][j - 1] + 1,     // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        
        return dp[s1.length][s2.length]
    }
    
    fun resolveExerciseName(
        userInput: String,
        exerciseDatabase: List<String>
    ): String? {
        val matches = findBestMatches(userInput, exerciseDatabase, 1)
        return if (matches.isNotEmpty() && matches.first().confidence >= MEDIUM_FUZZY_CONFIDENCE) {
            matches.first().exerciseName
        } else {
            null
        }
    }
}