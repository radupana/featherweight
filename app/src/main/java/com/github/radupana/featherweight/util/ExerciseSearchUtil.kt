package com.github.radupana.featherweight.util

object ExerciseSearchUtil {
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
        aliases: List<String> = emptyList()
    ): Int {
        if (query.isBlank()) return 0
        
        val nameLower = exerciseName.lowercase()
        val queryLower = query.lowercase().trim()
        val aliasesLower = aliases.map { it.lowercase() }
        
        // 1. Exact match on name gets highest score
        if (nameLower == queryLower) {
            return 1000
        }
        
        // 2. Exact match on alias gets second highest score  
        if (aliasesLower.any { it == queryLower }) {
            return 900
        }
        
        // 3. Name contains full query
        if (nameLower.contains(queryLower)) {
            return if (nameLower.startsWith(queryLower)) 900 else 800
        }
        
        // 4. Alias contains full query
        if (aliasesLower.any { it.contains(queryLower) }) {
            val startsWithQuery = aliasesLower.any { it.startsWith(queryLower) }
            return if (startsWithQuery) 750 else 700
        }
        
        // 5. Multi-word matching - check if all search words are present
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
                    positionBonus += 50
                }
            }
            // Check aliases if not found in name
            else if (aliasesLower.any { alias ->
                alias.split("\\s+".toRegex()).any { it.contains(searchWord) }
            }) {
                matchedWordsInAliases++
            }
        }
        
        val totalMatchedWords = matchedWordsInName + matchedWordsInAliases
        
        // Only return a score if ALL search words match
        if (totalMatchedWords == searchWords.size) {
            var score = (matchedWordsInName * 100) + (matchedWordsInAliases * 50)
            // Bonus for matching all search words
            score += 200
            // Add position bonus
            score += positionBonus
            return score
        }
        
        return 0
    }
    
    /**
     * Simple boolean check if an exercise matches a search query.
     * Uses the scoring function but returns true for any non-zero score.
     */
    fun exerciseMatches(
        exerciseName: String,
        query: String,
        aliases: List<String> = emptyList()
    ): Boolean {
        return scoreExerciseMatch(exerciseName, query, aliases) > 0
    }
    
    /**
     * Filters and sorts a list of exercises based on search query.
     * Returns exercises sorted by relevance (best matches first).
     */
    fun <T> filterAndSortExercises(
        exercises: List<T>,
        query: String,
        nameExtractor: (T) -> String,
        aliasExtractor: (T) -> List<String> = { emptyList() }
    ): List<T> {
        if (query.isBlank()) return exercises
        
        return exercises
            .mapNotNull { exercise ->
                val score = scoreExerciseMatch(
                    nameExtractor(exercise),
                    query,
                    aliasExtractor(exercise)
                )
                if (score > 0) exercise to score else null
            }
            .sortedWith(
                compareByDescending<Pair<T, Int>> { it.second }
                    .thenBy { nameExtractor(it.first) }
            )
            .map { it.first }
    }
}
