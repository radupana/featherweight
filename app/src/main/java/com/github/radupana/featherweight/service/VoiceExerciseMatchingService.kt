package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.exercise.ExerciseWithAliases
import com.github.radupana.featherweight.util.ExerciseMatchingService
import com.github.radupana.featherweight.util.ExerciseSearchUtil

data class ExerciseMatchResult(
    val exerciseId: String,
    val exerciseName: String,
    val score: Int,
    val isAutoMatch: Boolean,
)

data class ExerciseMatchSuggestions(
    val bestMatch: ExerciseMatchResult?,
    val suggestions: List<ExerciseMatchResult>,
)

interface VoiceExerciseMatcher {
    fun findMatchesForExercise(
        spokenName: String,
        interpretedName: String,
        allExercises: List<ExerciseWithAliases>,
    ): ExerciseMatchSuggestions
}

class VoiceExerciseMatchingService : VoiceExerciseMatcher {
    companion object {
        const val AUTO_MATCH_THRESHOLD = 500
        const val MAX_SUGGESTIONS = 5
    }

    override fun findMatchesForExercise(
        spokenName: String,
        interpretedName: String,
        allExercises: List<ExerciseWithAliases>,
    ): ExerciseMatchSuggestions {
        val directMatch = ExerciseMatchingService.findBestExerciseMatch(interpretedName, allExercises)

        if (directMatch != null) {
            val matchedExercise = allExercises.find { it.id == directMatch }
            if (matchedExercise != null) {
                val score =
                    ExerciseSearchUtil.scoreExerciseMatch(
                        matchedExercise.name,
                        interpretedName,
                        matchedExercise.aliases,
                    )
                val result =
                    ExerciseMatchResult(
                        exerciseId = directMatch,
                        exerciseName = matchedExercise.name,
                        score = maxOf(score, AUTO_MATCH_THRESHOLD + 1),
                        isAutoMatch = true,
                    )
                return ExerciseMatchSuggestions(
                    bestMatch = result,
                    suggestions = listOf(result),
                )
            }
        }

        val scoredMatches = scoreAllExercises(interpretedName, spokenName, allExercises)

        if (scoredMatches.isEmpty()) {
            return ExerciseMatchSuggestions(bestMatch = null, suggestions = emptyList())
        }

        val bestMatch = scoredMatches.first()
        val isAutoMatch = bestMatch.score >= AUTO_MATCH_THRESHOLD

        return ExerciseMatchSuggestions(
            bestMatch = bestMatch.copy(isAutoMatch = isAutoMatch),
            suggestions = scoredMatches.take(MAX_SUGGESTIONS),
        )
    }

    private fun scoreAllExercises(
        interpretedName: String,
        spokenName: String,
        allExercises: List<ExerciseWithAliases>,
    ): List<ExerciseMatchResult> =
        allExercises
            .map { exercise ->
                val interpretedScore =
                    ExerciseSearchUtil.scoreExerciseMatch(
                        exercise.name,
                        interpretedName,
                        exercise.aliases,
                    )
                val spokenScore =
                    ExerciseSearchUtil.scoreExerciseMatch(
                        exercise.name,
                        spokenName,
                        exercise.aliases,
                    )
                val maxScore = maxOf(interpretedScore, spokenScore)

                ExerciseMatchResult(
                    exerciseId = exercise.id,
                    exerciseName = exercise.name,
                    score = maxScore,
                    isAutoMatch = false,
                )
            }.filter { it.score > 0 }
            .sortedByDescending { it.score }
}
