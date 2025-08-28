package com.github.radupana.featherweight.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.github.radupana.featherweight.MainActivity
import com.github.radupana.featherweight.R
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.export.ExportOptions
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.github.radupana.featherweight.service.WorkoutExportService
import java.time.LocalDateTime

class ExportWorkoutsWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    companion object {
        const val CHANNEL_ID = "workout_export_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_CANCEL = "com.github.radupana.featherweight.CANCEL_EXPORT"
    }

    private val database = FeatherweightDatabase.getDatabase(applicationContext)
    private val repository = FeatherweightRepository(applicationContext.applicationContext as android.app.Application)
    private val exportService =
        WorkoutExportService(
            workoutDao = database.workoutDao(),
            exerciseLogDao = database.exerciseLogDao(),
            setLogDao = database.setLogDao(),
            oneRMDao = database.oneRMDao(),
            repository = repository,
        )

    override suspend fun doWork(): Result {
        val startDate =
            inputData.getString("startDate")?.let { LocalDateTime.parse(it) }
                ?: return Result.failure()
        val endDate =
            inputData.getString("endDate")?.let { LocalDateTime.parse(it) }
                ?: return Result.failure()

        val exportOptions =
            ExportOptions(
                includeBodyweight = inputData.getBoolean("includeBodyweight", true),
                includeOneRepMaxes = inputData.getBoolean("includeOneRepMaxes", true),
                includeNotes = inputData.getBoolean("includeNotes", true),
                includeProfile = inputData.getBoolean("includeProfile", true),
            )

        return try {
            // Create notification channel
            createNotificationChannel()

            // Update notification progress
            setForeground(createForegroundInfo(0, 0))

            val file =
                exportService.exportWorkoutsToFile(
                    applicationContext,
                    startDate,
                    endDate,
                    exportOptions,
                ) { current, total ->
                    // Progress callback - we can't update foreground from here
                    // since it's not a suspend function
                }

            // Save file path for retrieval
            val outputData =
                workDataOf(
                    "filePath" to file.absolutePath,
                    "fileSize" to file.length(),
                )

            // Show completion notification
            showCompletionNotification(file)

            Result.success(outputData)
        } catch (e: java.io.IOException) {
            showErrorNotification(e.message ?: "Export failed")
            Result.failure()
        }
    }

    private fun createNotificationChannel() {
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                "Workout Export",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Shows progress while exporting workout data"
            }

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createForegroundInfo(
        current: Int,
        total: Int,
    ): ForegroundInfo {
        val cancelIntent =
            Intent(ACTION_CANCEL).apply {
                putExtra("work_id", id.toString())
            }
        val cancelPendingIntent =
            PendingIntent.getBroadcast(
                applicationContext,
                0,
                cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val notification =
            NotificationCompat
                .Builder(applicationContext, CHANNEL_ID)
                .setContentTitle("Exporting Workouts")
                .setContentText(
                    if (total > 0) {
                        "Progress: $current/$total workouts"
                    } else {
                        "Processing workout $current..."
                    },
                ).setSmallIcon(R.drawable.ic_download)
                .setProgress(total.coerceAtLeast(1), current, total <= 0)
                .setOngoing(true)
                .addAction(R.drawable.ic_cancel, "Cancel", cancelPendingIntent)
                .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun showCompletionNotification(file: java.io.File) {
        val intent =
            Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("exported_file", file.absolutePath)
            }

        val pendingIntent =
            PendingIntent.getActivity(
                applicationContext,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val notification =
            NotificationCompat
                .Builder(applicationContext, CHANNEL_ID)
                .setContentTitle("Export Complete")
                .setContentText("Tap to share or save your workout data")
                .setSmallIcon(R.drawable.ic_check)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    private fun showErrorNotification(message: String) {
        val notification =
            NotificationCompat
                .Builder(applicationContext, CHANNEL_ID)
                .setContentTitle("Export Failed")
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_error)
                .setAutoCancel(true)
                .build()

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID + 2, notification)
    }
}
