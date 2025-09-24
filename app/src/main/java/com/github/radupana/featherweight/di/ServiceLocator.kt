package com.github.radupana.featherweight.di

import android.content.Context
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.manager.AuthenticationManager
import com.github.radupana.featherweight.manager.AuthenticationManagerImpl
import com.github.radupana.featherweight.manager.WeightUnitManager
import com.github.radupana.featherweight.manager.WeightUnitManagerImpl
import com.github.radupana.featherweight.service.FirebaseAuthService
import com.github.radupana.featherweight.service.FirebaseAuthServiceImpl
import com.github.radupana.featherweight.sync.SyncManager
import com.github.radupana.featherweight.viewmodel.SyncViewModel

object ServiceLocator {
    private var authenticationManager: AuthenticationManager? = null
    private var firebaseAuthService: FirebaseAuthService? = null
    private var weightUnitManager: WeightUnitManager? = null
    private var syncManager: SyncManager? = null
    private var syncViewModel: SyncViewModel? = null

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

    fun getSyncManager(context: Context): SyncManager =
        syncManager ?: synchronized(this) {
            syncManager ?: SyncManager(
                context = context.applicationContext,
                database = FeatherweightDatabase.getDatabase(context.applicationContext),
                authManager = provideAuthenticationManager(context),
            ).also {
                syncManager = it
            }
        }

    fun getSyncViewModel(context: Context): SyncViewModel =
        syncViewModel ?: synchronized(this) {
            syncViewModel ?: SyncViewModel(
                context = context.applicationContext,
                syncManager = getSyncManager(context),
                authManager = provideAuthenticationManager(context),
            ).also {
                syncViewModel = it
            }
        }

    fun getAuthenticationManager(context: Context): AuthenticationManager = provideAuthenticationManager(context)

    fun reset() {
        authenticationManager = null
        firebaseAuthService = null
        weightUnitManager = null
        syncManager = null
        syncViewModel = null
    }
}
