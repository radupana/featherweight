package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.*

class ProgrammeValidator {
    companion object {
        // Volume guidelines (sets per week per muscle group)
        private val VOLUME_GUIDELINES =
            mapOf(
                "beginner" to
                    mapOf(
                        MuscleGroup.CHEST to 8..16,
                        MuscleGroup.BACK to 10..18,
                        MuscleGroup.SHOULDERS to 8..16,
                        MuscleGroup.QUADS to 8..16,
                        MuscleGroup.HAMSTRINGS to 6..12,
                        MuscleGroup.GLUTES to 6..12,
                    ),
                "intermediate" to
                    mapOf(
                        MuscleGroup.CHEST to 12..20,
                        MuscleGroup.BACK to 14..22,
                        MuscleGroup.SHOULDERS to 12..20,
                        MuscleGroup.QUADS to 12..20,
                        MuscleGroup.HAMSTRINGS to 8..16,
                        MuscleGroup.GLUTES to 8..16,
                    ),
                "advanced" to
                    mapOf(
                        MuscleGroup.CHEST to 16..24,
                        MuscleGroup.BACK to 18..26,
                        MuscleGroup.SHOULDERS to 16..24,
                        MuscleGroup.QUADS to 16..24,
                        MuscleGroup.HAMSTRINGS to 10..18,
                        MuscleGroup.GLUTES to 10..18,
                    ),
            )

        // Exercise classifications for muscle groups
        private val EXERCISE_MUSCLE_GROUPS =
            mapOf(
                "bench press" to listOf(MuscleGroup.CHEST, MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS),
                "squat" to listOf(MuscleGroup.QUADS, MuscleGroup.GLUTES),
                "deadlift" to listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.BACK),
                "pull" to listOf(MuscleGroup.BACK, MuscleGroup.BICEPS),
                "row" to listOf(MuscleGroup.BACK, MuscleGroup.BICEPS),
                "press" to listOf(MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS),
                "curl" to listOf(MuscleGroup.BICEPS),
                "extension" to listOf(MuscleGroup.TRICEPS),
                "raise" to listOf(MuscleGroup.SHOULDERS),
                "lunge" to listOf(MuscleGroup.QUADS, MuscleGroup.GLUTES),
                "calf" to listOf(MuscleGroup.CALVES),
                "crunch" to listOf(MuscleGroup.CORE),
                "plank" to listOf(MuscleGroup.CORE),
            )

        // Movement pattern classifications
        private val EXERCISE_MOVEMENT_PATTERNS =
            mapOf(
                "bench press" to MovementPattern.HORIZONTAL_PUSH,
                "incline" to MovementPattern.VERTICAL_PUSH,
                "overhead press" to MovementPattern.VERTICAL_PUSH,
                "squat" to MovementPattern.SQUAT,
                "deadlift" to MovementPattern.HINGE,
                "lunge" to MovementPattern.LUNGE,
                "pull-up" to MovementPattern.VERTICAL_PULL,
                "pulldown" to MovementPattern.VERTICAL_PULL,
                "row" to MovementPattern.HORIZONTAL_PULL,
                "curl" to MovementPattern.ISOLATION,
                "extension" to MovementPattern.ISOLATION,
                "raise" to MovementPattern.ISOLATION,
            )
    }

    fun validate(
        programme: GeneratedProgrammePreview,
        userExperienceLevel: String = "intermediate",
    ): ValidationResult {
        val warnings = mutableListOf<ValidationWarning>()
        val errors = mutableListOf<ValidationError>()

        // Calculate overall programme metrics
        val totalVolume = calculateTotalVolume(programme)
        val muscleGroupVolume = calculateMuscleGroupVolume(programme)

        // Volume validation
        validateVolume(muscleGroupVolume, userExperienceLevel, warnings)

        // Balance validation
        validateMuscleBalance(muscleGroupVolume, warnings)

        // Rest day validation
        validateRestDays(programme, warnings)

        // Duration validation
        validateWorkoutDuration(programme, warnings)

        // Exercise safety validation
        validateExerciseSafety(programme, warnings)

        // Progression validation
        validateProgression(programme, warnings)

        // Movement pattern validation
        validateMovementPatterns(programme, warnings)

        val score = calculateValidationScore(warnings, errors)

        return ValidationResult(warnings, errors, score)
    }

    private fun calculateTotalVolume(programme: GeneratedProgrammePreview): VolumeMetrics {
        var totalSets = 0
        var totalReps = 0
        val muscleGroupVolume = mutableMapOf<MuscleGroup, Int>()
        val movementPatternVolume = mutableMapOf<MovementPattern, Int>()

        programme.weeks.forEach { week ->
            week.workouts.forEach { workout ->
                workout.exercises.forEach { exercise ->
                    totalSets += exercise.sets
                    totalReps += exercise.sets * ((exercise.repsMin + exercise.repsMax) / 2)

                    // Add to muscle group volume
                    val muscleGroups = getMuscleGroupsForExercise(exercise.exerciseName)
                    muscleGroups.forEach { muscle ->
                        muscleGroupVolume[muscle] = muscleGroupVolume.getOrDefault(muscle, 0) + exercise.sets
                    }

                    // Add to movement pattern volume
                    getMovementPatternForExercise(exercise.exerciseName)?.let { pattern ->
                        movementPatternVolume[pattern] = movementPatternVolume.getOrDefault(pattern, 0) + exercise.sets
                    }
                }
            }
        }

        return VolumeMetrics(
            totalSets = totalSets / programme.durationWeeks, // Weekly average
            totalReps = totalReps / programme.durationWeeks,
            muscleGroupVolume = muscleGroupVolume.mapValues { it.value / programme.durationWeeks },
            movementPatternVolume = movementPatternVolume.mapValues { it.value / programme.durationWeeks },
        )
    }

    private fun calculateMuscleGroupVolume(programme: GeneratedProgrammePreview): Map<MuscleGroup, Int> {
        val volume = mutableMapOf<MuscleGroup, Int>()

        programme.weeks.forEach { week ->
            week.workouts.forEach { workout ->
                workout.exercises.forEach { exercise ->
                    val muscleGroups = getMuscleGroupsForExercise(exercise.exerciseName)
                    muscleGroups.forEach { muscle ->
                        volume[muscle] = volume.getOrDefault(muscle, 0) + exercise.sets
                    }
                }
            }
        }

        // Return weekly average
        return volume.mapValues { it.value / programme.durationWeeks }
    }

    private fun validateVolume(
        muscleGroupVolume: Map<MuscleGroup, Int>,
        experienceLevel: String,
        warnings: MutableList<ValidationWarning>,
    ) {
        val guidelines = VOLUME_GUIDELINES[experienceLevel] ?: VOLUME_GUIDELINES["intermediate"]!!

        guidelines.forEach { (muscle, range) ->
            val currentVolume = muscleGroupVolume[muscle] ?: 0

            when {
                currentVolume < range.first -> {
                    warnings.add(
                        ValidationWarning(
                            message = "Low volume for ${muscle.displayName}",
                            category = ValidationCategory.VOLUME,
                            suggestion = "Consider adding ${range.first - currentVolume} more sets",
                        ),
                    )
                }
                currentVolume > range.last -> {
                    val severity = if (currentVolume > range.last * 1.5) "high" else "moderate"
                    warnings.add(
                        ValidationWarning(
                            message = "High volume for ${muscle.displayName}",
                            category = ValidationCategory.VOLUME,
                            suggestion = "Consider reducing volume to avoid overtraining",
                        ),
                    )
                }
            }
        }
    }

    private fun validateMuscleBalance(
        muscleGroupVolume: Map<MuscleGroup, Int>,
        warnings: MutableList<ValidationWarning>,
    ) {
        // Push/Pull balance
        val pushVolume =
            (muscleGroupVolume[MuscleGroup.CHEST] ?: 0) +
                (muscleGroupVolume[MuscleGroup.SHOULDERS] ?: 0) +
                (muscleGroupVolume[MuscleGroup.TRICEPS] ?: 0)
        val pullVolume =
            (muscleGroupVolume[MuscleGroup.BACK] ?: 0) +
                (muscleGroupVolume[MuscleGroup.BICEPS] ?: 0)

        if (pushVolume > 0 && pullVolume > 0) {
            val ratio = pushVolume.toFloat() / pullVolume.toFloat()
            if (ratio > 1.5f) {
                warnings.add(
                    ValidationWarning(
                        message = "Push/pull imbalance detected",
                        category = ValidationCategory.BALANCE,
                        suggestion = "Add more pulling exercises",
                    ),
                )
            } else if (ratio < 0.7f) {
                warnings.add(
                    ValidationWarning(
                        message = "Push/pull imbalance detected",
                        category = ValidationCategory.BALANCE,
                        suggestion = "Add more pushing exercises",
                    ),
                )
            }
        }

        // Quad/Hamstring balance
        val quadVolume = muscleGroupVolume[MuscleGroup.QUADS] ?: 0
        val hamstringVolume = muscleGroupVolume[MuscleGroup.HAMSTRINGS] ?: 0

        if (quadVolume > 0 && hamstringVolume > 0) {
            val ratio = quadVolume.toFloat() / hamstringVolume.toFloat()
            if (ratio > 2.0f) {
                warnings.add(
                    ValidationWarning(
                        message = "Quad/hamstring imbalance detected",
                        category = ValidationCategory.BALANCE,
                        suggestion = "Add more hamstring and glute work",
                    ),
                )
            }
        }
    }

    private fun validateRestDays(
        programme: GeneratedProgrammePreview,
        warnings: MutableList<ValidationWarning>,
    ) {
        if (programme.daysPerWeek >= 6) {
            warnings.add(
                ValidationWarning(
                    message = "High training frequency detected",
                    category = ValidationCategory.RECOVERY,
                    suggestion = "Consider reducing frequency or ensuring adequate sleep",
                ),
            )
        }

        // Check for consecutive high-volume days
        val avgSetsPerDay =
            programme.weeks.firstOrNull()?.workouts?.map { workout ->
                workout.exercises.sumOf { it.sets }
            } ?: emptyList()

        if (avgSetsPerDay.size >= 2) {
            for (i in 0 until avgSetsPerDay.size - 1) {
                if (avgSetsPerDay[i] > 15 && avgSetsPerDay[i + 1] > 15) {
                    warnings.add(
                        ValidationWarning(
                            message = "Consecutive high-volume days detected",
                            category = ValidationCategory.RECOVERY,
                            suggestion = "Consider spacing out high-volume sessions",
                        ),
                    )
                    break
                }
            }
        }
    }

    private fun validateWorkoutDuration(
        programme: GeneratedProgrammePreview,
        warnings: MutableList<ValidationWarning>,
    ) {
        programme.weeks.forEach { week ->
            week.workouts.forEach { workout ->
                if (workout.estimatedDuration > 120) {
                    warnings.add(
                        ValidationWarning(
                            message = "Workout duration is too long",
                            category = ValidationCategory.DURATION,
                            suggestion = "Consider splitting into separate sessions",
                        ),
                    )
                } else if (workout.estimatedDuration < 20) {
                    warnings.add(
                        ValidationWarning(
                            message = "Workout duration is too short",
                            category = ValidationCategory.DURATION,
                            suggestion = "Consider adding more exercises or volume",
                        ),
                    )
                }
            }
        }
    }

    private fun validateExerciseSafety(
        programme: GeneratedProgrammePreview,
        warnings: MutableList<ValidationWarning>,
    ) {
        programme.weeks.forEach { week ->
            week.workouts.forEach { workout ->
                workout.exercises.forEach { exercise ->
                    // Check for unsafe rep ranges with heavy compounds
                    if (isCompoundMovement(exercise.exerciseName)) {
                        if (exercise.repsMax > 15 && exercise.rpe != null && exercise.rpe > 8.5f) {
                            warnings.add(
                                ValidationWarning(
                                    message = "High RPE with high reps detected",
                                    category = ValidationCategory.SAFETY,
                                    suggestion = "Consider reducing RPE for high-rep sets",
                                ),
                            )
                        }
                    }

                    // Check for excessive volume on single exercises
                    if (exercise.sets > 8) {
                        warnings.add(
                            ValidationWarning(
                                message = "Excessive volume on single exercise",
                                category = ValidationCategory.VOLUME,
                                suggestion = "Consider splitting volume across variations",
                            ),
                        )
                    }
                }
            }
        }
    }

    private fun validateProgression(
        programme: GeneratedProgrammePreview,
        warnings: MutableList<ValidationWarning>,
    ) {
        if (programme.durationWeeks > 4) {
            // Check if there's any mention of progression
            val hasProgressionNotes = programme.weeks.any { !it.progressionNotes.isNullOrBlank() }

            if (!hasProgressionNotes) {
                warnings.add(
                    ValidationWarning(
                        message = "No progression plan detected",
                        category = ValidationCategory.PROGRESSION,
                        suggestion = "Add progressive overload instructions",
                    ),
                )
            }
        }
    }

    private fun validateMovementPatterns(
        programme: GeneratedProgrammePreview,
        warnings: MutableList<ValidationWarning>,
    ) {
        val patternCount = mutableMapOf<MovementPattern, Int>()

        programme.weeks.first().workouts.forEach { workout ->
            workout.exercises.forEach { exercise ->
                getMovementPatternForExercise(exercise.exerciseName)?.let { pattern ->
                    patternCount[pattern] = patternCount.getOrDefault(pattern, 0) + 1
                }
            }
        }

        // Check for missing fundamental patterns
        val fundamentalPatterns =
            listOf(
                MovementPattern.SQUAT,
                MovementPattern.HINGE,
                MovementPattern.HORIZONTAL_PUSH,
                MovementPattern.VERTICAL_PULL,
            )

        fundamentalPatterns.forEach { pattern ->
            if (patternCount[pattern] == null || patternCount[pattern] == 0) {
                warnings.add(
                    ValidationWarning(
                        message = "Missing fundamental movement pattern: ${pattern.displayName}",
                        category = ValidationCategory.EXERCISE_SELECTION,
                        suggestion = "Consider adding exercises for this movement",
                    ),
                )
            }
        }
    }

    private fun getMuscleGroupsForExercise(exerciseName: String): List<MuscleGroup> {
        val lowerName = exerciseName.lowercase()
        return EXERCISE_MUSCLE_GROUPS.entries
            .filter { lowerName.contains(it.key) }
            .flatMap { it.value }
            .ifEmpty { listOf(MuscleGroup.CORE) } // Default fallback
    }

    private fun getMovementPatternForExercise(exerciseName: String): MovementPattern? {
        val lowerName = exerciseName.lowercase()
        return EXERCISE_MOVEMENT_PATTERNS.entries
            .find { lowerName.contains(it.key) }
            ?.value
    }

    private fun isCompoundMovement(exerciseName: String): Boolean {
        val compounds = listOf("squat", "deadlift", "bench", "press", "row", "pull")
        val lowerName = exerciseName.lowercase()
        return compounds.any { lowerName.contains(it) }
    }

    private fun calculateValidationScore(
        warnings: List<ValidationWarning>,
        errors: List<ValidationError>,
    ): Float {
        val errorPenalty = errors.size * 0.3f
        val warningPenalty = warnings.size * 0.1f
        return (1.0f - errorPenalty - warningPenalty).coerceAtLeast(0.0f)
    }

    private fun Float.format(decimals: Int): String = "%.${decimals}f".format(this)
}
