package com.github.radupana.featherweight

import android.app.Application
import android.util.Log
import com.github.radupana.featherweight.di.ServiceLocator
import com.github.radupana.featherweight.util.WeightFormatter
import com.google.firebase.FirebaseApp
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace

class FeatherweightApplication : Application() {
    private companion object {
        private const val TAG = "FeatherweightApp"
    }

    private var appStartupTrace: Trace? = null

    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)

        initializeWeightFormatter()

        try {
            appStartupTrace = FirebasePerformance.getInstance().newTrace("app_cold_start")
            appStartupTrace?.start()

            val firebaseInitTrace = FirebasePerformance.getInstance().newTrace("firebase_init")
            firebaseInitTrace.start()
            firebaseInitTrace.stop()
        } catch (e: IllegalStateException) {
            Log.d(TAG, "Firebase Performance not available: ${e.message}")
        }

        Log.i(TAG, "Application started - Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        Log.i(TAG, "Debug mode: ${BuildConfig.DEBUG}")
        Log.i(TAG, "Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        Log.i(TAG, "Android version: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")

        appStartupTrace?.stop()
    }

    private fun initializeWeightFormatter() {
        val weightUnitManager = ServiceLocator.provideWeightUnitManager(this)
        WeightFormatter.initialize(weightUnitManager)
        Log.d(TAG, "WeightFormatter initialized with WeightUnitManager")
    }
}
