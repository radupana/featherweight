# Exercise Swap Issues - Analysis and Solution Plan

## Issues Identified

### 1. Equipment Names in ALL CAPS
**Location**: `ExerciseSelectorScreen.kt` lines 529, 861
**Problem**: Using `exercise.variation.equipment.name` displays enum name (e.g., "BARBELL") instead of display name
**Root Cause**: `.name` property returns the Java enum name which is in ALL_CAPS format

### 2. "Current Exercise" Hardcoded
**Location**: `ExerciseSelectorScreen.kt` line 169
**Problem**: Shows hardcoded "Current Exercise" instead of actual exercise name
**Root Cause**: The exercise name is never looked up from the `currentExercise` parameter

### 3. Broken Swap Suggestion Logic
**Location**: `ExerciseSelectorViewModel.kt` lines 498-522 (calculateSuggestionScore function)
**Problems**:
- Severely limited scoring logic (only equipment + difficulty + usage count)
- No muscle group comparison (commented out)
- No category comparison (commented out)
- Results in nonsensical suggestions (e.g., Barbell Bench Press suggested for Barbell Back Squat)

**Root Cause**: After database normalization to 3NF:
- `ExerciseWithDetails` objects are created with empty muscle lists (lines 196, 386, 411, 461)
- Muscle data is never loaded from the `variation_muscles` table
- Without muscle data, the suggestion algorithm can't make intelligent comparisons

## Solution Plan

### Fix 1: Equipment Display Name
**Simple fix**: Replace `.name` with `.displayName`
```kotlin
// BEFORE
text = exercise.variation.equipment.name

// AFTER
text = exercise.variation.equipment.displayName
```

### Fix 2: Show Actual Exercise Name in Swap Dialog
**Approach**: Get exercise name from the ExerciseLog's exerciseVariationId
```kotlin
// Add to ExerciseSelectorScreen.kt in the swap dialog section
val exerciseName = remember(currentExercise) {
    currentExercise?.let { log ->
        // Either get from a viewmodel state or fetch synchronously
        viewModel.getExerciseName(log.exerciseVariationId)
    } ?: "Unknown Exercise"
}

// Display it instead of hardcoded "Current Exercise"
Text(
    text = exerciseName,
    ...
)
```

### Fix 3: Enhance Swap Suggestion Logic

#### Step 1: Load Muscle Data
Modify `ExerciseSelectorViewModel` to properly load muscle data when creating `ExerciseWithDetails`:

```kotlin
// Add a helper method to load muscles for a variation
private suspend fun loadExerciseWithDetails(variation: ExerciseVariation): ExerciseWithDetails {
    val muscles = variationMuscleDao.getMusclesForVariation(variation.id)
    return ExerciseWithDetails(
        variation = variation,
        muscles = muscles,
        aliases = emptyList(), // Can load if needed
        instructions = emptyList() // Can load if needed
    )
}
```

#### Step 2: Improve Scoring Algorithm
Enhance the `calculateSuggestionScore` function:

```kotlin
private fun calculateSuggestionScore(
    current: ExerciseWithDetails,
    candidate: ExerciseWithDetails
): Int {
    var score = 0
    
    // Primary muscle match (highest priority)
    val currentPrimary = current.getPrimaryMuscles().toSet()
    val candidatePrimary = candidate.getPrimaryMuscles().toSet()
    val primaryOverlap = currentPrimary.intersect(candidatePrimary).size
    score += primaryOverlap * 100  // High score for primary muscle matches
    
    // Secondary muscle match
    val currentSecondary = current.getSecondaryMuscles().toSet()
    val candidateSecondary = candidate.getSecondaryMuscles().toSet()
    val secondaryOverlap = currentSecondary.intersect(candidateSecondary).size
    score += secondaryOverlap * 30  // Medium score for secondary muscle matches
    
    // Same equipment (important for home workouts)
    if (current.variation.equipment == candidate.variation.equipment) {
        score += 50
    }
    
    // Similar movement pattern (inferred from exercise name patterns)
    if (inferMovementPattern(current) == inferMovementPattern(candidate)) {
        score += 40
    }
    
    // Similar difficulty
    val difficultyDiff = kotlin.math.abs(
        current.variation.difficulty.level - candidate.variation.difficulty.level
    )
    score += (5 - difficultyDiff) * 10  // Closer difficulty = higher score
    
    // Usage count (popular exercises are good alternatives)
    score += candidate.variation.usageCount.coerceAtMost(20)
    
    return score
}

// Helper to infer movement pattern from name
private fun inferMovementPattern(exercise: ExerciseWithDetails): String? {
    val name = exercise.variation.name.lowercase()
    return when {
        name.contains("squat") -> "squat"
        name.contains("press") && name.contains("bench") -> "horizontal_push"
        name.contains("press") && !name.contains("bench") -> "vertical_push"
        name.contains("row") -> "horizontal_pull"
        name.contains("pull") && (name.contains("up") || name.contains("down")) -> "vertical_pull"
        name.contains("deadlift") -> "hinge"
        name.contains("curl") -> "isolation_pull"
        name.contains("extension") -> "isolation_push"
        else -> null
    }
}
```

#### Step 3: Better Suggestion Reasons
Create meaningful suggestion reasons based on what matched:

```kotlin
private fun getSuggestionReason(
    current: ExerciseWithDetails,
    candidate: ExerciseWithDetails
): String {
    val reasons = mutableListOf<String>()
    
    val currentPrimary = current.getPrimaryMuscles()
    val candidatePrimary = candidate.getPrimaryMuscles()
    
    if (currentPrimary.isNotEmpty() && candidatePrimary.isNotEmpty() &&
        currentPrimary.first() == candidatePrimary.first()) {
        reasons.add("Same primary muscle")
    }
    
    if (current.variation.equipment == candidate.variation.equipment) {
        reasons.add("Same equipment")
    }
    
    if (inferMovementPattern(current) == inferMovementPattern(candidate)) {
        reasons.add("Similar movement")
    }
    
    return reasons.joinToString(" • ")
}
```

## Implementation Priority

1. **High Priority** (Quick fixes):
   - Fix equipment display name (1 line change)
   - Fix "Current Exercise" display (5-10 lines)

2. **Medium Priority** (Core functionality):
   - Load muscle data for exercises
   - Enhance scoring algorithm with muscle comparison

3. **Low Priority** (Nice to have):
   - Load and use exercise aliases
   - Add movement pattern detection
   - Consider exercise relationships (progressions/regressions)

## Testing Considerations

After implementation, test these scenarios:
1. Swap from Barbell Back Squat → Should suggest other squat variations, leg exercises
2. Swap from Barbell Bench Press → Should suggest other chest/push exercises
3. Swap from Dumbbell Bicep Curl → Should suggest other arm/curl exercises
4. Equipment-constrained swaps (e.g., home workout with only dumbbells)

## Performance Considerations

- Cache muscle data lookups to avoid repeated database queries
- Consider loading all exercise details upfront if the dataset is small
- Use coroutines properly to avoid blocking the UI thread