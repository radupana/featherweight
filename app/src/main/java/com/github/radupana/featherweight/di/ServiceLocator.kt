package com.github.radupana.featherweight.di

import android.content.Context
import com.github.radupana.featherweight.manager.AuthenticationManager
import com.github.radupana.featherweight.manager.AuthenticationManagerImpl
import com.github.radupana.featherweight.manager.WeightUnitManager
import com.github.radupana.featherweight.manager.WeightUnitManagerImpl
import com.github.radupana.featherweight.service.FirebaseAuthService
import com.github.radupana.featherweight.service.FirebaseAuthServiceImpl

object ServiceLocator {
    private var authenticationManager: AuthenticationManager? = null
    private var firebaseAuthService: FirebaseAuthService? = null
    private var weightUnitManager: WeightUnitManager? = null

    fun provideAuthenticationManager(context: Context): AuthenticationManager =
        authenticationManager ?: synchronized(this) {
            authenticationManager ?: AuthenticationManagerImpl(context.applicationContext).also {
                authenticationManager = it
            }
        }

    fun provideFirebaseAuthService(): FirebaseAuthService =
        firebaseAuthService ?: synchronized(this) {
            firebaseAuthService ?: FirebaseAuthServiceImpl().also {
                firebaseAuthService = it
            }
        }

    fun provideWeightUnitManager(context: Context): WeightUnitManager =
        weightUnitManager ?: synchronized(this) {
            weightUnitManager ?: WeightUnitManagerImpl(context.applicationContext).also {
                weightUnitManager = it
            }
        }

    fun reset() {
        authenticationManager = null
        firebaseAuthService = null
        weightUnitManager = null
    }
}
