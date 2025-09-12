package com.github.radupana.featherweight

import android.app.Application
import android.util.Log
import com.github.radupana.featherweight.service.ConfigServiceFactory

class TestApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.i("TestApplication", "Test application started - Firebase initialization skipped")
        Log.i("TestApplication", "External service calls will be mocked")

        // Set test mode for service factory
        ConfigServiceFactory.isTestMode = true

        // Disable Firebase Performance if it tries to initialize
        try {
            val perfClass = Class.forName("com.google.firebase.perf.FirebasePerformance")
            val getInstanceMethod = perfClass.getMethod("getInstance")
            val instance = getInstanceMethod.invoke(null)
            val setCollectionEnabledMethod = perfClass.getMethod("setPerformanceCollectionEnabled", Boolean::class.java)
            setCollectionEnabledMethod.invoke(instance, false)
            Log.i("TestApplication", "Firebase Performance collection disabled")
        } catch (e: Exception) {
            Log.i("TestApplication", "Firebase Performance not present or already disabled")
        }
    }
}
