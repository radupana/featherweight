package com.github.radupana.featherweight.service

import android.content.Context

class RestTimerNotificationService(
    private val soundProvider: SoundProvider,
    private val vibrationProvider: VibrationProvider,
) {
    companion object {
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
        } catch (e: RuntimeException) {
            // Silently ignore - notification is not critical
        }

        try {
            vibrationProvider.vibratePattern(CELEBRATION_PATTERN)
        } catch (e: RuntimeException) {
            // Silently ignore - notification is not critical
        }
    }
}
