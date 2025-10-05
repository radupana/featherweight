package com.github.radupana.featherweight.service

import android.content.Context
import android.util.JsonWriter
import com.github.radupana.featherweight.BuildConfig
import com.github.radupana.featherweight.data.ExerciseLogDao
import com.github.radupana.featherweight.data.SetLogDao
import com.github.radupana.featherweight.data.Workout
import com.github.radupana.featherweight.data.WorkoutDao
import com.github.radupana.featherweight.data.WorkoutStatus
import com.github.radupana.featherweight.data.export.ExportOptions
import com.github.radupana.featherweight.data.profile.ExerciseMaxTrackingDao
import com.github.radupana.featherweight.manager.AuthenticationManager
import com.github.radupana.featherweight.manager.WeightUnitManager
import com.github.radupana.featherweight.repository.FeatherweightRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime

class WorkoutExportService(
    private val workoutDao: WorkoutDao,
    private val exerciseLogDao: ExerciseLogDao,
    private val setLogDao: SetLogDao,
    private val oneRMDao: ExerciseMaxTrackingDao,
    private val repository: FeatherweightRepository,
    private val authManager: AuthenticationManager,
    private val weightUnitManager: WeightUnitManager? = null,
) {
    suspend fun exportWorkoutsToFile(
        context: Context,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        exportOptions: ExportOptions,
        onProgress: (current: Int, total: Int) -> Unit,
    ): File =
        withContext(Dispatchers.IO) {
            val tempFile = File(context.cacheDir, "workout_export_${System.currentTimeMillis()}.json")

            val userId = authManager.getCurrentUserId() ?: "local"
            val totalWorkouts = workoutDao.getWorkoutCountInDateRange(userId, startDate, endDate)

            JsonWriter(tempFile.outputStream().bufferedWriter()).use { writer ->
                writer.setIndent("  ") // Pretty print JSON
                writer.beginObject()

                // Write metadata
                writeMetadata(writer, startDate, endDate, exportOptions, totalWorkouts)

                // Write user profile if requested
                if (exportOptions.includeProfile) {
                    writeUserProfile(writer, exportOptions)
                }

                // Stream workouts with pagination
                writer.name("workouts").beginArray()

                var offset = 0
                val pageSize = 50
                var totalProcessed = 0

                while (true) {
                    val workouts =
                        workoutDao.getWorkoutsInDateRangePaged(
                            userId,
                            startDate,
                            endDate,
                            WorkoutStatus.NOT_STARTED,
                            pageSize,
                            offset,
                        )

                    if (workouts.isEmpty()) break

                    for (workout in workouts) {
                        writeWorkout(writer, workout, exportOptions)
                        totalProcessed++
                        onProgress(totalProcessed, totalWorkouts)
                    }

                    offset += pageSize
                }

                writer.endArray()
                writer.endObject()
            }

            tempFile
        }

    private fun writeMetadata(
        writer: JsonWriter,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        exportOptions: ExportOptions,
        totalWorkouts: Int,
    ) {
        writer.name("metadata").beginObject()
        writer.name("exportDate").value(LocalDateTime.now().toString())
        writer.name("startDate").value(startDate.toString())
        writer.name("endDate").value(endDate.toString())
        writer.name("totalWorkouts").value(totalWorkouts)
        writer.name("appVersion").value(BuildConfig.VERSION_NAME)

        // Add weight unit to metadata
        val currentUnit = weightUnitManager?.getCurrentUnit() ?: com.github.radupana.featherweight.model.WeightUnit.KG
        writer.name("weightUnit").value(currentUnit.name)

        writer.name("exportOptions").beginObject()
        writer.name("includeBodyweight").value(exportOptions.includeBodyweight)
        writer.name("includeOneRepMaxes").value(exportOptions.includeOneRepMaxes)
        writer.name("includeNotes").value(exportOptions.includeNotes)
        writer.endObject()

        writer.endObject()
    }

    private suspend fun writeUserProfile(
        writer: JsonWriter,
        exportOptions: ExportOptions,
    ) {
        writer.name("userProfile").beginObject()

        // Write 1RM history if requested
        if (exportOptions.includeOneRepMaxes) {
            writer.name("oneRepMaxHistory").beginArray()

            val userId = authManager.getCurrentUserId() ?: "local"
            val currentMaxes = oneRMDao.getAllCurrentMaxesForExport(userId)

            for (max in currentMaxes) {
                writer.beginObject()
                writer.name("exerciseId").value(max.exerciseId)
                writer.name("exerciseName").value(max.exerciseName)

                // Export weight in current unit
                val exportWeight = weightUnitManager?.convertFromKg(max.oneRMEstimate) ?: max.oneRMEstimate
                writer.name("weight").value(exportWeight)

                writer.name("recordedDate").value(max.oneRMDate.toString())
                writer.endObject()
            }

            writer.endArray()
        }

        // Note: Bodyweight history would go here if we tracked it
        if (exportOptions.includeBodyweight) {
            writer.name("bodyweightHistory").beginArray()
            // Currently not tracked in the app
            writer.endArray()
        }

        writer.endObject()
    }

    private suspend fun writeWorkout(
        writer: JsonWriter,
        workout: Workout,
        exportOptions: ExportOptions,
    ) {
        writer.beginObject()
        writer.name("id").value(workout.id)
        writer.name("date").value(workout.date.toString())

        workout.name?.let { writer.name("name").value(it) }

        // Write programme info if it's a programme workout
        if (workout.isProgrammeWorkout && workout.programmeId != null) {
            writer.name("programmeInfo").beginObject()
            workout.programmeWorkoutName?.let { writer.name("programmeName").value(it) }
            workout.weekNumber?.let { writer.name("weekNumber").value(it) }
            workout.dayNumber?.let { writer.name("dayNumber").value(it) }
            writer.endObject()
        }

        workout.durationSeconds?.let { writer.name("duration").value(it) }
        writer.name("status").value(workout.status.name)

        // Write exercises
        writer.name("exercises").beginArray()
        val exerciseLogs = exerciseLogDao.getExerciseLogsForWorkout(workout.id)

        for (exercise in exerciseLogs) {
            writeExercise(writer, exercise, exportOptions)
        }

        writer.endArray()
        writer.endObject()
    }

    private suspend fun writeExercise(
        writer: JsonWriter,
        exercise: com.github.radupana.featherweight.data.ExerciseLog,
        exportOptions: ExportOptions,
    ) {
        writer.beginObject()

        writer.name("exerciseId").value(exercise.exerciseId)
        // Get exercise name from repository
        val exerciseVariation = repository.getExerciseById(exercise.exerciseId)
        writer.name("exerciseName").value(exerciseVariation?.name ?: "Unknown Exercise")
        writer.name("order").value(exercise.exerciseOrder)

        if (exportOptions.includeNotes && exercise.notes != null) {
            writer.name("notes").value(exercise.notes)
        }

        // Write sets
        writer.name("sets").beginArray()
        val setLogs = setLogDao.getSetLogsForExercise(exercise.id)

        for ((index, set) in setLogs.withIndex()) {
            writeSet(writer, set, index + 1)
        }

        writer.endArray()
        writer.endObject()
    }

    private fun writeSet(
        writer: JsonWriter,
        set: com.github.radupana.featherweight.data.SetLog,
        setNumber: Int,
    ) {
        writer.beginObject()
        writer.name("setNumber").value(setNumber)
        set.targetReps?.let { writer.name("targetReps").value(it) }

        // Export weights in current unit
        set.targetWeight?.let {
            val exportWeight = weightUnitManager?.convertFromKg(it) ?: it
            writer.name("targetWeight").value(exportWeight)
        }
        writer.name("actualReps").value(set.actualReps)

        val actualExportWeight = weightUnitManager?.convertFromKg(set.actualWeight) ?: set.actualWeight
        writer.name("actualWeight").value(actualExportWeight)

        set.actualRpe?.let { writer.name("rpe").value(it) }
        writer.name("completed").value(set.isCompleted)
        writer.endObject()
    }

    suspend fun exportSingleWorkout(
        context: Context,
        workoutId: String,
        exportOptions: ExportOptions,
    ): File =
        withContext(Dispatchers.IO) {
            val workout =
                workoutDao.getWorkoutById(workoutId)
                    ?: throw IllegalArgumentException("Workout not found")

            val fileName =
                buildString {
                    append("workout_")
                    append(workout.date.toLocalDate())
                    workout.name?.let { append("_${it.replace(" ", "_")}") }
                    append("_${System.currentTimeMillis()}.json")
                }

            val tempFile = File(context.cacheDir, fileName)

            JsonWriter(tempFile.outputStream().bufferedWriter()).use { writer ->
                writer.setIndent("  ")
                writer.beginObject()

                writer.name("metadata").beginObject()
                writer.name("exportDate").value(LocalDateTime.now().toString())
                writer.name("exportType").value("single_workout")
                writer.name("appVersion").value(BuildConfig.VERSION_NAME)

                // Add weight unit to metadata
                val currentUnit = weightUnitManager?.getCurrentUnit() ?: com.github.radupana.featherweight.model.WeightUnit.KG
                writer.name("weightUnit").value(currentUnit.name)

                writer.name("exportOptions").beginObject()
                writer.name("includeBodyweight").value(exportOptions.includeBodyweight)
                writer.name("includeOneRepMaxes").value(exportOptions.includeOneRepMaxes)
                writer.name("includeNotes").value(exportOptions.includeNotes)
                writer.endObject()

                writer.endObject()

                if (exportOptions.includeProfile) {
                    writeUserProfile(writer, exportOptions)
                }

                writer.name("workouts").beginArray()
                writeWorkout(writer, workout, exportOptions)
                writer.endArray()

                writer.endObject()
            }

            tempFile
        }

    suspend fun exportProgrammeWorkouts(
        context: Context,
        programmeId: String,
        exportOptions: ExportOptions,
        onProgress: (current: Int, total: Int) -> Unit,
    ): File =
        withContext(Dispatchers.IO) {
            val workouts = workoutDao.getCompletedWorkoutsByProgramme(programmeId)
            require(workouts.isNotEmpty()) { "No completed workouts found for programme" }

            val programmeName = workouts.firstOrNull()?.programmeWorkoutName ?: "programme"
            val fileName =
                buildString {
                    append("programme_")
                    append(programmeName.replace(" ", "_"))
                    append("_${System.currentTimeMillis()}.json")
                }

            val tempFile = File(context.cacheDir, fileName)

            JsonWriter(tempFile.outputStream().bufferedWriter()).use { writer ->
                writer.setIndent("  ")
                writer.beginObject()

                writer.name("metadata").beginObject()
                writer.name("exportDate").value(LocalDateTime.now().toString())
                writer.name("exportType").value("programme")
                writer.name("programmeName").value(programmeName)
                writer.name("totalWorkouts").value(workouts.size)
                writer.name("appVersion").value(BuildConfig.VERSION_NAME)

                // Add weight unit to metadata
                val currentUnit = weightUnitManager?.getCurrentUnit() ?: com.github.radupana.featherweight.model.WeightUnit.KG
                writer.name("weightUnit").value(currentUnit.name)

                writer.name("exportOptions").beginObject()
                writer.name("includeBodyweight").value(exportOptions.includeBodyweight)
                writer.name("includeOneRepMaxes").value(exportOptions.includeOneRepMaxes)
                writer.name("includeNotes").value(exportOptions.includeNotes)
                writer.endObject()

                writer.endObject()

                if (exportOptions.includeProfile) {
                    writeUserProfile(writer, exportOptions)
                }

                writer.name("workouts").beginArray()
                workouts.forEachIndexed { index, workout ->
                    writeWorkout(writer, workout, exportOptions)
                    onProgress(index + 1, workouts.size)
                }
                writer.endArray()

                writer.endObject()
            }

            tempFile
        }
}
