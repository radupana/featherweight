package com.github.radupana.featherweight.sync.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

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

/**
 * Typed model for custom exercises.
 * Assembled from normalized Firestore subcollections by FirestoreRepository.
 * Replaces the unsafe Map<String, Any> pattern.
 */
data class FirestoreCustomExercise(
    @DocumentId
    val id: String = "",
    val type: String = "USER",
    val userId: String = "",
    val name: String = "",
    val category: String = "",
    val movementPattern: String? = null,
    val isCompound: Boolean = true,
    val equipment: String = "",
    val difficulty: String? = null,
    val requiresWeight: Boolean = true,
    val rmScalingType: String? = null,
    val restDurationSeconds: Int? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val isDeleted: Boolean = false,
    @ServerTimestamp
    val lastModified: Timestamp? = null,
    // Assembled from subcollections (not stored in main document):
    val muscles: List<FirestoreMuscle> = emptyList(),
    val aliases: List<String> = emptyList(),
    val instructions: List<FirestoreInstruction> = emptyList(),
) {
    // No-arg constructor required for Firestore
    constructor() : this(
        id = "",
        type = "USER",
        userId = "",
        name = "",
        category = "",
        movementPattern = null,
        isCompound = true,
        equipment = "",
        difficulty = null,
        requiresWeight = true,
        rmScalingType = null,
        restDurationSeconds = null,
        createdAt = null,
        updatedAt = null,
        isDeleted = false,
        lastModified = null,
        muscles = emptyList(),
        aliases = emptyList(),
        instructions = emptyList(),
    )
}

/**
 * Muscle mapping stored in customExercises/{exerciseId}/muscles subcollection.
 */
data class FirestoreCustomExerciseMuscle(
    @DocumentId
    val id: String = "",
    val exerciseId: String = "",
    val muscle: String = "",
    val targetType: String = "",
    val isDeleted: Boolean = false,
    @ServerTimestamp
    val lastModified: Timestamp? = null,
) {
    // No-arg constructor required for Firestore
    constructor() : this("", "", "", "", false, null)
}

/**
 * Alias stored in customExercises/{exerciseId}/aliases subcollection.
 */
data class FirestoreCustomExerciseAlias(
    @DocumentId
    val id: String = "",
    val exerciseId: String = "",
    val alias: String = "",
    val isDeleted: Boolean = false,
    @ServerTimestamp
    val lastModified: Timestamp? = null,
) {
    // No-arg constructor required for Firestore
    constructor() : this("", "", "", false, null)
}

/**
 * Instruction stored in customExercises/{exerciseId}/instructions subcollection.
 */
data class FirestoreCustomExerciseInstruction(
    @DocumentId
    val id: String = "",
    val exerciseId: String = "",
    val instructionType: String = "",
    val orderIndex: Int = 0,
    val instructionText: String = "",
    val isDeleted: Boolean = false,
    @ServerTimestamp
    val lastModified: Timestamp? = null,
) {
    // No-arg constructor required for Firestore
    constructor() : this("", "", "", 0, "", false, null)
}
