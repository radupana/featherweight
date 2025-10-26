package com.github.radupana.featherweight

import android.app.Application
import android.util.Log
import com.github.radupana.featherweight.di.ServiceLocator
import com.github.radupana.featherweight.service.RemoteConfigService
import com.github.radupana.featherweight.util.CloudLogger
import com.github.radupana.featherweight.util.WeightFormatter
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FeatherweightApplication : Application() {
    private companion object {
        private const val TAG = "FeatherweightApp"
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var appStartupTrace: Trace? = null

    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)
        initializeAppCheck()
        initializeAnonymousAuth()

        initializeWeightFormatter()
        initializeCloudLogger()

        try {
            appStartupTrace = FirebasePerformance.getInstance().newTrace("app_cold_start")
            appStartupTrace?.start()

            val firebaseInitTrace = FirebasePerformance.getInstance().newTrace("firebase_init")
            firebaseInitTrace.start()
            firebaseInitTrace.stop()
        } catch (e: IllegalStateException) {
            Log.d(TAG, "Firebase Performance not available: ${e.message}")
        }

        CloudLogger.info(TAG, "Application started - Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        CloudLogger.info(TAG, "Debug mode: ${BuildConfig.DEBUG}")
        CloudLogger.info(TAG, "Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        CloudLogger.info(TAG, "Android version: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")

        appStartupTrace?.stop()
    }

    private fun initializeAppCheck() {
        val appCheck = FirebaseAppCheck.getInstance()
        if (BuildConfig.DEBUG) {
            // Use Debug provider for debug builds and emulators
            appCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance(),
            )
            Log.d(TAG, "App Check initialized with Debug provider")
        } else {
            // Use Play Integrity for release builds
            appCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance(),
            )
            Log.d(TAG, "App Check initialized with Play Integrity provider")
        }
    }

    private fun initializeAnonymousAuth() {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            // Sign in anonymously so unauthenticated users can send logs
            applicationScope.launch {
                try {
                    auth.signInAnonymously().await()
                    Log.i(TAG, "Anonymous authentication successful")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sign in anonymously", e)
                }
            }
        } else {
            Log.d(TAG, "User already authenticated: ${auth.currentUser?.uid}")
        }
    }

    private fun initializeWeightFormatter() {
        val weightUnitManager = ServiceLocator.provideWeightUnitManager(this)
        WeightFormatter.initialize(weightUnitManager)
        Log.d(TAG, "WeightFormatter initialized with WeightUnitManager")
    }

    private fun initializeCloudLogger() {
        val authManager = ServiceLocator.provideAuthenticationManager(this)
        val remoteConfigService = RemoteConfigService.getInstance()
        CloudLogger.initialize(this, authManager, remoteConfigService)
        Log.d(TAG, "CloudLogger initialized")

        applicationScope.launch {
            remoteConfigService.initialize()
        }
    }
}
