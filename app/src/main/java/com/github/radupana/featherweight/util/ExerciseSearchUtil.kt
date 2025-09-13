package com.github.radupana.featherweight.util

object ExerciseSearchUtil {
    // Scoring constants
    private const val SCORE_EXACT_NAME_MATCH = 1000
    private const val SCORE_EXACT_ALIAS_MATCH = 900
    private const val SCORE_NAME_STARTS_WITH = 900
    private const val SCORE_NAME_CONTAINS = 800
    private const val SCORE_ALIAS_STARTS_WITH = 750
    private const val SCORE_ALIAS_CONTAINS = 700
    private const val SCORE_MULTI_WORD_BASE = 200
    private const val SCORE_PER_NAME_WORD = 100
    private const val SCORE_PER_ALIAS_WORD = 50
    private const val SCORE_POSITION_BONUS = 50

    /**
     * Matches an exercise against a search query using multi-word matching.
     * Returns a score indicating match quality (0 = no match, higher = better match).
     *
     * This search algorithm:
     * 1. Checks for exact matches (highest score)
     * 2. Checks if the full query is contained in the name
     * 3. Splits the query into words and checks if all words are present
     *
     * @param exerciseName The name of the exercise to match
     * @param query The search query
     * @param aliases Optional list of aliases to also search
     * @return Score indicating match quality (0 = no match)
     */
    fun scoreExerciseMatch(
        exerciseName: String,
        query: String,
        aliases: List<String> = emptyList(),
    ): Int {
        if (query.isBlank()) return 0

        val nameLower = exerciseName.lowercase()
        val queryLower = query.lowercase().trim()
        val aliasesLower = aliases.map { it.lowercase() }

        // Check for exact matches first
        val exactMatchScore =
            when {
                nameLower == queryLower -> SCORE_EXACT_NAME_MATCH
                aliasesLower.any { it == queryLower } -> SCORE_EXACT_ALIAS_MATCH
                else -> 0
            }
        if (exactMatchScore > 0) return exactMatchScore

        // Check for contains matches
        val containsScore =
            when {
                nameLower.contains(queryLower) -> if (nameLower.startsWith(queryLower)) SCORE_NAME_STARTS_WITH else SCORE_NAME_CONTAINS
                aliasesLower.any { it.contains(queryLower) } -> {
                    if (aliasesLower.any { it.startsWith(queryLower) }) SCORE_ALIAS_STARTS_WITH else SCORE_ALIAS_CONTAINS
                }
                else -> 0
            }
        if (containsScore > 0) return containsScore

        // Multi-word matching - check if all search words are present
        val searchWords = queryLower.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        if (searchWords.isEmpty()) return 0

        val nameWords = nameLower.split("\\s+".toRegex())
        var matchedWordsInName = 0
        var matchedWordsInAliases = 0
        var positionBonus = 0

        searchWords.forEachIndexed { index, searchWord ->
            // Check if any word in the exercise name contains this search word
            if (nameWords.any { it.contains(searchWord) }) {
                matchedWordsInName++
                // Bonus for words at the beginning
                if (index == 0 && nameWords.first().startsWith(searchWord)) {
                    positionBonus += SCORE_POSITION_BONUS
                }
            }
            // Check aliases if not found in name
            else if (aliasesLower.any { alias ->
                    alias.split("\\s+".toRegex()).any { it.contains(searchWord) }
                }
            ) {
                matchedWordsInAliases++
            }
        }

        val totalMatchedWords = matchedWordsInName + matchedWordsInAliases

        // Calculate multi-word score
        return if (totalMatchedWords == searchWords.size) {
            (matchedWordsInName * SCORE_PER_NAME_WORD) + (matchedWordsInAliases * SCORE_PER_ALIAS_WORD) + SCORE_MULTI_WORD_BASE + positionBonus
        } else {
            0
        }
    }

    /**
     * Simple boolean check if an exercise matches a search query.
     * Uses the scoring function but returns true for any non-zero score.
     */
    fun exerciseMatches(
        exerciseName: String,
        query: String,
        aliases: List<String> = emptyList(),
    ): Boolean = scoreExerciseMatch(exerciseName, query, aliases) > 0

    /**
     * Filters and sorts a list of exercises based on search query.
     * Returns exercises sorted by relevance (best matches first).
     */
    fun <T> filterAndSortExercises(
        exercises: List<T>,
        query: String,
        nameExtractor: (T) -> String,
        aliasExtractor: (T) -> List<String> = { emptyList() },
    ): List<T> {
        if (query.isBlank()) return exercises

        return exercises
            .mapNotNull { exercise ->
                val score =
                    scoreExerciseMatch(
                        nameExtractor(exercise),
                        query,
                        aliasExtractor(exercise),
                    )
                if (score > 0) exercise to score else null
            }.sortedWith(
                compareByDescending<Pair<T, Int>> { it.second }
                    .thenBy { nameExtractor(it.first) },
            ).map { it.first }
    }
}
