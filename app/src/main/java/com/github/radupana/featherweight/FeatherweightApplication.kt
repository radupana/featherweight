package com.github.radupana.featherweight

import android.app.Application
import com.github.radupana.featherweight.logging.BugfenderLogger
import com.github.radupana.featherweight.service.RemoteConfigService
import com.google.firebase.FirebaseApp

class FeatherweightApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        FirebaseApp.initializeApp(this)
        
        RemoteConfigService.getInstance().fetchAndActivate {
            val bugfenderKey = RemoteConfigService.getInstance().getBugfenderApiKey()
            if (!bugfenderKey.isNullOrEmpty()) {
                BugfenderLogger.initialize(
                    context = this@FeatherweightApplication, 
                    appKey = bugfenderKey, 
                    isDebug = BuildConfig.DEBUG
                )
                
                BugfenderLogger.i("FeatherweightApp", "Application started - Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                BugfenderLogger.i("FeatherweightApp", "Debug mode: ${BuildConfig.DEBUG}")
                BugfenderLogger.i("FeatherweightApp", "Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
                BugfenderLogger.i("FeatherweightApp", "Android version: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
            }
        }
    }
}