package com.github.radupana.featherweight.manager

interface AuthenticationManager {
    fun isFirstLaunch(): Boolean

    fun setFirstLaunchComplete()

    fun isAuthenticated(): Boolean

    fun getCurrentUserId(): String?

    fun setCurrentUserId(userId: String?)

    fun shouldShowWarning(): Boolean

    fun setWarningShown()

    fun clearUserData()

    fun hasSeenUnauthenticatedWarning(): Boolean

    fun setUnauthenticatedWarningShown()
}
