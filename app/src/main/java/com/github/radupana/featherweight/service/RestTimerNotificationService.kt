package com.github.radupana.featherweight.service

import android.content.Context
import android.util.Log
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class RestTimerNotificationService(
    private val context: Context,
) {
    companion object {
        private const val TAG = "RestTimerNotification"
    }
    fun notifyTimerCompleted() {
        Log.d(TAG, "Rest timer completed - triggering notifications")
        playCompletionSound()
        triggerHapticFeedback()
    }

    private fun playCompletionSound() {
        try {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            Log.d(TAG, "Sound URI: $notificationUri")
            val ringtone = RingtoneManager.getRingtone(context, notificationUri)
            if (ringtone != null) {
                ringtone.play()
                Log.d(TAG, "Sound played successfully")
            } else {
                Log.w(TAG, "Ringtone is null - no sound played")
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Sound disabled due to permissions", e)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Invalid notification URI", e)
        }
    }

    private fun triggerHapticFeedback() {
        try {
            val vibrator = getVibrator()
            Log.d(TAG, "Vibrator obtained: ${vibrator.javaClass.simpleName}")
            
            // Check if device supports vibration
            if (!vibrator.hasVibrator()) {
                Log.d(TAG, "Device does not support vibration")
                return
            }
            
            Log.d(TAG, "Device supports vibration - creating effect")
            // Success celebration pattern: ta-da-da-da (like Duolingo)
            val pattern = longArrayOf(0, 100, 80, 60, 50, 60, 50, 60)
            val effect = VibrationEffect.createWaveform(pattern, -1)
            vibrator.vibrate(effect)
            Log.d(TAG, "Vibration triggered successfully")
            
        } catch (e: SecurityException) {
            Log.w(TAG, "Vibration disabled due to permissions", e)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Invalid vibration pattern", e)
        }
    }

    private fun getVibrator(): Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
}
