package com.github.radupana.featherweight.sync.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

/**
 * Denormalized exercise model for Firestore.
 * Contains all exercise data in a single document for optimal read performance.
 */
data class FirestoreExercise(
    // Core exercise data (embedded)
    @PropertyName("coreName")
    val coreName: String = "",
    @PropertyName("coreCategory")
    val coreCategory: String = "",
    @PropertyName("coreMovementPattern")
    val coreMovementPattern: String = "",
    @PropertyName("coreIsCompound")
    val coreIsCompound: Boolean = false,
    // Variation data
    @PropertyName("name")
    val name: String = "",
    @PropertyName("equipment")
    val equipment: String = "",
    @PropertyName("difficulty")
    val difficulty: String = "",
    @PropertyName("requiresWeight")
    val requiresWeight: Boolean = false,
    @PropertyName("recommendedRepRange")
    val recommendedRepRange: String? = null,
    @PropertyName("rmScalingType")
    val rmScalingType: String = "STANDARD",
    @PropertyName("restDurationSeconds")
    val restDurationSeconds: Int = 90,
    // Embedded arrays
    @PropertyName("muscles")
    val muscles: List<FirestoreMuscle> = emptyList(),
    @PropertyName("aliases")
    val aliases: List<String> = emptyList(),
    @PropertyName("instructions")
    val instructions: List<FirestoreInstruction> = emptyList(),
    // Metadata
    @PropertyName("createdAt")
    val createdAt: String? = null,
    @PropertyName("updatedAt")
    val updatedAt: String? = null,
) {
    // No-arg constructor required for Firestore
    constructor() : this(
        coreName = "",
        coreCategory = "",
        coreMovementPattern = "",
        coreIsCompound = false,
        name = "",
        equipment = "",
        difficulty = "",
        requiresWeight = false,
        recommendedRepRange = null,
        rmScalingType = "STANDARD",
        restDurationSeconds = 90,
        muscles = emptyList(),
        aliases = emptyList(),
        instructions = emptyList(),
        createdAt = null,
        updatedAt = null,
    )
}

/**
 * Muscle mapping for an exercise.
 */
data class FirestoreMuscle(
    @PropertyName("muscle")
    val muscle: String = "",
    @PropertyName("isPrimary")
    val isPrimary: Boolean = false,
    @PropertyName("emphasisModifier")
    val emphasisModifier: Double = 1.0,
) {
    // No-arg constructor required for Firestore
    constructor() : this("", false, 1.0)
}

/**
 * Exercise instruction.
 */
data class FirestoreInstruction(
    @PropertyName("type")
    val type: String = "EXECUTION",
    @PropertyName("content")
    val content: String = "",
    @PropertyName("orderIndex")
    val orderIndex: Int = 0,
    @PropertyName("languageCode")
    val languageCode: String = "en",
) {
    // No-arg constructor required for Firestore
    constructor() : this("EXECUTION", "", 0, "en")
}

/**
 * User exercise usage tracking.
 */
data class FirestoreExerciseUsage(
    @PropertyName("id")
    val id: String = "",
    @PropertyName("localId")
    val localId: String = "",
    @PropertyName("userId")
    val userId: String = "",
    @PropertyName("exerciseId")
    val exerciseId: String = "",
    @PropertyName("usageCount")
    val usageCount: Int = 0,
    @PropertyName("lastUsedAt")
    val lastUsedAt: Timestamp? = null,
    @PropertyName("personalNotes")
    val personalNotes: String? = null,
    @PropertyName("createdAt")
    val createdAt: Timestamp? = null,
    @PropertyName("updatedAt")
    val updatedAt: Timestamp? = null,
    @PropertyName("lastModified")
    val lastModified: Timestamp? = null,
) {
    // No-arg constructor required for Firestore
    constructor() : this(
        id = "",
        localId = "",
        userId = "",
        exerciseId = "",
        usageCount = 0,
        lastUsedAt = null,
        personalNotes = null,
        createdAt = null,
        updatedAt = null,
        lastModified = null,
    )
}
