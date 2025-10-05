package com.github.radupana.featherweight.data.exercise

/**
 * Defines the type of exercise based on its origin.
 * This provides explicit typing instead of relying on userId nullability.
 */
enum class ExerciseType {
    /**
     * System-provided exercise that's part of the core exercise library.
     * These exercises are managed centrally and synced from Firestore.
     * They cannot be modified by users.
     */
    SYSTEM,

    /**
     * User-created custom exercise.
     * These exercises are created and managed by individual users.
     */
    USER,
}
