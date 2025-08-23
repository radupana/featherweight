package com.github.radupana.featherweight.data

enum class WorkoutMode {
    ACTIVE, // Normal workout - can complete, has timer, can add notes
    TEMPLATE_EDIT, // Editing workout template - can edit exercises/sets, no timer, no complete
}
