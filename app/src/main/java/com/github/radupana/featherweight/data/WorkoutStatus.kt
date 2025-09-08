package com.github.radupana.featherweight.data

enum class WorkoutStatus {
    NOT_STARTED, // Workout created but no sets completed
    IN_PROGRESS, // At least one set completed but workout not finished
    COMPLETED, // Workout explicitly completed by user
    TEMPLATE, // Workout used as a template
}
