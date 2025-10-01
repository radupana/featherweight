package com.github.radupana.featherweight.sync.converters

import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.ExercisePerformanceTracking
import com.github.radupana.featherweight.data.ExerciseSwapHistory
import com.github.radupana.featherweight.data.GlobalExerciseProgress
import com.github.radupana.featherweight.data.PRType
import com.github.radupana.featherweight.data.ParseRequest
import com.github.radupana.featherweight.data.ParseStatus
import com.github.radupana.featherweight.data.PersonalRecord
import com.github.radupana.featherweight.data.ProgressTrend
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.TemplateExercise
import com.github.radupana.featherweight.data.TemplateSet
import com.github.radupana.featherweight.data.TrainingAnalysis
import com.github.radupana.featherweight.data.VolumeTrend
import com.github.radupana.featherweight.data.Workout
import com.github.radupana.featherweight.data.WorkoutStatus
import com.github.radupana.featherweight.data.WorkoutTemplate
import com.github.radupana.featherweight.data.exercise.Equipment
import com.github.radupana.featherweight.data.exercise.ExerciseCategory
import com.github.radupana.featherweight.data.exercise.ExerciseCore
import com.github.radupana.featherweight.data.exercise.ExerciseDifficulty
import com.github.radupana.featherweight.data.exercise.ExerciseVariation
import com.github.radupana.featherweight.data.exercise.InstructionType
import com.github.radupana.featherweight.data.exercise.MovementPattern
import com.github.radupana.featherweight.data.exercise.MuscleGroup
import com.github.radupana.featherweight.data.exercise.RMScalingType
import com.github.radupana.featherweight.data.exercise.VariationAlias
import com.github.radupana.featherweight.data.exercise.VariationInstruction
import com.github.radupana.featherweight.data.exercise.VariationMuscle
import com.github.radupana.featherweight.data.profile.OneRMHistory
import com.github.radupana.featherweight.data.profile.OneRMType
import com.github.radupana.featherweight.data.profile.UserExerciseMax
import com.github.radupana.featherweight.data.programme.Programme
import com.github.radupana.featherweight.data.programme.ProgrammeDifficulty
import com.github.radupana.featherweight.data.programme.ProgrammeProgress
import com.github.radupana.featherweight.data.programme.ProgrammeStatus
import com.github.radupana.featherweight.data.programme.ProgrammeType
import com.github.radupana.featherweight.data.programme.ProgrammeWeek
import com.github.radupana.featherweight.data.programme.ProgrammeWorkout
import com.github.radupana.featherweight.sync.models.FirestoreExerciseCore
import com.github.radupana.featherweight.sync.models.FirestoreExerciseLog
import com.github.radupana.featherweight.sync.models.FirestoreExercisePerformanceTracking
import com.github.radupana.featherweight.sync.models.FirestoreExerciseSwapHistory
import com.github.radupana.featherweight.sync.models.FirestoreExerciseVariation
import com.github.radupana.featherweight.sync.models.FirestoreGlobalExerciseProgress
import com.github.radupana.featherweight.sync.models.FirestoreOneRMHistory
import com.github.radupana.featherweight.sync.models.FirestoreParseRequest
import com.github.radupana.featherweight.sync.models.FirestorePersonalRecord
import com.github.radupana.featherweight.sync.models.FirestoreProgramme
import com.github.radupana.featherweight.sync.models.FirestoreProgrammeProgress
import com.github.radupana.featherweight.sync.models.FirestoreProgrammeWeek
import com.github.radupana.featherweight.sync.models.FirestoreProgrammeWorkout
import com.github.radupana.featherweight.sync.models.FirestoreSetLog
import com.github.radupana.featherweight.sync.models.FirestoreTemplateExercise
import com.github.radupana.featherweight.sync.models.FirestoreTemplateSet
import com.github.radupana.featherweight.sync.models.FirestoreTrainingAnalysis
import com.github.radupana.featherweight.sync.models.FirestoreUserExerciseMax
import com.github.radupana.featherweight.sync.models.FirestoreVariationAlias
import com.github.radupana.featherweight.sync.models.FirestoreVariationInstruction
import com.github.radupana.featherweight.sync.models.FirestoreVariationMuscle
import com.github.radupana.featherweight.sync.models.FirestoreWorkout
import com.github.radupana.featherweight.sync.models.FirestoreWorkoutTemplate
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
            id = firestoreWorkout.localId.ifEmpty { firestoreWorkout.id }, // Use localId, fallback to document ID
            userId = firestoreWorkout.userId.ifEmpty { null },
            name = firestoreWorkout.name,
            notes = firestoreWorkout.notes,
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
            id = firestoreTemplate.localId.ifEmpty { firestoreTemplate.id },
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
            exerciseVariationId = exercise.exerciseVariationId,
            exerciseOrder = exercise.exerciseOrder,
            supersetGroup = exercise.supersetGroup,
            notes = exercise.notes,
        )

    fun fromFirestoreTemplateExercise(firestoreExercise: FirestoreTemplateExercise): TemplateExercise =
        TemplateExercise(
            id = firestoreExercise.localId.ifEmpty { firestoreExercise.id },
            userId = firestoreExercise.userId,
            templateId = firestoreExercise.templateId,
            exerciseVariationId = firestoreExercise.exerciseVariationId,
            exerciseOrder = firestoreExercise.exerciseOrder,
            supersetGroup = firestoreExercise.supersetGroup,
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
            id = firestoreSet.localId.ifEmpty { firestoreSet.id },
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
            exerciseVariationId = exerciseLog.exerciseVariationId,
            exerciseOrder = exerciseLog.exerciseOrder,
            supersetGroup = exerciseLog.supersetGroup,
            notes = exerciseLog.notes,
            originalVariationId = exerciseLog.originalVariationId,
            isSwapped = exerciseLog.isSwapped,
        )

    fun fromFirestoreExerciseLog(firestoreLog: FirestoreExerciseLog): ExerciseLog =
        ExerciseLog(
            id = firestoreLog.localId.ifEmpty { firestoreLog.id },
            userId = null,
            workoutId = firestoreLog.workoutId,
            exerciseVariationId = firestoreLog.exerciseVariationId,
            exerciseOrder = firestoreLog.exerciseOrder,
            supersetGroup = firestoreLog.supersetGroup,
            notes = firestoreLog.notes,
            originalVariationId = firestoreLog.originalVariationId,
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
            suggestedWeight = setLog.suggestedWeight,
            suggestedReps = setLog.suggestedReps,
            suggestionSource = setLog.suggestionSource,
            suggestionConfidence = setLog.suggestionConfidence,
            calculationDetails = setLog.calculationDetails,
            tag = setLog.tag,
            notes = setLog.notes,
            isCompleted = setLog.isCompleted,
            completedAt = setLog.completedAt,
        )

    fun fromFirestoreSetLog(firestoreLog: FirestoreSetLog): SetLog =
        SetLog(
            id = firestoreLog.localId.ifEmpty { firestoreLog.id },
            userId = null,
            exerciseLogId = firestoreLog.exerciseLogId,
            setOrder = firestoreLog.setOrder,
            targetReps = firestoreLog.targetReps,
            targetWeight = firestoreLog.targetWeight,
            targetRpe = firestoreLog.targetRpe,
            actualReps = firestoreLog.actualReps,
            actualWeight = firestoreLog.actualWeight,
            actualRpe = firestoreLog.actualRpe,
            suggestedWeight = firestoreLog.suggestedWeight,
            suggestedReps = firestoreLog.suggestedReps,
            suggestionSource = firestoreLog.suggestionSource,
            suggestionConfidence = firestoreLog.suggestionConfidence,
            calculationDetails = firestoreLog.calculationDetails,
            tag = firestoreLog.tag,
            notes = firestoreLog.notes,
            isCompleted = firestoreLog.isCompleted,
            completedAt = firestoreLog.completedAt,
        )

    fun toFirestoreExerciseCore(exerciseCore: ExerciseCore): FirestoreExerciseCore =
        FirestoreExerciseCore(
            id = exerciseCore.id,
            localId = exerciseCore.id,
            createdByUserId = null, // System exercises don't have creators
            name = exerciseCore.name,
            category = exerciseCore.category.name,
            movementPattern = exerciseCore.movementPattern.name,
            isCompound = exerciseCore.isCompound,
            createdAt = localDateTimeToTimestamp(exerciseCore.createdAt),
            updatedAt = localDateTimeToTimestamp(exerciseCore.updatedAt),
        )

    fun fromFirestoreExerciseCore(firestoreCore: FirestoreExerciseCore): ExerciseCore =
        ExerciseCore(
            id = firestoreCore.id.ifEmpty { firestoreCore.localId },
            name = firestoreCore.name,
            category = ExerciseCategory.valueOf(firestoreCore.category),
            movementPattern = MovementPattern.valueOf(firestoreCore.movementPattern),
            isCompound = firestoreCore.isCompound,
            createdAt = timestampToLocalDateTime(firestoreCore.createdAt),
            updatedAt = timestampToLocalDateTime(firestoreCore.updatedAt),
        )

    fun toFirestoreExerciseVariation(variation: ExerciseVariation): FirestoreExerciseVariation =
        FirestoreExerciseVariation(
            id = variation.id,
            localId = variation.id,
            createdByUserId = null, // System exercises don't have creators
            coreExerciseId = variation.coreExerciseId,
            name = variation.name,
            equipment = variation.equipment.name,
            difficulty = variation.difficulty.name,
            requiresWeight = variation.requiresWeight,
            recommendedRepRange = variation.recommendedRepRange,
            rmScalingType = variation.rmScalingType.name,
            restDurationSeconds = variation.restDurationSeconds,
            usageCount = 0, // Usage tracked separately
            isCustom = false, // System exercises are never custom
            createdAt = localDateTimeToTimestamp(variation.createdAt),
            updatedAt = localDateTimeToTimestamp(variation.updatedAt),
        )

    fun fromFirestoreExerciseVariation(firestoreVariation: FirestoreExerciseVariation): ExerciseVariation =
        ExerciseVariation(
            id = firestoreVariation.id.ifEmpty { firestoreVariation.localId },
            coreExerciseId = firestoreVariation.coreExerciseId,
            name = firestoreVariation.name,
            equipment = Equipment.valueOf(firestoreVariation.equipment),
            difficulty = ExerciseDifficulty.valueOf(firestoreVariation.difficulty),
            requiresWeight = firestoreVariation.requiresWeight,
            recommendedRepRange = firestoreVariation.recommendedRepRange,
            rmScalingType = RMScalingType.valueOf(firestoreVariation.rmScalingType),
            restDurationSeconds = firestoreVariation.restDurationSeconds,
            createdAt = timestampToLocalDateTime(firestoreVariation.createdAt),
            updatedAt = timestampToLocalDateTime(firestoreVariation.updatedAt),
        )

    fun toFirestoreVariationMuscle(muscle: VariationMuscle): FirestoreVariationMuscle =
        FirestoreVariationMuscle(
            variationId = muscle.variationId,
            muscle = muscle.muscle.name,
            isPrimary = muscle.isPrimary,
            emphasisModifier = muscle.emphasisModifier,
        )

    fun fromFirestoreVariationMuscle(firestoreMuscle: FirestoreVariationMuscle): VariationMuscle =
        VariationMuscle(
            variationId = firestoreMuscle.variationId,
            muscle = MuscleGroup.valueOf(firestoreMuscle.muscle),
            isPrimary = firestoreMuscle.isPrimary,
            emphasisModifier = firestoreMuscle.emphasisModifier,
        )

    fun toFirestoreVariationInstruction(instruction: VariationInstruction): FirestoreVariationInstruction =
        FirestoreVariationInstruction(
            id = instruction.id,
            localId = instruction.id,
            variationId = instruction.variationId,
            instructionType = instruction.instructionType.name,
            content = instruction.content,
            orderIndex = instruction.orderIndex,
            languageCode = instruction.languageCode,
        )

    fun fromFirestoreVariationInstruction(firestoreInstruction: FirestoreVariationInstruction): VariationInstruction =
        VariationInstruction(
            id = firestoreInstruction.id.ifEmpty { firestoreInstruction.localId },
            variationId = firestoreInstruction.variationId,
            instructionType = InstructionType.valueOf(firestoreInstruction.instructionType),
            content = firestoreInstruction.content,
            orderIndex = firestoreInstruction.orderIndex,
            languageCode = firestoreInstruction.languageCode,
        )

    fun toFirestoreVariationAlias(alias: VariationAlias): FirestoreVariationAlias =
        FirestoreVariationAlias(
            id = alias.id,
            localId = alias.id,
            variationId = alias.variationId,
            alias = alias.alias,
            confidence = alias.confidence,
            languageCode = alias.languageCode,
            source = alias.source,
        )

    fun fromFirestoreVariationAlias(firestoreAlias: FirestoreVariationAlias): VariationAlias =
        VariationAlias(
            id = firestoreAlias.id.ifEmpty { firestoreAlias.localId },
            variationId = firestoreAlias.variationId,
            alias = firestoreAlias.alias,
            confidence = firestoreAlias.confidence,
            languageCode = firestoreAlias.languageCode,
            source = firestoreAlias.source,
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
            id = firestoreProgramme.id.ifEmpty { firestoreProgramme.localId },
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
            focusAreas = week.focusAreas,
            intensityLevel = week.intensityLevel,
            volumeLevel = week.volumeLevel,
            isDeload = week.isDeload,
            phase = week.phase,
        )

    fun fromFirestoreProgrammeWeek(firestoreWeek: FirestoreProgrammeWeek): ProgrammeWeek =
        ProgrammeWeek(
            id = firestoreWeek.id.ifEmpty { firestoreWeek.localId },
            userId = firestoreWeek.userId,
            programmeId = firestoreWeek.programmeId,
            weekNumber = firestoreWeek.weekNumber,
            name = firestoreWeek.name,
            description = firestoreWeek.description,
            focusAreas = firestoreWeek.focusAreas,
            intensityLevel = firestoreWeek.intensityLevel,
            volumeLevel = firestoreWeek.volumeLevel,
            isDeload = firestoreWeek.isDeload,
            phase = firestoreWeek.phase,
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
            id = firestoreWorkout.id.ifEmpty { firestoreWorkout.localId },
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
            adherencePercentage = progress.adherencePercentage,
            strengthProgress = progress.strengthProgress,
        )

    fun fromFirestoreProgrammeProgress(firestoreProgress: FirestoreProgrammeProgress): ProgrammeProgress =
        ProgrammeProgress(
            id = firestoreProgress.id.ifEmpty { firestoreProgress.localId },
            userId = firestoreProgress.userId,
            programmeId = firestoreProgress.programmeId,
            currentWeek = firestoreProgress.currentWeek,
            currentDay = firestoreProgress.currentDay,
            completedWorkouts = firestoreProgress.completedWorkouts,
            totalWorkouts = firestoreProgress.totalWorkouts,
            lastWorkoutDate = firestoreProgress.lastWorkoutDate?.let { timestampToLocalDateTime(it) },
            adherencePercentage = firestoreProgress.adherencePercentage,
            strengthProgress = firestoreProgress.strengthProgress,
        )

    fun toFirestoreUserExerciseMax(max: UserExerciseMax): FirestoreUserExerciseMax =
        FirestoreUserExerciseMax(
            id = max.id,
            localId = max.id,
            userId = max.userId,
            exerciseVariationId = max.exerciseVariationId,
            isCustomExercise = null, // Deprecated field, kept for backwards compatibility
            mostWeightLifted = max.mostWeightLifted,
            mostWeightReps = max.mostWeightReps,
            mostWeightRpe = max.mostWeightRpe,
            mostWeightDate = localDateTimeToTimestamp(max.mostWeightDate),
            oneRMEstimate = max.oneRMEstimate,
            oneRMContext = max.oneRMContext,
            oneRMConfidence = max.oneRMConfidence,
            oneRMDate = localDateTimeToTimestamp(max.oneRMDate),
            oneRMType = max.oneRMType.name,
            notes = max.notes,
        )

    fun fromFirestoreUserExerciseMax(firestoreMax: FirestoreUserExerciseMax): UserExerciseMax =
        UserExerciseMax(
            id = firestoreMax.id.ifEmpty { firestoreMax.localId },
            userId = firestoreMax.userId,
            exerciseVariationId = firestoreMax.exerciseVariationId,
            mostWeightLifted = firestoreMax.mostWeightLifted,
            mostWeightReps = firestoreMax.mostWeightReps,
            mostWeightRpe = firestoreMax.mostWeightRpe,
            mostWeightDate = timestampToLocalDateTime(firestoreMax.mostWeightDate),
            oneRMEstimate = firestoreMax.oneRMEstimate,
            oneRMContext = firestoreMax.oneRMContext,
            oneRMConfidence = firestoreMax.oneRMConfidence,
            oneRMDate = timestampToLocalDateTime(firestoreMax.oneRMDate),
            oneRMType = OneRMType.valueOf(firestoreMax.oneRMType),
            notes = firestoreMax.notes,
        )

    fun toFirestoreOneRMHistory(history: OneRMHistory): FirestoreOneRMHistory =
        FirestoreOneRMHistory(
            id = history.id,
            localId = history.id,
            userId = history.userId,
            exerciseVariationId = history.exerciseVariationId,
            isCustomExercise = null, // Deprecated field
            oneRMEstimate = history.oneRMEstimate,
            context = history.context,
            recordedAt = localDateTimeToTimestamp(history.recordedAt),
        )

    fun fromFirestoreOneRMHistory(firestoreHistory: FirestoreOneRMHistory): OneRMHistory =
        OneRMHistory(
            id = firestoreHistory.id.ifEmpty { firestoreHistory.localId },
            userId = firestoreHistory.userId,
            exerciseVariationId = firestoreHistory.exerciseVariationId,
            oneRMEstimate = firestoreHistory.oneRMEstimate,
            context = firestoreHistory.context,
            recordedAt = timestampToLocalDateTime(firestoreHistory.recordedAt),
        )

    fun toFirestorePersonalRecord(record: PersonalRecord): FirestorePersonalRecord =
        FirestorePersonalRecord(
            id = record.id,
            localId = record.id,
            userId = record.userId,
            exerciseVariationId = record.exerciseVariationId,
            isCustomExercise = null, // Deprecated field
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
            id = firestoreRecord.id.ifEmpty { firestoreRecord.localId },
            userId = firestoreRecord.userId,
            exerciseVariationId = firestoreRecord.exerciseVariationId,
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
            id = firestoreSwap.id.ifEmpty { firestoreSwap.localId },
            userId = firestoreSwap.userId,
            originalExerciseId = firestoreSwap.originalExerciseId,
            swappedToExerciseId = firestoreSwap.swappedToExerciseId,
            swapDate = timestampToLocalDateTime(firestoreSwap.swapDate),
            workoutId = firestoreSwap.workoutId,
            programmeId = firestoreSwap.programmeId,
        )

    fun toFirestoreExercisePerformanceTracking(tracking: ExercisePerformanceTracking): FirestoreExercisePerformanceTracking =
        FirestoreExercisePerformanceTracking(
            id = tracking.id,
            localId = tracking.id,
            userId = tracking.userId,
            programmeId = tracking.programmeId,
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
            deloadReason = tracking.deloadReason,
            averageRpe = tracking.averageRpe,
            notes = tracking.notes,
        )

    fun fromFirestoreExercisePerformanceTracking(firestoreTracking: FirestoreExercisePerformanceTracking): ExercisePerformanceTracking =
        ExercisePerformanceTracking(
            id = firestoreTracking.id.ifEmpty { firestoreTracking.localId },
            userId = firestoreTracking.userId,
            programmeId = firestoreTracking.programmeId,
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
            deloadReason = firestoreTracking.deloadReason,
            averageRpe = firestoreTracking.averageRpe,
            notes = firestoreTracking.notes,
        )

    fun toFirestoreGlobalExerciseProgress(progress: GlobalExerciseProgress): FirestoreGlobalExerciseProgress =
        FirestoreGlobalExerciseProgress(
            id = progress.id,
            localId = progress.id,
            userId = progress.userId,
            exerciseVariationId = progress.exerciseVariationId,
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
            id = firestoreProgress.id.ifEmpty { firestoreProgress.localId },
            userId = firestoreProgress.userId,
            exerciseVariationId = firestoreProgress.exerciseVariationId,
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
            id = firestoreAnalysis.id.ifEmpty { firestoreAnalysis.localId },
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
            id = firestoreRequest.id.ifEmpty { firestoreRequest.localId },
            userId = firestoreRequest.userId,
            rawText = firestoreRequest.rawText,
            createdAt = timestampToLocalDateTime(firestoreRequest.createdAt),
            status = ParseStatus.valueOf(firestoreRequest.status),
            error = firestoreRequest.error,
            resultJson = firestoreRequest.resultJson,
            completedAt = firestoreRequest.completedAt?.let { timestampToLocalDateTime(it) },
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
