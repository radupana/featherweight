package com.github.radupana.featherweight.sync

import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.sync.converters.SyncConverters
import com.github.radupana.featherweight.sync.repository.FirestoreRepository
import com.github.radupana.featherweight.util.CloudLogger

/**
 * Handles uploading local data to Firestore.
 * Extracted from SyncManager to reduce function count.
 */
class SyncUploader(
    private val database: FeatherweightDatabase,
    private val firestoreRepository: FirestoreRepository,
) {
    suspend fun uploadWorkouts(userId: String) {
        val workouts = database.workoutDao().getAllWorkouts(userId)
        val firestoreWorkouts = workouts.map { SyncConverters.toFirestoreWorkout(it) }
        firestoreRepository.uploadWorkouts(userId, firestoreWorkouts).getOrThrow()
    }

    suspend fun uploadExerciseLogs(userId: String) {
        val workouts = database.workoutDao().getAllWorkouts(userId)
        val workoutIds = workouts.map { it.id }
        val exerciseLogs = database.exerciseLogDao().getExerciseLogsForWorkouts(workoutIds)
        val firestoreLogs = exerciseLogs.map { SyncConverters.toFirestoreExerciseLog(it) }
        firestoreRepository.uploadExerciseLogs(userId, firestoreLogs).getOrThrow()
    }

    suspend fun uploadSetLogs(userId: String) {
        val workouts = database.workoutDao().getAllWorkouts(userId)
        val workoutIds = workouts.map { it.id }
        val exerciseLogs = database.exerciseLogDao().getExerciseLogsForWorkouts(workoutIds)
        val exerciseLogIds = exerciseLogs.map { it.id }
        val setLogs = database.setLogDao().getSetLogsForExercises(exerciseLogIds)
        val firestoreLogs = setLogs.map { SyncConverters.toFirestoreSetLog(it) }
        firestoreRepository.uploadSetLogs(userId, firestoreLogs).getOrThrow()
    }

    suspend fun uploadWorkoutTemplates(userId: String) {
        val templates = database.workoutTemplateDao().getTemplates(userId)
        val firestoreTemplates = templates.map { SyncConverters.toFirestoreWorkoutTemplate(it) }
        firestoreRepository.uploadWorkoutTemplates(userId, firestoreTemplates).getOrThrow()
    }

    suspend fun uploadTemplateExercises(userId: String) {
        val templates = database.workoutTemplateDao().getTemplates(userId)
        val templateIds = templates.map { it.id }
        val templateExercises = database.templateExerciseDao().getExercisesForTemplates(templateIds)
        val firestoreExercises = templateExercises.map { SyncConverters.toFirestoreTemplateExercise(it) }
        firestoreRepository.uploadTemplateExercises(userId, firestoreExercises).getOrThrow()
    }

    suspend fun uploadTemplateSets(userId: String) {
        val templates = database.workoutTemplateDao().getTemplates(userId)
        val templateIds = templates.map { it.id }
        val templateExercises = database.templateExerciseDao().getExercisesForTemplates(templateIds)
        val exerciseIds = templateExercises.map { it.id }
        val templateSets = database.templateSetDao().getSetsForTemplateExercises(exerciseIds)
        val firestoreSets = templateSets.map { SyncConverters.toFirestoreTemplateSet(it) }
        firestoreRepository.uploadTemplateSets(userId, firestoreSets).getOrThrow()
    }

    suspend fun uploadProgrammes(userId: String) {
        val programmes = database.programmeDao().getAllProgrammes()
        val userProgrammes = programmes.filter { it.userId == userId }
        val firestoreProgrammes = userProgrammes.map { SyncConverters.toFirestoreProgramme(it) }
        firestoreRepository.uploadProgrammes(userId, firestoreProgrammes).getOrThrow()
    }

    suspend fun uploadProgrammeWeeks(userId: String) {
        val weeks = database.programmeDao().getAllProgrammeWeeks()
        val userWeeks = weeks.filter { it.userId == userId }
        val firestoreWeeks = userWeeks.map { SyncConverters.toFirestoreProgrammeWeek(it) }
        firestoreRepository.uploadProgrammeWeeks(userId, firestoreWeeks).getOrThrow()
    }

    suspend fun uploadProgrammeWorkouts(userId: String) {
        val workouts = database.programmeDao().getAllProgrammeWorkouts()
        val userWorkouts = workouts.filter { it.userId == userId }
        val firestoreWorkouts = userWorkouts.map { SyncConverters.toFirestoreProgrammeWorkout(it) }
        firestoreRepository.uploadProgrammeWorkouts(userId, firestoreWorkouts).getOrThrow()
    }

    suspend fun uploadProgrammeProgress(userId: String) {
        val progress = database.programmeDao().getAllProgrammeProgress()
        val userProgress = progress.filter { it.userId == userId }
        val firestoreProgress = userProgress.map { SyncConverters.toFirestoreProgrammeProgress(it) }
        firestoreRepository.uploadProgrammeProgress(userId, firestoreProgress).getOrThrow()
    }

    suspend fun uploadUserExerciseMaxes(userId: String) {
        val allTracking = database.exerciseMaxTrackingDao().getAllForUser(userId)
        val firestoreMaxes = allTracking.map { SyncConverters.toFirestoreUserExerciseMax(it) }
        firestoreRepository.uploadUserExerciseMaxes(userId, firestoreMaxes).getOrThrow()
    }

    suspend fun uploadPersonalRecords(userId: String) {
        val records = database.personalRecordDao().getAllPersonalRecords()
        val userRecords = records.filter { it.userId == userId }
        val firestoreRecords = userRecords.map { SyncConverters.toFirestorePersonalRecord(it) }
        firestoreRepository.uploadPersonalRecords(userId, firestoreRecords).getOrThrow()
    }

    suspend fun uploadUserExerciseUsages(userId: String) {
        val usages = database.userExerciseUsageDao().getAllUsageForUser(userId)
        val firestoreUsages = usages.map { SyncConverters.toFirestoreExerciseUsage(it) }
        firestoreRepository.uploadUserExerciseUsages(userId, firestoreUsages).getOrThrow()
    }

    suspend fun uploadExerciseSwapHistory(userId: String) {
        val swaps = database.exerciseSwapHistoryDao().getAllSwapHistory()
        val userSwaps = swaps.filter { it.userId == userId }
        val firestoreSwaps = userSwaps.map { SyncConverters.toFirestoreExerciseSwapHistory(it) }
        firestoreRepository.uploadExerciseSwapHistory(userId, firestoreSwaps).getOrThrow()
    }

    suspend fun uploadExercisePerformanceTracking(userId: String) {
        val tracking = database.programmeExerciseTrackingDao().getAllTracking()
        val userTracking = tracking.filter { it.userId == userId }
        val firestoreTracking = userTracking.map { SyncConverters.toFirestoreProgrammeExerciseTracking(it) }
        firestoreRepository.uploadExercisePerformanceTracking(userId, firestoreTracking).getOrThrow()
    }

    suspend fun uploadGlobalExerciseProgress(userId: String) {
        val progress = database.globalExerciseProgressDao().getAllProgress()
        val userProgress = progress.filter { it.userId == userId }
        val firestoreProgress = userProgress.map { SyncConverters.toFirestoreGlobalExerciseProgress(it) }
        firestoreRepository.uploadGlobalExerciseProgress(userId, firestoreProgress).getOrThrow()
    }

    suspend fun uploadTrainingAnalyses(userId: String) {
        val analyses = database.trainingAnalysisDao().getAllAnalyses()
        val userAnalyses = analyses.filter { it.userId == userId }
        val firestoreAnalyses = userAnalyses.map { SyncConverters.toFirestoreTrainingAnalysis(it) }
        firestoreRepository.uploadTrainingAnalyses(userId, firestoreAnalyses).getOrThrow()
    }

    suspend fun uploadParseRequests(userId: String) {
        val requests = database.parseRequestDao().getAllRequestsList()
        val userRequests = requests.filter { it.userId == userId }
        val firestoreRequests = userRequests.map { SyncConverters.toFirestoreParseRequest(it) }
        firestoreRepository.uploadParseRequests(userId, firestoreRequests).getOrThrow()
    }

    suspend fun uploadAllLocalChanges(userId: String) {
        CloudLogger.debug("SyncUploader", "uploadAllLocalChanges: Starting upload for user $userId")

        CloudLogger.debug("SyncUploader", "Uploading workouts...")
        uploadWorkouts(userId)
        CloudLogger.debug("SyncUploader", "Uploading exercise logs...")
        uploadExerciseLogs(userId)
        CloudLogger.debug("SyncUploader", "Uploading set logs...")
        uploadSetLogs(userId)

        CloudLogger.debug("SyncUploader", "Uploading workout templates...")
        uploadWorkoutTemplates(userId)
        CloudLogger.debug("SyncUploader", "Uploading template exercises...")
        uploadTemplateExercises(userId)
        CloudLogger.debug("SyncUploader", "Uploading template sets...")
        uploadTemplateSets(userId)

        uploadProgrammes(userId)
        uploadProgrammeWeeks(userId)
        uploadProgrammeWorkouts(userId)
        uploadProgrammeProgress(userId)

        uploadUserExerciseMaxes(userId)
        uploadPersonalRecords(userId)
        uploadUserExerciseUsages(userId)

        uploadExerciseSwapHistory(userId)
        uploadExercisePerformanceTracking(userId)
        uploadGlobalExerciseProgress(userId)
        uploadTrainingAnalyses(userId)
        uploadParseRequests(userId)
    }

    suspend fun uploadUserData(userId: String) {
        uploadWorkouts(userId)
        uploadExerciseLogs(userId)
        uploadSetLogs(userId)

        uploadProgrammes(userId)
        uploadProgrammeWeeks(userId)
        uploadProgrammeWorkouts(userId)
        uploadProgrammeProgress(userId)

        uploadUserExerciseMaxes(userId)
        uploadPersonalRecords(userId)
        uploadUserExerciseUsages(userId)

        uploadExerciseSwapHistory(userId)
        uploadExercisePerformanceTracking(userId)
        uploadGlobalExerciseProgress(userId)
        uploadTrainingAnalyses(userId)
        uploadParseRequests(userId)
    }
}
