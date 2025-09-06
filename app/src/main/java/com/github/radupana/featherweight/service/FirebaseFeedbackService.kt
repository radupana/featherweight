package com.github.radupana.featherweight.service

import android.util.Log
import com.github.radupana.featherweight.BuildConfig

class FirebaseFeedbackService {
    companion object {
        private const val TAG = "FirebaseFeedbackService"
        
        private fun isTestBuild(): Boolean {
            return BuildConfig.DEBUG
        }
    }

    fun startFeedback() {
        if (!isTestBuild()) {
            Log.w(TAG, "Feedback only available in test builds")
            return
        }

        try {
            val clazz = Class.forName("com.google.firebase.appdistribution.FirebaseAppDistribution")
            val instance = clazz.getMethod("getInstance").invoke(null)
            
            val method = clazz.getMethod("startFeedback", Int::class.java)
            val textResId = com.github.radupana.featherweight.R.string.feedback_prompt_text
            method.invoke(instance, textResId)
            Log.d(TAG, "Feedback started successfully")
        } catch (e: ClassNotFoundException) {
            Log.w(TAG, "Firebase App Distribution SDK not available in this build", e)
        } catch (e: ReflectiveOperationException) {
            Log.e(TAG, "Failed to start feedback", e)
        }
    }
}
