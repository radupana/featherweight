package com.github.radupana.featherweight.sync

import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.PRType
import com.github.radupana.featherweight.sync.converters.SyncConverters
import com.github.radupana.featherweight.sync.repository.FirestoreRepository
import com.github.radupana.featherweight.util.CloudLogger
import com.google.firebase.Timestamp

/**
 * Handles downloading and merging remote data from Firestore.
 * Extracted from SyncManager to reduce function count.
 */
class SyncDownloader(
    private val database: FeatherweightDatabase,
    private val firestoreRepository: FirestoreRepository,
) {
    suspend fun downloadAndMergeWorkouts(
        userId: String,
        lastSyncTime: Timestamp?,
    ) {
        CloudLogger.debug("SyncDownloader", "downloadAndMergeWorkouts: Starting for user $userId")
        val remoteWorkouts = firestoreRepository.downloadWorkouts(userId, lastSyncTime).getOrThrow()
        CloudLogger.debug("SyncDownloader", "downloadAndMergeWorkouts: Downloaded ${remoteWorkouts.size} workouts")
        val localWorkouts = remoteWorkouts.map { SyncConverters.fromFirestoreWorkout(it) }

        localWorkouts.forEach { workout ->
            database.workoutDao().upsertWorkout(workout)
        }
    }

    suspend fun downloadAndMergeExerciseLogs(
        userId: String,
        lastSyncTime: Timestamp?,
    ) {
        CloudLogger.debug("SyncDownloader", "downloadAndMergeExerciseLogs: Starting for user $userId")
        val remoteLogs = firestoreRepository.downloadExerciseLogs(userId, lastSyncTime).getOrThrow()
        CloudLogger.debug("SyncDownloader", "downloadAndMergeExerciseLogs: Downloaded ${remoteLogs.size} exercise logs")
        val localLogs = remoteLogs.map { SyncConverters.fromFirestoreExerciseLog(it) }

        val localWorkoutIds =
            database
                .workoutDao()
                .getAllWorkouts(userId)
                .map { it.id }
                .toSet()

        var insertedCount = 0
        var skippedCount = 0
        localLogs.forEach { log ->
            if (log.workoutId !in localWorkoutIds) {
                CloudLogger.warn("SyncDownloader", "Skipping orphaned exercise log ${log.id} - workout ${log.workoutId} doesn't exist")
                skippedCount++
                return@forEach
            }
            database.exerciseLogDao().upsertExerciseLog(log)
            insertedCount++
        }
        CloudLogger.debug("SyncDownloader", "downloadAndMergeExerciseLogs: Inserted $insertedCount, skipped $skippedCount orphaned")
    }

    suspend fun downloadAndMergeSetLogs(
        userId: String,
        lastSyncTime: Timestamp?,
    ) {
        CloudLogger.debug("SyncDownloader", "downloadAndMergeSetLogs: Starting for user $userId")
        val remoteLogs = firestoreRepository.downloadSetLogs(userId, lastSyncTime).getOrThrow()
        CloudLogger.debug("SyncDownloader", "downloadAndMergeSetLogs: Downloaded ${remoteLogs.size} set logs")
        val localLogs = remoteLogs.map { SyncConverters.fromFirestoreSetLog(it) }

        val referencedExerciseLogIds = localLogs.map { it.exerciseLogId }.distinct()
        val existingExerciseLogIds = database.exerciseLogDao().getExistingExerciseLogIds(referencedExerciseLogIds).toSet()

        var insertedCount = 0
        var skippedCount = 0
        localLogs.forEach { log ->
            if (log.exerciseLogId !in existingExerciseLogIds) {
                CloudLogger.warn("SyncDownloader", "Skipping orphaned set log ${log.id} - exercise log ${log.exerciseLogId} doesn't exist")
                skippedCount++
                return@forEach
            }
            database.setLogDao().upsertSetLog(log)
            insertedCount++
        }
        CloudLogger.debug("SyncDownloader", "downloadAndMergeSetLogs: Inserted $insertedCount, skipped $skippedCount orphaned")
    }

    suspend fun downloadAndMergeWorkoutTemplates(
        userId: String,
        lastSyncTime: Timestamp?,
    ) {
        CloudLogger.debug("SyncDownloader", "downloadAndMergeWorkoutTemplates: Starting for user $userId")
        val remoteTemplates = firestoreRepository.downloadWorkoutTemplates(userId, lastSyncTime).getOrThrow()
        CloudLogger.debug("SyncDownloader", "downloadAndMergeWorkoutTemplates: Downloaded ${remoteTemplates.size} templates")
        val localTemplates = remoteTemplates.map { SyncConverters.fromFirestoreWorkoutTemplate(it) }

        localTemplates.forEach { template ->
            database.workoutTemplateDao().upsertTemplate(template)
        }
    }

    suspend fun downloadAndMergeTemplateExercises(
        userId: String,
        lastSyncTime: Timestamp?,
    ) {
        val remoteExercises = firestoreRepository.downloadTemplateExercises(userId, lastSyncTime).getOrThrow()
        val localExercises = remoteExercises.map { SyncConverters.fromFirestoreTemplateExercise(it) }

        val localTemplateIds =
            database
                .workoutTemplateDao()
                .getTemplates(userId)
                .map { it.id }
                .toSet()

        var insertedCount = 0
        var skippedCount = 0
        localExercises.forEach { exercise ->
            if (exercise.templateId !in localTemplateIds) {
                CloudLogger.warn("SyncDownloader", "Skipping orphaned template exercise ${exercise.id}")
                skippedCount++
                return@forEach
            }
            database.templateExerciseDao().upsertTemplateExercise(exercise)
            insertedCount++
        }
        if (skippedCount > 0) {
            CloudLogger.debug("SyncDownloader", "downloadAndMergeTemplateExercises: Inserted $insertedCount, skipped $skippedCount")
        }
    }

    suspend fun downloadAndMergeTemplateSets(
        userId: String,
        lastSyncTime: Timestamp?,
    ) {
        val remoteSets = firestoreRepository.downloadTemplateSets(userId, lastSyncTime).getOrThrow()
        val localSets = remoteSets.map { SyncConverters.fromFirestoreTemplateSet(it) }

        val templates = database.workoutTemplateDao().getTemplates(userId)
        val localTemplateExerciseIds = mutableSetOf<String>()
        templates.forEach { template ->
            val exercises = database.templateExerciseDao().getExercisesForTemplate(template.id)
            exercises.forEach { localTemplateExerciseIds.add(it.id) }
        }

        var insertedCount = 0
        var skippedCount = 0
        localSets.forEach { set ->
            if (set.templateExerciseId !in localTemplateExerciseIds) {
                CloudLogger.warn("SyncDownloader", "Skipping orphaned template set ${set.id}")
                skippedCount++
                return@forEach
            }
            database.templateSetDao().upsertTemplateSet(set)
            insertedCount++
        }
        if (skippedCount > 0) {
            CloudLogger.debug("SyncDownloader", "downloadAndMergeTemplateSets: Inserted $insertedCount, skipped $skippedCount")
        }
    }

    suspend fun downloadAndMergeProgrammes(userId: String) {
        CloudLogger.debug("SyncDownloader", "downloadAndMergeProgrammes: Starting")
        val remoteProgrammes = firestoreRepository.downloadProgrammes(userId).getOrThrow()
        CloudLogger.debug("SyncDownloader", "downloadAndMergeProgrammes: Downloaded ${remoteProgrammes.size} programmes")
        val localProgrammes = remoteProgrammes.map { SyncConverters.fromFirestoreProgramme(it) }

        localProgrammes.forEach { programme ->
            val existing = database.programmeDao().getProgrammeById(programme.id)
            if (existing == null) {
                CloudLogger.debug("SyncDownloader", "Inserting new programme ${programme.id}")
                database.programmeDao().insertProgramme(programme)
            }
        }
        CloudLogger.debug("SyncDownloader", "downloadAndMergeProgrammes: Completed")
    }

    suspend fun downloadAndMergeProgrammeWeeks(userId: String) {
        CloudLogger.debug("SyncDownloader", "downloadAndMergeProgrammeWeeks: Starting")
        val remoteWeeks = firestoreRepository.downloadProgrammeWeeks(userId).getOrThrow()
        CloudLogger.debug("SyncDownloader", "downloadAndMergeProgrammeWeeks: Downloaded ${remoteWeeks.size} weeks")
        val localWeeks = remoteWeeks.map { SyncConverters.fromFirestoreProgrammeWeek(it) }

        localWeeks.forEach { week ->
            val existing = database.programmeDao().getProgrammeWeekById(week.id)
            if (existing == null) {
                database.programmeDao().insertProgrammeWeek(week)
            }
        }
        CloudLogger.debug("SyncDownloader", "downloadAndMergeProgrammeWeeks: Completed")
    }

    suspend fun downloadAndMergeProgrammeWorkouts(userId: String) {
        val remoteWorkouts = firestoreRepository.downloadProgrammeWorkouts(userId).getOrThrow()
        val localWorkouts = remoteWorkouts.map { SyncConverters.fromFirestoreProgrammeWorkout(it) }

        localWorkouts.forEach { workout ->
            val existing = database.programmeDao().getProgrammeWorkoutById(workout.id)
            if (existing == null) {
                database.programmeDao().insertProgrammeWorkout(workout)
            }
        }
    }

    suspend fun downloadAndMergeProgrammeProgress(userId: String) {
        val remoteProgress = firestoreRepository.downloadProgrammeProgress(userId).getOrThrow()
        val localProgress = remoteProgress.map { SyncConverters.fromFirestoreProgrammeProgress(it) }

        localProgress.forEach { progress ->
            val existing = database.programmeDao().getProgrammeProgressById(progress.id)
            if (existing == null) {
                database.programmeDao().insertProgrammeProgress(progress)
            } else {
                if (progress.currentWeek > existing.currentWeek ||
                    (progress.currentWeek == existing.currentWeek && progress.currentDay > existing.currentDay)
                ) {
                    database.programmeDao().updateProgrammeProgress(progress)
                }
            }
        }
    }

    suspend fun downloadAndMergeUserExerciseMaxes(userId: String) {
        val remoteMaxes = firestoreRepository.downloadUserExerciseMaxes(userId).getOrThrow()
        val localMaxes = remoteMaxes.map { SyncConverters.fromFirestoreUserExerciseMax(it) }

        localMaxes.forEach { max ->
            val existing = database.exerciseMaxTrackingDao().getById(max.id)
            if (existing == null) {
                database.exerciseMaxTrackingDao().insert(max)
            } else {
                if (max.oneRMEstimate > existing.oneRMEstimate) {
                    database.exerciseMaxTrackingDao().update(max)
                }
            }
        }
    }

    suspend fun downloadAndMergePersonalRecords(userId: String) {
        val remoteRecords = firestoreRepository.downloadPersonalRecords(userId).getOrThrow()
        val localRecords = remoteRecords.map { SyncConverters.fromFirestorePersonalRecord(it) }

        localRecords.forEach { record ->
            val existing = database.personalRecordDao().getPersonalRecordById(record.id)
            if (existing == null) {
                database.personalRecordDao().insertPersonalRecord(record)
            } else {
                val shouldUpdate =
                    when (record.recordType) {
                        PRType.WEIGHT -> record.weight > existing.weight
                        PRType.ESTIMATED_1RM -> (record.estimated1RM ?: 0f) > (existing.estimated1RM ?: 0f)
                    }
                if (shouldUpdate) {
                    database.personalRecordDao().updatePersonalRecord(record)
                }
            }
        }
    }

    suspend fun downloadAndMergeUserExerciseUsages(userId: String) {
        val remoteUsages = firestoreRepository.downloadUserExerciseUsages(userId).getOrThrow()
        val localUsages = remoteUsages.map { SyncConverters.fromFirestoreExerciseUsage(it) }

        localUsages.forEach { remote ->
            val existing = database.userExerciseUsageDao().getUsage(userId, remote.exerciseId)
            if (existing == null) {
                database.userExerciseUsageDao().insertUsage(remote)
            } else {
                val mergedUsage =
                    existing.copy(
                        usageCount = maxOf(existing.usageCount, remote.usageCount),
                        lastUsedAt =
                            when {
                                existing.lastUsedAt == null -> remote.lastUsedAt
                                remote.lastUsedAt == null -> existing.lastUsedAt
                                remote.lastUsedAt.isAfter(existing.lastUsedAt) -> remote.lastUsedAt
                                else -> existing.lastUsedAt
                            },
                        personalNotes = remote.personalNotes ?: existing.personalNotes,
                        updatedAt = java.time.LocalDateTime.now(),
                    )
                database.userExerciseUsageDao().updateUsage(mergedUsage)
            }
        }
    }

    suspend fun downloadAndMergeExerciseSwapHistory(userId: String) {
        val remoteSwaps = firestoreRepository.downloadExerciseSwapHistory(userId).getOrThrow()
        val localSwaps = remoteSwaps.map { SyncConverters.fromFirestoreExerciseSwapHistory(it) }

        localSwaps.forEach { swap ->
            val existing =
                database.exerciseSwapHistoryDao().getExistingSwap(
                    userId = swap.userId,
                    originalExerciseId = swap.originalExerciseId,
                    swappedToExerciseId = swap.swappedToExerciseId,
                    workoutId = swap.workoutId,
                )

            if (existing == null) {
                database.exerciseSwapHistoryDao().insertSwapHistory(swap)
            } else {
                database.exerciseSwapHistoryDao().upsertSwapHistory(swap)
            }
        }
    }

    suspend fun downloadAndMergeExercisePerformanceTracking(userId: String) {
        val remoteTracking = firestoreRepository.downloadExercisePerformanceTracking(userId).getOrThrow()
        val localTracking = remoteTracking.map { SyncConverters.fromFirestoreProgrammeExerciseTracking(it) }

        localTracking.forEach { tracking ->
            val existing = database.programmeExerciseTrackingDao().getTrackingById(tracking.id)
            if (existing == null) {
                database.programmeExerciseTrackingDao().insertTracking(tracking)
            }
        }
    }

    suspend fun downloadAndMergeGlobalExerciseProgress(userId: String) {
        val remoteProgress = firestoreRepository.downloadGlobalExerciseProgress(userId).getOrThrow()
        val localProgress = remoteProgress.map { SyncConverters.fromFirestoreGlobalExerciseProgress(it) }

        localProgress.forEach { progress ->
            val existing = database.globalExerciseProgressDao().getProgressById(progress.id)
            if (existing == null) {
                database.globalExerciseProgressDao().insertProgress(progress)
            }
        }
    }

    suspend fun downloadAndMergeTrainingAnalyses(userId: String) {
        val remoteAnalyses = firestoreRepository.downloadTrainingAnalyses(userId).getOrThrow()
        val localAnalyses = remoteAnalyses.map { SyncConverters.fromFirestoreTrainingAnalysis(it) }

        localAnalyses.forEach { analysis ->
            val existing = database.trainingAnalysisDao().getAnalysisById(analysis.id)
            if (existing == null) {
                database.trainingAnalysisDao().insertAnalysis(analysis)
            }
        }
    }

    suspend fun downloadAndMergeParseRequests(userId: String) {
        val remoteRequests = firestoreRepository.downloadParseRequests(userId).getOrThrow()
        val localRequests = remoteRequests.map { SyncConverters.fromFirestoreParseRequest(it) }

        localRequests.forEach { request ->
            val existing = database.parseRequestDao().getParseRequestById(request.id)
            if (existing == null) {
                database.parseRequestDao().insertParseRequest(request)
            }
        }
    }
}
