package com.github.radupana.featherweight.sync.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class FirestoreWorkout(
    @DocumentId val id: String = "",
    val localId: String = "",
    val userId: String = "",
    val name: String? = null,
    val notes: String? = null,
    val date: Timestamp = Timestamp.now(),
    val status: String = "NOT_STARTED",
    val programmeId: String? = null,
    val weekNumber: Int? = null,
    val dayNumber: Int? = null,
    val programmeWorkoutName: String? = null,
    val isProgrammeWorkout: Boolean = false,
    val durationSeconds: Long? = null,
    val timerStartTime: String? = null,
    val timerElapsedSeconds: Int = 0,
    @ServerTimestamp val lastModified: Timestamp? = null,
)

data class FirestoreExerciseLog(
    @DocumentId val id: String = "",
    val localId: String = "",
    val workoutId: String = "",
    val exerciseVariationId: String = "",
    val exerciseOrder: Int = 0,
    val supersetGroup: Int? = null,
    val notes: String? = null,
    val originalVariationId: String? = null,
    val isSwapped: Boolean = false,
    @ServerTimestamp val lastModified: Timestamp? = null,
)

data class FirestoreSetLog(
    @DocumentId val id: String = "",
    val localId: String = "",
    val exerciseLogId: String = "",
    val setOrder: Int = 0,
    val targetReps: Int? = null,
    val targetWeight: Float? = null,
    val targetRpe: Float? = null,
    val actualReps: Int = 0,
    val actualWeight: Float = 0f,
    val actualRpe: Float? = null,
    val suggestedWeight: Float? = null,
    val suggestedReps: Int? = null,
    val suggestionSource: String? = null,
    val suggestionConfidence: Float? = null,
    val calculationDetails: String? = null,
    val tag: String? = null,
    val notes: String? = null,
    val isCompleted: Boolean = false,
    val completedAt: String? = null,
    @ServerTimestamp val lastModified: Timestamp? = null,
)

data class FirestoreExerciseCore(
    @DocumentId val id: String = "",
    val localId: String = "",
    val createdByUserId: String? = null,
    val name: String = "",
    val category: String = "",
    val movementPattern: String = "",
    val isCompound: Boolean = false,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    @ServerTimestamp val lastModified: Timestamp? = null,
)

data class FirestoreExerciseVariation(
    @DocumentId val id: String = "",
    val localId: String = "",
    val createdByUserId: String? = null,
    val coreExerciseId: String = "",
    val name: String = "",
    val equipment: String = "",
    val difficulty: String = "",
    val requiresWeight: Boolean = false,
    val recommendedRepRange: String? = null,
    val rmScalingType: String = "STANDARD",
    val restDurationSeconds: Int = 90,
    val usageCount: Int = 0,
    val isCustom: Boolean = false,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    @ServerTimestamp val lastModified: Timestamp? = null,
)

data class FirestoreVariationMuscle(
    @DocumentId val id: String = "",
    val variationId: String = "",
    val muscle: String = "",
    val isPrimary: Boolean = false,
    val emphasisModifier: Float = 1.0f,
    @ServerTimestamp val lastModified: Timestamp? = null,
)

data class FirestoreVariationInstruction(
    @DocumentId val id: String = "",
    val localId: String = "",
    val variationId: String = "",
    val instructionType: String = "",
    val content: String = "",
    val orderIndex: Int = 0,
    val languageCode: String = "en",
    @ServerTimestamp val lastModified: Timestamp? = null,
)

data class FirestoreVariationAlias(
    @DocumentId val id: String = "",
    val localId: String = "",
    val variationId: String = "",
    val alias: String = "",
    val confidence: Float = 1.0f,
    val languageCode: String = "en",
    val source: String = "manual",
    @ServerTimestamp val lastModified: Timestamp? = null,
)

data class FirestoreProgramme(
    @DocumentId val id: String = "",
    val localId: String = "",
    val userId: String? = null,
    val name: String = "",
    val description: String? = null,
    val durationWeeks: Int = 0,
    val programmeType: String = "",
    val difficulty: String = "",
    val isCustom: Boolean = false,
    val isActive: Boolean = false,
    val status: String = "NOT_STARTED",
    val createdAt: Timestamp = Timestamp.now(),
    val startedAt: Timestamp? = null,
    val completedAt: Timestamp? = null,
    val completionNotes: String? = null,
    val notesCreatedAt: Timestamp? = null,
    val squatMax: Float? = null,
    val benchMax: Float? = null,
    val deadliftMax: Float? = null,
    val ohpMax: Float? = null,
    val weightCalculationRules: String? = null,
    val progressionRules: String? = null,
    val templateName: String? = null,
    @ServerTimestamp val lastModified: Timestamp? = null,
)

data class FirestoreProgrammeWeek(
    @DocumentId val id: String = "",
    val localId: String = "",
    val userId: String? = null,
    val programmeId: String = "",
    val weekNumber: Int = 0,
    val name: String? = null,
    val description: String? = null,
    val focusAreas: String? = null,
    val intensityLevel: String? = null,
    val volumeLevel: String? = null,
    val isDeload: Boolean = false,
    val phase: String? = null,
    @ServerTimestamp val lastModified: Timestamp? = null,
)

data class FirestoreProgrammeWorkout(
    @DocumentId val id: String = "",
    val localId: String = "",
    val userId: String? = null,
    val weekId: String = "",
    val dayNumber: Int = 0,
    val name: String = "",
    val description: String? = null,
    val estimatedDuration: Int? = null,
    val workoutStructure: String = "",
    @ServerTimestamp val lastModified: Timestamp? = null,
)

data class FirestoreProgrammeProgress(
    @DocumentId val id: String = "",
    val localId: String = "",
    val userId: String? = null,
    val programmeId: String = "",
    val currentWeek: Int = 0,
    val currentDay: Int = 0,
    val completedWorkouts: Int = 0,
    val totalWorkouts: Int = 0,
    val lastWorkoutDate: Timestamp? = null,
    val adherencePercentage: Float = 0f,
    val strengthProgress: String? = null,
    @ServerTimestamp val lastModified: Timestamp? = null,
)

// =====================================================
// USER PROFILE / STATS ENTITIES
// =====================================================

data class FirestoreUserExerciseMax(
    @DocumentId val id: String = "",
    val localId: String = "",
    val userId: String? = null,
    val exerciseVariationId: String = "",
    val isCustomExercise: Boolean? = false,
    val mostWeightLifted: Float = 0f,
    val mostWeightReps: Int = 0,
    val mostWeightRpe: Float? = null,
    val mostWeightDate: Timestamp = Timestamp.now(),
    val oneRMEstimate: Float = 0f,
    val oneRMContext: String = "",
    val oneRMConfidence: Float = 0f,
    val oneRMDate: Timestamp = Timestamp.now(),
    val oneRMType: String = "AUTOMATICALLY_CALCULATED",
    val notes: String? = null,
    @ServerTimestamp val lastModified: Timestamp? = null,
)

data class FirestoreOneRMHistory(
    @DocumentId val id: String = "",
    val localId: String = "",
    val userId: String? = null,
    val exerciseVariationId: String = "",
    val isCustomExercise: Boolean? = false,
    val oneRMEstimate: Float = 0f,
    val context: String = "",
    val recordedAt: Timestamp = Timestamp.now(),
    @ServerTimestamp val lastModified: Timestamp? = null,
)

data class FirestorePersonalRecord(
    @DocumentId val id: String = "",
    val localId: String = "",
    val userId: String? = null,
    val exerciseVariationId: String = "",
    val isCustomExercise: Boolean? = false,
    val weight: Float = 0f,
    val reps: Int = 0,
    val rpe: Float? = null,
    val recordDate: Timestamp = Timestamp.now(),
    val previousWeight: Float? = null,
    val previousReps: Int? = null,
    val previousDate: Timestamp? = null,
    val improvementPercentage: Float = 0f,
    val recordType: String = "",
    val volume: Float = 0f,
    val estimated1RM: Float? = null,
    val notes: String? = null,
    val workoutId: String? = null,
    @ServerTimestamp val lastModified: Timestamp? = null,
)

data class FirestoreExerciseSwapHistory(
    @DocumentId val id: String = "",
    val localId: String = "",
    val userId: String? = null,
    val originalExerciseId: String = "",
    val swappedToExerciseId: String = "",
    val swapDate: Timestamp = Timestamp.now(),
    val workoutId: String? = null,
    val programmeId: String? = null,
    @ServerTimestamp val lastModified: Timestamp? = null,
)

data class FirestoreExercisePerformanceTracking(
    @DocumentId val id: String = "",
    val localId: String = "",
    val userId: String? = null,
    val programmeId: String = "",
    val exerciseName: String = "",
    val targetWeight: Float = 0f,
    val achievedWeight: Float = 0f,
    val targetSets: Int = 0,
    val completedSets: Int = 0,
    val targetReps: Int? = null,
    val achievedReps: Int = 0,
    val missedReps: Int = 0,
    val wasSuccessful: Boolean = false,
    val workoutDate: Timestamp = Timestamp.now(),
    val workoutId: String = "",
    val isDeloadWorkout: Boolean = false,
    val deloadReason: String? = null,
    val averageRpe: Float? = null,
    val notes: String? = null,
    @ServerTimestamp val lastModified: Timestamp? = null,
)

data class FirestoreGlobalExerciseProgress(
    @DocumentId val id: String = "",
    val localId: String = "",
    val userId: String? = null,
    val exerciseVariationId: String = "",
    val currentWorkingWeight: Float = 0f,
    val estimatedMax: Float = 0f,
    val lastUpdated: Timestamp = Timestamp.now(),
    val recentAvgRpe: Float? = null,
    val consecutiveStalls: Int = 0,
    val lastPrDate: Timestamp? = null,
    val lastPrWeight: Float? = null,
    val trend: String = "STALLING",
    val volumeTrend: String? = null,
    val totalVolumeLast30Days: Float = 0f,
    @ServerTimestamp val lastModified: Timestamp? = null,
)

data class FirestoreTrainingAnalysis(
    @DocumentId val id: String = "",
    val localId: String = "",
    val userId: String? = null,
    val analysisDate: Timestamp = Timestamp.now(),
    val periodStart: String = "",
    val periodEnd: String = "",
    val overallAssessment: String = "",
    val keyInsightsJson: String = "",
    val recommendationsJson: String = "",
    val warningsJson: String = "",
    @ServerTimestamp val lastModified: Timestamp? = null,
)

data class FirestoreParseRequest(
    @DocumentId val id: String = "",
    val localId: String = "",
    val userId: String? = null,
    val rawText: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val status: String = "PROCESSING",
    val error: String? = null,
    val resultJson: String? = null,
    val completedAt: Timestamp? = null,
    @ServerTimestamp val lastModified: Timestamp? = null,
)

data class FirestoreSyncMetadata(
    @DocumentId val id: String = "",
    val userId: String = "",
    val deviceId: String = "",
    val deviceName: String = "",
    @ServerTimestamp val lastSyncTime: Timestamp? = null,
)

data class FirestoreWorkoutTemplate(
    @DocumentId val id: String = "",
    val localId: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    @ServerTimestamp val lastModified: Timestamp? = null,
)

data class FirestoreTemplateExercise(
    @DocumentId val id: String = "",
    val localId: String = "",
    val userId: String = "",
    val templateId: String = "",
    val exerciseVariationId: String = "",
    val exerciseOrder: Int = 0,
    val supersetGroup: Int? = null,
    val notes: String? = null,
    @ServerTimestamp val lastModified: Timestamp? = null,
)

data class FirestoreTemplateSet(
    @DocumentId val id: String = "",
    val localId: String = "",
    val userId: String = "",
    val templateExerciseId: String = "",
    val setOrder: Int = 0,
    val targetReps: Int = 0,
    val targetWeight: Float? = null,
    val targetRpe: Float? = null,
    val notes: String? = null,
    @ServerTimestamp val lastModified: Timestamp? = null,
)
