package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.PersonalRecord
import com.github.radupana.featherweight.data.SetLog

/**
 * Service to handle batch completion of sets and PR detection
 *
 * This service ensures that when multiple sets are completed at once,
 * only the best PR for each exercise is shown to the user.
 */
class BatchCompletionService {
    fun filterBestPRsPerExercise(allPRs: List<PersonalRecord>): List<PersonalRecord> {
        if (allPRs.isEmpty()) return emptyList()

        // Group PRs by exercise and keep only the best one per exercise
        return allPRs
            .groupBy { it.exerciseId }
            .mapValues { (_, prs) ->
                // Keep the PR with highest 1RM, or highest weight if 1RM is null
                prs.maxByOrNull { pr ->
                    pr.estimated1RM ?: pr.weight
                }
            }.values
            .filterNotNull()
    }

    fun findBestSetForOneRM(
        sets: List<SetLog>,
        calculate1RM: (SetLog) -> Float?,
    ): SetLog? {
        if (sets.isEmpty()) return null

        // Find the set with the highest estimated 1RM
        return sets
            .mapNotNull { set -> calculate1RM(set)?.let { oneRM -> set to oneRM } }
            .maxByOrNull { it.second }
            ?.first
    }

    fun groupSetsByExercise(
        sets: List<SetLog>,
        getExerciseId: (SetLog) -> String?,
    ): Map<String, List<SetLog>> =
        sets
            .mapNotNull { set ->
                getExerciseId(set)?.let { exerciseId ->
                    exerciseId to set
                }
            }.groupBy({ it.first }, { it.second })

    fun shouldUseBatchCompletion(setsToComplete: Int): Boolean = setsToComplete >= 2
}
