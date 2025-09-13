package com.github.radupana.featherweight.di

import android.content.Context
import com.github.radupana.featherweight.manager.WeightUnitManager
import com.github.radupana.featherweight.manager.WeightUnitManagerImpl

object ServiceLocator {
    private var weightUnitManager: WeightUnitManager? = null

    fun provideWeightUnitManager(context: Context): WeightUnitManager =
        weightUnitManager ?: synchronized(this) {
            weightUnitManager ?: WeightUnitManagerImpl(context.applicationContext).also {
                weightUnitManager = it
            }
        }

    fun reset() {
        weightUnitManager = null
    }
}
