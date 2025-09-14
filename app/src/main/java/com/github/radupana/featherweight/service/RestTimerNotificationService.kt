package com.github.radupana.featherweight.service

import android.content.Context
import com.github.radupana.featherweight.util.ExceptionLogger

class RestTimerNotificationService(
    private val soundProvider: SoundProvider,
    private val vibrationProvider: VibrationProvider,
) {
    companion object {
        private const val TAG = "RestTimerNotification"

        // Success celebration pattern: ta-da-da-da (like Duolingo)
        private val CELEBRATION_PATTERN = longArrayOf(0, 100, 80, 60, 50, 60, 50, 60)
    }

    constructor(context: Context) : this(
        DefaultSoundProvider(context),
        DefaultVibrationProvider(context),
    )

    fun notifyTimerCompleted() {
        try {
            soundProvider.playNotificationSound()
        } catch (e: IllegalStateException) {
            ExceptionLogger.logNonCritical(TAG, "Sound provider not available", e)
        } catch (e: SecurityException) {
            ExceptionLogger.logNonCritical(TAG, "Permission denied for sound notification", e)
        }

        try {
            vibrationProvider.vibratePattern(CELEBRATION_PATTERN)
        } catch (e: IllegalStateException) {
            ExceptionLogger.logNonCritical(TAG, "Vibration provider not available", e)
        } catch (e: SecurityException) {
            ExceptionLogger.logNonCritical(TAG, "Permission denied for vibration", e)
        }
    }
}
