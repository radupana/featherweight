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
                BugfenderLogger.initialize(this@FeatherweightApplication, bugfenderKey, enableLogcat = true)
                BugfenderLogger.i("FeatherweightApp", "Application started")
            }
        }
    }
}