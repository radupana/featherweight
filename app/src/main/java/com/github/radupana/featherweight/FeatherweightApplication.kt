package com.github.radupana.featherweight

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class FeatherweightApplication : Application() {
    private companion object {
        private const val TAG = "FeatherweightApp"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        FirebaseApp.initializeApp(this)
        
        Log.i(TAG, "Application started - Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        Log.i(TAG, "Debug mode: ${BuildConfig.DEBUG}")
        Log.i(TAG, "Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        Log.i(TAG, "Android version: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
    }
}
