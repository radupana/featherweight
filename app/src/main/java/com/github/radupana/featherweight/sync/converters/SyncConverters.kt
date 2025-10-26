package com.github.radupana.featherweight.sync.converters

import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.ExerciseSwapHistory
import com.github.radupana.featherweight.data.GlobalExerciseProgress
import com.github.radupana.featherweight.data.PRType
import com.github.radupana.featherweight.data.ParseRequest
import com.github.radupana.featherweight.data.ParseStatus
import com.github.radupana.featherweight.data.PersonalRecord
import com.github.radupana.featherweight.data.ProgrammeExerciseTracking
import com.github.radupana.featherweight.data.ProgressTrend
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.TemplateExercise
import com.github.radupana.featherweight.data.TemplateSet
import com.github.radupana.featherweight.data.TrainingAnalysis
import com.github.radupana.featherweight.data.VolumeTrend
import com.github.radupana.featherweight.data.Workout
import com.github.radupana.featherweight.data.WorkoutStatus
import com.github.radupana.featherweight.data.WorkoutTemplate
import com.github.radupana.featherweight.data.exercise.Exercise
import com.github.radupana.featherweight.data.exercise.ExerciseAlias
import com.github.radupana.featherweight.data.exercise.ExerciseInstruction
import com.github.radupana.featherweight.data.exercise.ExerciseMuscle
import com.github.radupana.featherweight.data.profile.ExerciseMaxTracking
import com.github.radupana.featherweight.data.profile.OneRMType
import com.github.radupana.featherweight.data.programme.Programme
import com.github.radupana.featherweight.data.programme.ProgrammeDifficulty
import com.github.radupana.featherweight.data.programme.ProgrammeProgress
import com.github.radupana.featherweight.data.programme.ProgrammeStatus
import com.github.radupana.featherweight.data.programme.ProgrammeType
import com.github.radupana.featherweight.data.programme.ProgrammeWeek
import com.github.radupana.featherweight.data.programme.ProgrammeWorkout
import com.github.radupana.featherweight.sync.models.FirestoreExercise
import com.github.radupana.featherweight.sync.models.FirestoreExerciseLog
import com.github.radupana.featherweight.sync.models.FirestoreExerciseSwapHistory
import com.github.radupana.featherweight.sync.models.FirestoreGlobalExerciseProgress
import com.github.radupana.featherweight.sync.models.FirestoreInstruction
import com.github.radupana.featherweight.sync.models.FirestoreMuscle
import com.github.radupana.featherweight.sync.models.FirestoreParseRequest
import com.github.radupana.featherweight.sync.models.FirestorePersonalRecord
import com.github.radupana.featherweight.sync.models.FirestoreProgramme
import com.github.radupana.featherweight.sync.models.FirestoreProgrammeExerciseTracking
import com.github.radupana.featherweight.sync.models.FirestoreProgrammeProgress
import com.github.radupana.featherweight.sync.models.FirestoreProgrammeWeek
import com.github.radupana.featherweight.sync.models.FirestoreProgrammeWorkout
import com.github.radupana.featherweight.sync.models.FirestoreSetLog
import com.github.radupana.featherweight.sync.models.FirestoreTemplateExercise
import com.github.radupana.featherweight.sync.models.FirestoreTemplateSet
import com.github.radupana.featherweight.sync.models.FirestoreTrainingAnalysis
import com.github.radupana.featherweight.sync.models.FirestoreUserExerciseMax
import com.github.radupana.featherweight.sync.models.FirestoreWorkout
import com.github.radupana.featherweight.sync.models.FirestoreWorkoutTemplate
import com.github.radupana.featherweight.util.IdGenerator
import com.google.firebase.Timestamp
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

@Suppress("TooManyFunctions")
object SyncConverters {
    fun toFirestoreWorkout(workout: Workout): FirestoreWorkout =
        FirestoreWorkout(
            id = workout.id, // Use the same ID for Firestore document
            localId = workout.id, // Keep for backward compatibility
            userId = workout.userId ?: "",
            name = workout.name,
            notes = workout.notes,
            notesUpdatedAt = workout.notesUpdatedAt?.let { localDateTimeToTimestamp(it) },
            date = localDateTimeToTimestamp(workout.date),
            status = workout.status.name,
            programmeId = workout.programmeId,
            weekNumber = workout.weekNumber,
            dayNumber = workout.dayNumber,
            programmeWorkoutName = workout.programmeWorkoutName,
            isProgrammeWorkout = workout.isProgrammeWorkout,
            durationSeconds = workout.durationSeconds?.toLongOrNull(),
            timerStartTime = workout.timerStartTime?.toString(),
            timerElapsedSeconds = workout.timerElapsedSeconds,
        )

    fun fromFirestoreWorkout(firestoreWorkout: FirestoreWorkout): Workout =
        Workout(
            id = firestoreWorkout.localId.ifEmpty { firestoreWorkout.id ?: "" }, // Use localId, fallback to document ID
            userId = firestoreWorkout.userId.ifEmpty { null },
            name = firestoreWorkout.name,
            notes = firestoreWorkout.notes,
            notesUpdatedAt = firestoreWorkout.notesUpdatedAt?.let { timestampToLocalDateTime(it) },
            date = timestampToLocalDateTime(firestoreWorkout.date),
            status = WorkoutStatus.valueOf(firestoreWorkout.status),
            programmeId = firestoreWorkout.programmeId,
            weekNumber = firestoreWorkout.weekNumber,
            dayNumber = firestoreWorkout.dayNumber,
            programmeWorkoutName = firestoreWorkout.programmeWorkoutName,
            isProgrammeWorkout = firestoreWorkout.isProgrammeWorkout,
            durationSeconds = firestoreWorkout.durationSeconds?.toString(),
            timerStartTime = firestoreWorkout.timerStartTime?.let { LocalDateTime.parse(it) },
            timerElapsedSeconds = firestoreWorkout.timerElapsedSeconds,
        )

    fun toFirestoreWorkoutTemplate(template: WorkoutTemplate): FirestoreWorkoutTemplate =
        FirestoreWorkoutTemplate(
            id = template.id,
            localId = template.id,
            userId = template.userId,
            name = template.name,
            description = template.description,
            createdAt = localDateTimeToTimestamp(template.createdAt),
            updatedAt = localDateTimeToTimestamp(template.updatedAt),
        )

    fun fromFirestoreWorkoutTemplate(firestoreTemplate: FirestoreWorkoutTemplate): WorkoutTemplate =
        WorkoutTemplate(
            id = firestoreTemplate.localId.ifEmpty { firestoreTemplate.id ?: "" },
            userId = firestoreTemplate.userId,
            name = firestoreTemplate.name,
            description = firestoreTemplate.description,
            createdAt = timestampToLocalDateTime(firestoreTemplate.createdAt),
            updatedAt = timestampToLocalDateTime(firestoreTemplate.updatedAt),
        )

    fun toFirestoreTemplateExercise(exercise: TemplateExercise): FirestoreTemplateExercise =
        FirestoreTemplateExercise(
            id = exercise.id,
            localId = exercise.id,
            userId = exercise.userId,
            templateId = exercise.templateId,
            exerciseId = exercise.exerciseId,
            exerciseOrder = exercise.exerciseOrder,
            notes = exercise.notes,
        )

    fun fromFirestoreTemplateExercise(firestoreExercise: FirestoreTemplateExercise): TemplateExercise =
        TemplateExercise(
            id = firestoreExercise.localId.ifEmpty { firestoreExercise.id ?: "" },
            userId = firestoreExercise.userId,
            templateId = firestoreExercise.templateId,
            exerciseId = firestoreExercise.exerciseId,
            exerciseOrder = firestoreExercise.exerciseOrder,
            notes = firestoreExercise.notes,
        )

    fun toFirestoreTemplateSet(set: TemplateSet): FirestoreTemplateSet =
        FirestoreTemplateSet(
            id = set.id,
            localId = set.id,
            userId = set.userId,
            templateExerciseId = set.templateExerciseId,
            setOrder = set.setOrder,
            targetReps = set.targetReps,
            targetWeight = set.targetWeight,
            targetRpe = set.targetRpe,
            notes = set.notes,
        )

    fun fromFirestoreTemplateSet(firestoreSet: FirestoreTemplateSet): TemplateSet =
        TemplateSet(
            id = firestoreSet.localId.ifEmpty { firestoreSet.id ?: "" },
            userId = firestoreSet.userId,
            templateExerciseId = firestoreSet.templateExerciseId,
            setOrder = firestoreSet.setOrder,
            targetReps = firestoreSet.targetReps,
            targetWeight = firestoreSet.targetWeight,
            targetRpe = firestoreSet.targetRpe,
            notes = firestoreSet.notes,
        )

    fun toFirestoreExerciseLog(exerciseLog: ExerciseLog): FirestoreExerciseLog =
        FirestoreExerciseLog(
            id = exerciseLog.id,
            localId = exerciseLog.id,
            workoutId = exerciseLog.workoutId,
            exerciseId = exerciseLog.exerciseId,
            exerciseOrder = exerciseLog.exerciseOrder,
            notes = exerciseLog.notes,
            originalExerciseId = exerciseLog.originalExerciseId,
            isSwapped = exerciseLog.isSwapped,
        )

    fun fromFirestoreExerciseLog(firestoreLog: FirestoreExerciseLog): ExerciseLog =
        ExerciseLog(
            id = firestoreLog.localId.ifEmpty { firestoreLog.id ?: "" },
            userId = null,
            workoutId = firestoreLog.workoutId,
            exerciseId = firestoreLog.exerciseId,
            exerciseOrder = firestoreLog.exerciseOrder,
            notes = firestoreLog.notes,
            originalExerciseId = firestoreLog.originalExerciseId,
            isSwapped = firestoreLog.isSwapped,
        )

    fun toFirestoreSetLog(setLog: SetLog): FirestoreSetLog =
        FirestoreSetLog(
            id = setLog.id,
            localId = setLog.id,
            exerciseLogId = setLog.exerciseLogId,
            setOrder = setLog.setOrder,
            targetReps = setLog.targetReps,
            targetWeight = setLog.targetWeight,
            targetRpe = setLog.targetRpe,
            actualReps = setLog.actualReps,
            actualWeight = setLog.actualWeight,
            actualRpe = setLog.actualRpe,
            tag = setLog.tag,
            notes = setLog.notes,
            isCompleted = setLog.isCompleted,
            completedAt = setLog.completedAt,
        )

    fun fromFirestoreSetLog(firestoreLog: FirestoreSetLog): SetLog =
        SetLog(
            id = firestoreLog.localId.ifEmpty { firestoreLog.id ?: "" },
            userId = null,
            exerciseLogId = firestoreLog.exerciseLogId,
            setOrder = firestoreLog.setOrder,
            targetReps = firestoreLog.targetReps,
            targetWeight = firestoreLog.targetWeight,
            targetRpe = firestoreLog.targetRpe,
            actualReps = firestoreLog.actualReps,
            actualWeight = firestoreLog.actualWeight,
            actualRpe = firestoreLog.actualRpe,
            tag = firestoreLog.tag,
            notes = firestoreLog.notes,
            isCompleted = firestoreLog.isCompleted,
            completedAt = firestoreLog.completedAt,
        )

    fun toFirestoreExercise(exercise: Exercise): FirestoreExercise =
        FirestoreExercise(
            // Now using merged entity, so core fields come from the same exercise
            coreName = exercise.name,
            coreCategory = exercise.category ?: "OTHER",
            coreMovementPattern = exercise.movementPattern ?: "OTHER",
            coreIsCompound = exercise.isCompound,
            // Variation fields (now same as core since merged)
            name = exercise.name,
            equipment = exercise.equipment,
            difficulty = exercise.difficulty ?: "BEGINNER",
            requiresWeight = exercise.requiresWeight,
            recommendedRepRange = null,
            rmScalingType = exercise.rmScalingType ?: "STANDARD",
            restDurationSeconds = exercise.restDurationSeconds ?: 90,
            // Empty embedded arrays - these would be populated separately if needed
            muscles = emptyList(),
            aliases = emptyList(),
            instructions = emptyList(),
            // Metadata
            createdAt = exercise.updatedAt?.toString(),
            updatedAt = exercise.updatedAt?.toString(),
        )

    fun toFirestoreExerciseMuscle(muscle: ExerciseMuscle): FirestoreMuscle =
        FirestoreMuscle(
            muscle = muscle.muscle,
            isPrimary = muscle.targetType == "primary",
            emphasisModifier = 1.0, // Default value
        )

    fun fromFirestoreExerciseMuscle(
        firestoreMuscle: FirestoreMuscle,
        exerciseId: String,
    ): ExerciseMuscle =
        ExerciseMuscle(
            exerciseId = exerciseId,
            muscle = firestoreMuscle.muscle,
            targetType = if (firestoreMuscle.isPrimary) "PRIMARY" else "SECONDARY",
        )

    fun toFirestoreExerciseInstruction(instruction: ExerciseInstruction): FirestoreInstruction =
        FirestoreInstruction(
            type = instruction.instructionType,
            content = instruction.instructionText,
            orderIndex = instruction.orderIndex,
            languageCode = "en", // Default language code
        )

    fun fromFirestoreExerciseInstruction(
        firestoreInstruction: FirestoreInstruction,
        exerciseId: String,
    ): ExerciseInstruction =
        ExerciseInstruction(
            id = IdGenerator.generateId(),
            exerciseId = exerciseId,
            instructionType = firestoreInstruction.type,
            instructionText = firestoreInstruction.content,
            orderIndex = firestoreInstruction.orderIndex,
        )

    fun toFirestoreExerciseAlias(alias: ExerciseAlias): String = alias.alias // In the denormalized model, aliases are just strings

    fun fromFirestoreExerciseAlias(
        firestoreAlias: String,
        exerciseId: String,
    ): ExerciseAlias =
        ExerciseAlias(
            id = IdGenerator.generateId(),
            exerciseId = exerciseId,
            alias = firestoreAlias,
        )

    // =====================================================
    // PROGRAMME CONVERTERS
    // =====================================================

    fun toFirestoreProgramme(programme: Programme): FirestoreProgramme =
        FirestoreProgramme(
            id = programme.id,
            localId = programme.id,
            userId = programme.userId,
            name = programme.name,
            description = programme.description,
            durationWeeks = programme.durationWeeks,
            programmeType = programme.programmeType.name,
            difficulty = programme.difficulty.name,
            isCustom = programme.isCustom,
            isActive = programme.isActive,
            status = programme.status.name,
            createdAt = localDateTimeToTimestamp(programme.createdAt),
            startedAt = programme.startedAt?.let { localDateTimeToTimestamp(it) },
            completedAt = programme.completedAt?.let { localDateTimeToTimestamp(it) },
            completionNotes = programme.completionNotes,
            notesCreatedAt = programme.notesCreatedAt?.let { localDateTimeToTimestamp(it) },
            squatMax = programme.squatMax,
            benchMax = programme.benchMax,
            deadliftMax = programme.deadliftMax,
            ohpMax = programme.ohpMax,
            weightCalculationRules = programme.weightCalculationRules,
            progressionRules = programme.progressionRules,
            templateName = programme.templateName,
        )

    fun fromFirestoreProgramme(firestoreProgramme: FirestoreProgramme): Programme =
        Programme(
            id = firestoreProgramme.id?.ifEmpty { firestoreProgramme.localId } ?: firestoreProgramme.localId,
            userId = firestoreProgramme.userId,
            name = firestoreProgramme.name,
            description = firestoreProgramme.description,
            durationWeeks = firestoreProgramme.durationWeeks,
            programmeType = ProgrammeType.valueOf(firestoreProgramme.programmeType),
            difficulty = ProgrammeDifficulty.valueOf(firestoreProgramme.difficulty),
            isCustom = firestoreProgramme.isCustom,
            isActive = firestoreProgramme.isActive,
            status = ProgrammeStatus.valueOf(firestoreProgramme.status),
            createdAt = timestampToLocalDateTime(firestoreProgramme.createdAt),
            startedAt = firestoreProgramme.startedAt?.let { timestampToLocalDateTime(it) },
            completedAt = firestoreProgramme.completedAt?.let { timestampToLocalDateTime(it) },
            completionNotes = firestoreProgramme.completionNotes,
            notesCreatedAt = firestoreProgramme.notesCreatedAt?.let { timestampToLocalDateTime(it) },
            squatMax = firestoreProgramme.squatMax,
            benchMax = firestoreProgramme.benchMax,
            deadliftMax = firestoreProgramme.deadliftMax,
            ohpMax = firestoreProgramme.ohpMax,
            weightCalculationRules = firestoreProgramme.weightCalculationRules,
            progressionRules = firestoreProgramme.progressionRules,
            templateName = firestoreProgramme.templateName,
        )

    fun toFirestoreProgrammeWeek(week: ProgrammeWeek): FirestoreProgrammeWeek =
        FirestoreProgrammeWeek(
            id = week.id,
            localId = week.id,
            userId = week.userId,
            programmeId = week.programmeId,
            weekNumber = week.weekNumber,
            name = week.name,
            description = week.description,
        )

    fun fromFirestoreProgrammeWeek(firestoreWeek: FirestoreProgrammeWeek): ProgrammeWeek =
        ProgrammeWeek(
            id = firestoreWeek.id?.ifEmpty { firestoreWeek.localId } ?: firestoreWeek.localId,
            userId = firestoreWeek.userId,
            programmeId = firestoreWeek.programmeId,
            weekNumber = firestoreWeek.weekNumber,
            name = firestoreWeek.name,
            description = firestoreWeek.description,
        )

    fun toFirestoreProgrammeWorkout(workout: ProgrammeWorkout): FirestoreProgrammeWorkout =
        FirestoreProgrammeWorkout(
            id = workout.id,
            localId = workout.id,
            userId = workout.userId,
            weekId = workout.weekId,
            dayNumber = workout.dayNumber,
            name = workout.name,
            description = workout.description,
            estimatedDuration = workout.estimatedDuration,
            workoutStructure = workout.workoutStructure,
        )

    fun fromFirestoreProgrammeWorkout(firestoreWorkout: FirestoreProgrammeWorkout): ProgrammeWorkout =
        ProgrammeWorkout(
            id = firestoreWorkout.id?.ifEmpty { firestoreWorkout.localId } ?: firestoreWorkout.localId,
            userId = firestoreWorkout.userId,
            weekId = firestoreWorkout.weekId,
            dayNumber = firestoreWorkout.dayNumber,
            name = firestoreWorkout.name,
            description = firestoreWorkout.description,
            estimatedDuration = firestoreWorkout.estimatedDuration,
            workoutStructure = firestoreWorkout.workoutStructure,
        )

    fun toFirestoreProgrammeProgress(progress: ProgrammeProgress): FirestoreProgrammeProgress =
        FirestoreProgrammeProgress(
            id = progress.id,
            localId = progress.id,
            userId = progress.userId,
            programmeId = progress.programmeId,
            currentWeek = progress.currentWeek,
            currentDay = progress.currentDay,
            completedWorkouts = progress.completedWorkouts,
            totalWorkouts = progress.totalWorkouts,
            lastWorkoutDate = progress.lastWorkoutDate?.let { localDateTimeToTimestamp(it) },
        )

    fun fromFirestoreProgrammeProgress(firestoreProgress: FirestoreProgrammeProgress): ProgrammeProgress =
        ProgrammeProgress(
            id = firestoreProgress.id?.ifEmpty { firestoreProgress.localId } ?: firestoreProgress.localId,
            userId = firestoreProgress.userId,
            programmeId = firestoreProgress.programmeId,
            currentWeek = firestoreProgress.currentWeek,
            currentDay = firestoreProgress.currentDay,
            completedWorkouts = firestoreProgress.completedWorkouts,
            totalWorkouts = firestoreProgress.totalWorkouts,
            lastWorkoutDate = firestoreProgress.lastWorkoutDate?.let { timestampToLocalDateTime(it) },
        )

    fun toFirestoreUserExerciseMax(tracking: ExerciseMaxTracking): FirestoreUserExerciseMax =
        FirestoreUserExerciseMax(
            id = tracking.id,
            localId = tracking.id,
            userId = tracking.userId,
            exerciseId = tracking.exerciseId,
            sourceSetId = tracking.sourceSetId,
            mostWeightLifted = tracking.mostWeightLifted,
            mostWeightReps = tracking.mostWeightReps,
            mostWeightRpe = tracking.mostWeightRpe,
            mostWeightDate = localDateTimeToTimestamp(tracking.mostWeightDate),
            oneRMEstimate = tracking.oneRMEstimate,
            oneRMContext = tracking.context,
            oneRMConfidence = tracking.oneRMConfidence,
            oneRMDate = localDateTimeToTimestamp(tracking.recordedAt),
            oneRMType = tracking.oneRMType.name,
            notes = tracking.notes,
        )

    fun fromFirestoreUserExerciseMax(firestoreMax: FirestoreUserExerciseMax): ExerciseMaxTracking =
        ExerciseMaxTracking(
            id = firestoreMax.id?.ifEmpty { firestoreMax.localId } ?: firestoreMax.localId,
            userId = firestoreMax.userId,
            exerciseId = firestoreMax.exerciseId,
            mostWeightLifted = firestoreMax.mostWeightLifted,
            mostWeightReps = firestoreMax.mostWeightReps,
            mostWeightRpe = firestoreMax.mostWeightRpe,
            mostWeightDate = timestampToLocalDateTime(firestoreMax.mostWeightDate),
            oneRMEstimate = firestoreMax.oneRMEstimate,
            context = firestoreMax.oneRMContext,
            oneRMConfidence = firestoreMax.oneRMConfidence,
            recordedAt = timestampToLocalDateTime(firestoreMax.oneRMDate),
            oneRMType = OneRMType.valueOf(firestoreMax.oneRMType),
            notes = firestoreMax.notes,
            sourceSetId = firestoreMax.sourceSetId,
        )

    fun toFirestorePersonalRecord(record: PersonalRecord): FirestorePersonalRecord =
        FirestorePersonalRecord(
            id = record.id,
            localId = record.id,
            userId = record.userId,
            exerciseId = record.exerciseId,
            weight = record.weight,
            reps = record.reps,
            rpe = record.rpe,
            recordDate = localDateTimeToTimestamp(record.recordDate),
            previousWeight = record.previousWeight,
            previousReps = record.previousReps,
            previousDate = record.previousDate?.let { localDateTimeToTimestamp(it) },
            improvementPercentage = record.improvementPercentage,
            recordType = record.recordType.name,
            volume = record.volume,
            estimated1RM = record.estimated1RM,
            notes = record.notes,
            workoutId = record.workoutId,
        )

    fun fromFirestorePersonalRecord(firestoreRecord: FirestorePersonalRecord): PersonalRecord =
        PersonalRecord(
            id = firestoreRecord.id?.ifEmpty { firestoreRecord.localId } ?: firestoreRecord.localId,
            userId = firestoreRecord.userId,
            exerciseId = firestoreRecord.exerciseId,
            weight = firestoreRecord.weight,
            reps = firestoreRecord.reps,
            rpe = firestoreRecord.rpe,
            recordDate = timestampToLocalDateTime(firestoreRecord.recordDate),
            previousWeight = firestoreRecord.previousWeight,
            previousReps = firestoreRecord.previousReps,
            previousDate = firestoreRecord.previousDate?.let { timestampToLocalDateTime(it) },
            improvementPercentage = firestoreRecord.improvementPercentage,
            recordType = PRType.valueOf(firestoreRecord.recordType),
            volume = firestoreRecord.volume,
            estimated1RM = firestoreRecord.estimated1RM,
            notes = firestoreRecord.notes,
            workoutId = firestoreRecord.workoutId,
        )

    fun toFirestoreExerciseSwapHistory(swap: ExerciseSwapHistory): FirestoreExerciseSwapHistory =
        FirestoreExerciseSwapHistory(
            localId = swap.id,
            userId = swap.userId,
            originalExerciseId = swap.originalExerciseId,
            swappedToExerciseId = swap.swappedToExerciseId,
            swapDate = localDateTimeToTimestamp(swap.swapDate),
            workoutId = swap.workoutId,
            programmeId = swap.programmeId,
        )

    fun fromFirestoreExerciseSwapHistory(firestoreSwap: FirestoreExerciseSwapHistory): ExerciseSwapHistory =
        ExerciseSwapHistory(
            id = firestoreSwap.id?.ifEmpty { firestoreSwap.localId } ?: firestoreSwap.localId,
            userId = firestoreSwap.userId,
            originalExerciseId = firestoreSwap.originalExerciseId,
            swappedToExerciseId = firestoreSwap.swappedToExerciseId,
            swapDate = timestampToLocalDateTime(firestoreSwap.swapDate),
            workoutId = firestoreSwap.workoutId,
            programmeId = firestoreSwap.programmeId,
        )

    fun toFirestoreProgrammeExerciseTracking(tracking: ProgrammeExerciseTracking): FirestoreProgrammeExerciseTracking =
        FirestoreProgrammeExerciseTracking(
            id = tracking.id,
            localId = tracking.id,
            userId = tracking.userId,
            programmeId = tracking.programmeId,
            exerciseId = tracking.exerciseId,
            exerciseName = tracking.exerciseName,
            targetWeight = tracking.targetWeight,
            achievedWeight = tracking.achievedWeight,
            targetSets = tracking.targetSets,
            completedSets = tracking.completedSets,
            targetReps = tracking.targetReps,
            achievedReps = tracking.achievedReps,
            missedReps = tracking.missedReps,
            wasSuccessful = tracking.wasSuccessful,
            workoutDate = localDateTimeToTimestamp(tracking.workoutDate),
            workoutId = tracking.workoutId,
            isDeloadWorkout = tracking.isDeloadWorkout,
            averageRpe = tracking.averageRpe,
        )

    fun fromFirestoreProgrammeExerciseTracking(firestoreTracking: FirestoreProgrammeExerciseTracking): ProgrammeExerciseTracking =
        ProgrammeExerciseTracking(
            id = firestoreTracking.id?.ifEmpty { firestoreTracking.localId } ?: firestoreTracking.localId,
            userId = firestoreTracking.userId,
            programmeId = firestoreTracking.programmeId,
            exerciseId = firestoreTracking.exerciseId,
            exerciseName = firestoreTracking.exerciseName,
            targetWeight = firestoreTracking.targetWeight,
            achievedWeight = firestoreTracking.achievedWeight,
            targetSets = firestoreTracking.targetSets,
            completedSets = firestoreTracking.completedSets,
            targetReps = firestoreTracking.targetReps,
            achievedReps = firestoreTracking.achievedReps,
            missedReps = firestoreTracking.missedReps,
            wasSuccessful = firestoreTracking.wasSuccessful,
            workoutDate = timestampToLocalDateTime(firestoreTracking.workoutDate),
            workoutId = firestoreTracking.workoutId,
            isDeloadWorkout = firestoreTracking.isDeloadWorkout,
            averageRpe = firestoreTracking.averageRpe,
        )

    fun toFirestoreGlobalExerciseProgress(progress: GlobalExerciseProgress): FirestoreGlobalExerciseProgress =
        FirestoreGlobalExerciseProgress(
            id = progress.id,
            localId = progress.id,
            userId = progress.userId,
            exerciseId = progress.exerciseId,
            currentWorkingWeight = progress.currentWorkingWeight,
            estimatedMax = progress.estimatedMax,
            lastUpdated = localDateTimeToTimestamp(progress.lastUpdated),
            recentAvgRpe = progress.recentAvgRpe,
            consecutiveStalls = progress.consecutiveStalls,
            lastPrDate = progress.lastPrDate?.let { localDateTimeToTimestamp(it) },
            lastPrWeight = progress.lastPrWeight,
            trend = progress.trend.name,
            volumeTrend = progress.volumeTrend?.name,
            totalVolumeLast30Days = progress.totalVolumeLast30Days,
        )

    fun fromFirestoreGlobalExerciseProgress(firestoreProgress: FirestoreGlobalExerciseProgress): GlobalExerciseProgress =
        GlobalExerciseProgress(
            id = firestoreProgress.id?.ifEmpty { firestoreProgress.localId } ?: firestoreProgress.localId,
            userId = firestoreProgress.userId,
            exerciseId = firestoreProgress.exerciseId,
            currentWorkingWeight = firestoreProgress.currentWorkingWeight,
            estimatedMax = firestoreProgress.estimatedMax,
            lastUpdated = timestampToLocalDateTime(firestoreProgress.lastUpdated),
            recentAvgRpe = firestoreProgress.recentAvgRpe,
            consecutiveStalls = firestoreProgress.consecutiveStalls,
            lastPrDate = firestoreProgress.lastPrDate?.let { timestampToLocalDateTime(it) },
            lastPrWeight = firestoreProgress.lastPrWeight,
            trend = ProgressTrend.valueOf(firestoreProgress.trend),
            volumeTrend = firestoreProgress.volumeTrend?.let { VolumeTrend.valueOf(it) },
            totalVolumeLast30Days = firestoreProgress.totalVolumeLast30Days,
        )

    fun toFirestoreTrainingAnalysis(analysis: TrainingAnalysis): FirestoreTrainingAnalysis =
        FirestoreTrainingAnalysis(
            id = analysis.id,
            localId = analysis.id,
            userId = analysis.userId,
            analysisDate = localDateTimeToTimestamp(analysis.analysisDate),
            periodStart = analysis.periodStart.toString(),
            periodEnd = analysis.periodEnd.toString(),
            overallAssessment = analysis.overallAssessment,
            keyInsightsJson = analysis.keyInsightsJson,
            recommendationsJson = analysis.recommendationsJson,
            warningsJson = analysis.warningsJson,
        )

    fun fromFirestoreTrainingAnalysis(firestoreAnalysis: FirestoreTrainingAnalysis): TrainingAnalysis =
        TrainingAnalysis(
            id = firestoreAnalysis.id?.ifEmpty { firestoreAnalysis.localId } ?: firestoreAnalysis.localId,
            userId = firestoreAnalysis.userId,
            analysisDate = timestampToLocalDateTime(firestoreAnalysis.analysisDate),
            periodStart = java.time.LocalDate.parse(firestoreAnalysis.periodStart),
            periodEnd = java.time.LocalDate.parse(firestoreAnalysis.periodEnd),
            overallAssessment = firestoreAnalysis.overallAssessment,
            keyInsightsJson = firestoreAnalysis.keyInsightsJson,
            recommendationsJson = firestoreAnalysis.recommendationsJson,
            warningsJson = firestoreAnalysis.warningsJson,
        )

    fun toFirestoreParseRequest(request: ParseRequest): FirestoreParseRequest =
        FirestoreParseRequest(
            id = request.id,
            localId = request.id,
            userId = request.userId,
            rawText = request.rawText,
            createdAt = localDateTimeToTimestamp(request.createdAt),
            status = request.status.name,
            error = request.error,
            resultJson = request.resultJson,
            completedAt = request.completedAt?.let { localDateTimeToTimestamp(it) },
        )

    fun fromFirestoreParseRequest(firestoreRequest: FirestoreParseRequest): ParseRequest =
        ParseRequest(
            id = firestoreRequest.id?.ifEmpty { firestoreRequest.localId } ?: firestoreRequest.localId,
            userId = firestoreRequest.userId,
            rawText = firestoreRequest.rawText,
            createdAt = timestampToLocalDateTime(firestoreRequest.createdAt),
            status = ParseStatus.valueOf(firestoreRequest.status),
            error = firestoreRequest.error,
            resultJson = firestoreRequest.resultJson,
            completedAt = firestoreRequest.completedAt?.let { timestampToLocalDateTime(it) },
        )

    fun toFirestoreExerciseUsage(usage: com.github.radupana.featherweight.data.exercise.UserExerciseUsage): com.github.radupana.featherweight.sync.models.FirestoreExerciseUsage =
        com.github.radupana.featherweight.sync.models.FirestoreExerciseUsage(
            id = usage.id,
            localId = usage.id,
            userId = usage.userId,
            exerciseId = usage.exerciseId,
            usageCount = usage.usageCount,
            lastUsedAt = usage.lastUsedAt?.let { localDateTimeToTimestamp(it) },
            personalNotes = usage.personalNotes,
            createdAt = localDateTimeToTimestamp(usage.createdAt),
            updatedAt = localDateTimeToTimestamp(usage.updatedAt),
        )

    fun fromFirestoreExerciseUsage(firestoreUsage: com.github.radupana.featherweight.sync.models.FirestoreExerciseUsage): com.github.radupana.featherweight.data.exercise.UserExerciseUsage =
        com.github.radupana.featherweight.data.exercise.UserExerciseUsage(
            id = firestoreUsage.localId.ifEmpty { firestoreUsage.id ?: "" },
            userId = firestoreUsage.userId,
            exerciseId = firestoreUsage.exerciseId,
            usageCount = firestoreUsage.usageCount,
            lastUsedAt = firestoreUsage.lastUsedAt?.let { timestampToLocalDateTime(it) },
            personalNotes = firestoreUsage.personalNotes,
            createdAt = firestoreUsage.createdAt?.let { timestampToLocalDateTime(it) } ?: LocalDateTime.now(),
            updatedAt = firestoreUsage.updatedAt?.let { timestampToLocalDateTime(it) } ?: LocalDateTime.now(),
        )

    private fun localDateTimeToTimestamp(dateTime: LocalDateTime): Timestamp {
        val instant = dateTime.atZone(ZoneId.systemDefault()).toInstant()
        return Timestamp(Date.from(instant))
    }

    private fun timestampToLocalDateTime(timestamp: Timestamp): LocalDateTime {
        val instant = timestamp.toDate().toInstant()
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
    }
}
