package com.github.radupana.featherweight

import android.app.Application
import android.util.Log
import com.github.radupana.featherweight.service.ConfigServiceFactory

class TestApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.i("TestApplication", "Test application started - Firebase initialization skipped")
        Log.i("TestApplication", "External service calls will be mocked")

        ConfigServiceFactory.isTestMode = true
    }
}
