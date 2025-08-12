package com.github.radupana.featherweight.data.exercise

/**
 * Data class representing an exercise variation with all its related data.
 * This is the primary structure for working with exercises in the new schema.
 */
data class ExerciseWithDetails(
    val variation: ExerciseVariation,
    val muscles: List<VariationMuscle> = emptyList(),
    val aliases: List<VariationAlias> = emptyList(),
    val instructions: List<VariationInstruction> = emptyList()
) {
    /**
     * Get primary muscles for this variation.
     */
    fun getPrimaryMuscles(): List<MuscleGroup> {
        return muscles
            .filter { it.isPrimary }
            .map { it.muscle }
    }
    
    /**
     * Get secondary muscles for this variation.
     */
    fun getSecondaryMuscles(): List<MuscleGroup> {
        return muscles
            .filter { !it.isPrimary }
            .map { it.muscle }
    }
    
    /**
     * Get instructions of a specific type.
     */
    fun getInstructionsByType(type: InstructionType): List<VariationInstruction> {
        return instructions
            .filter { it.instructionType == type }
            .sortedBy { it.orderIndex }
    }
    
    /**
     * Get all aliases as strings.
     */
    fun getAliasStrings(): List<String> {
        return aliases.map { it.alias }
    }
    
    /**
     * Check if this exercise matches a search term (by name or alias).
     */
    fun matchesSearchTerm(searchTerm: String): Boolean {
        val lowerSearch = searchTerm.lowercase()
        
        // Check variation name
        if (variation.name.lowercase().contains(lowerSearch)) {
            return true
        }
        
        // Check aliases
        return aliases.any { it.alias.lowercase().contains(lowerSearch) }
    }
}

/**
 * Exercise search result with relevance scoring.
 */
data class ExerciseSearchResult(
    val variation: ExerciseVariation,
    val matchType: SearchMatchType,
    val relevanceScore: Float,
    val matchedText: String
)

/**
 * Exercise suggestion for user selection.
 */
data class ExerciseSuggestion(
    val variation: ExerciseVariation,
    val suggestionReason: SuggestionReason,
    val confidence: Float,
    val alternatives: List<ExerciseVariation> = emptyList()
)

enum class SearchMatchType {
    EXACT_NAME,
    EXACT_ALIAS,
    PARTIAL_NAME,
    PARTIAL_ALIAS,
    MUSCLE_GROUP,
    CATEGORY,
    EQUIPMENT
}

enum class SuggestionReason {
    SIMILAR_MUSCLES,
    SAME_CATEGORY,
    SAME_EQUIPMENT,
    PROGRESSION,
    REGRESSION,
    ALTERNATIVE,
    FREQUENTLY_USED,
    USER_HISTORY,
    BEGINNER_FRIENDLY,
    SUPERSET_PARTNER
}