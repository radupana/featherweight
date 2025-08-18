package com.github.radupana.featherweight.data

enum class WorkoutMode {
    ACTIVE,        // Normal workout - can complete, has timer, can add notes
    READ_ONLY,     // Viewing completed workout - no edits allowed
    TEMPLATE_EDIT  // Editing workout template - can edit exercises/sets, no timer, no complete
}

