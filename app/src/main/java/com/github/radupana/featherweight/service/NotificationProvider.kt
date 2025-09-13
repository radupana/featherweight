package com.github.radupana.featherweight.service

interface SoundProvider {
    fun playNotificationSound()
}

interface VibrationProvider {
    fun vibratePattern(pattern: LongArray)
}

class DefaultSoundProvider(
    private val context: android.content.Context,
) : SoundProvider {
    companion object {
        private const val TAG = "DefaultSoundProvider"
    }

    override fun playNotificationSound() {
        try {
            val notificationUri =
                android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = android.media.RingtoneManager.getRingtone(context, notificationUri)
            ringtone?.play()
        } catch (e: SecurityException) {
            android.util.Log.w(TAG, "Security exception playing sound", e)
        } catch (e: IllegalArgumentException) {
            android.util.Log.w(TAG, "Invalid URI for notification sound", e)
        }
    }
}

class DefaultVibrationProvider(
    private val context: android.content.Context,
) : VibrationProvider {
    companion object {
        private const val TAG = "DefaultVibrationProvider"
    }

    override fun vibratePattern(pattern: LongArray) {
        try {
            val vibrator =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    val vibratorManager =
                        context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                    vibratorManager.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
                }

            if (vibrator.hasVibrator()) {
                val effect = android.os.VibrationEffect.createWaveform(pattern, -1)
                vibrator.vibrate(effect)
            }
        } catch (e: SecurityException) {
            android.util.Log.w(TAG, "Security exception accessing vibrator", e)
        } catch (e: IllegalArgumentException) {
            android.util.Log.w(TAG, "Invalid vibration pattern", e)
        }
    }
}
