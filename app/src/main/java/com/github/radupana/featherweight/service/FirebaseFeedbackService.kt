package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.BuildConfig
import com.github.radupana.featherweight.util.CloudLogger

class FirebaseFeedbackService {
    companion object {
        private const val TAG = "FirebaseFeedbackService"

        private fun isTestBuild(): Boolean = BuildConfig.DEBUG
    }

    fun startFeedback() {
        if (!isTestBuild()) {
            CloudLogger.warn(TAG, "Feedback only available in test builds")
            return
        }

        try {
            val clazz = Class.forName("com.google.firebase.appdistribution.FirebaseAppDistribution")
            val instance = clazz.getMethod("getInstance").invoke(null)

            val method = clazz.getMethod("startFeedback", Int::class.java)
            val textResId = com.github.radupana.featherweight.R.string.feedback_prompt_text
            method.invoke(instance, textResId)
            CloudLogger.debug(TAG, "Feedback started successfully")
        } catch (e: ClassNotFoundException) {
            CloudLogger.warn(TAG, "Firebase App Distribution SDK not available in this build", e)
        } catch (e: ReflectiveOperationException) {
            CloudLogger.error(TAG, "Failed to start feedback", e)
        }
    }
}
