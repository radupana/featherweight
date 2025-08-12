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
import com.github.radupana.featherweight.data.profile.OneRMDao
import com.github.radupana.featherweight.data.profile.ProfileDao
import com.github.radupana.featherweight.repository.FeatherweightRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime

class WorkoutExportService(
    private val workoutDao: WorkoutDao,
    private val exerciseLogDao: ExerciseLogDao,
    private val setLogDao: SetLogDao,
    private val oneRMDao: OneRMDao,
    private val profileDao: ProfileDao,
    private val repository: FeatherweightRepository,
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

            val totalWorkouts = workoutDao.getWorkoutCountInDateRange(startDate, endDate)

            JsonWriter(tempFile.outputStream().bufferedWriter()).use { writer ->
                writer.setIndent("  ") // Pretty print JSON
                writer.beginObject()

                // Write metadata
                writeMetadata(writer, startDate, endDate, exportOptions, totalWorkouts)

                // Write user profile if requested
                if (exportOptions.includeProfile) {
                    writeUserProfile(writer, startDate, endDate, exportOptions)
                }

                // Stream workouts with pagination
                writer.name("workouts").beginArray()

                var offset = 0
                val pageSize = 50
                var totalProcessed = 0

                while (true) {
                    val workouts =
                        workoutDao.getWorkoutsInDateRangePaged(
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

    private suspend fun writeMetadata(
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

        writer.name("exportOptions").beginObject()
        writer.name("includeBodyweight").value(exportOptions.includeBodyweight)
        writer.name("includeOneRepMaxes").value(exportOptions.includeOneRepMaxes)
        writer.name("includeNotes").value(exportOptions.includeNotes)
        writer.endObject()

        writer.endObject()
    }

    private suspend fun writeUserProfile(
        writer: JsonWriter,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        exportOptions: ExportOptions,
    ) {
        writer.name("userProfile").beginObject()

        // Write 1RM history if requested
        if (exportOptions.includeOneRepMaxes) {
            writer.name("oneRepMaxHistory").beginArray()

            // Get current user ID (assuming single user for now)
            val userProfile = profileDao.getUserProfile()
            if (userProfile != null) {
                val currentMaxes = oneRMDao.getAllCurrentMaxesForExport(userProfile.id)

                for (max in currentMaxes) {
                    writer.beginObject()
                    writer.name("exerciseId").value(max.exerciseVariationId)
                    writer.name("exerciseName").value(max.exerciseName)
                    writer.name("weight").value(max.oneRMEstimate)
                    writer.name("recordedDate").value(max.oneRMDate.toString())
                    writer.endObject()
                }
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

        writer.name("exerciseId").value(exercise.exerciseVariationId)
        // Get exercise name from repository
        val exerciseVariation = repository.getExerciseById(exercise.exerciseVariationId)
        writer.name("exerciseName").value(exerciseVariation?.name ?: "Unknown Exercise")
        writer.name("order").value(exercise.exerciseOrder)

        exercise.supersetGroup?.let { writer.name("supersetGroup").value(it) }

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
        set.targetWeight?.let { writer.name("targetWeight").value(it) }
        writer.name("actualReps").value(set.actualReps)
        writer.name("actualWeight").value(set.actualWeight)
        set.actualRpe?.let { writer.name("rpe").value(it) }
        writer.name("completed").value(set.isCompleted)
        writer.endObject()
    }
}
