package com.github.radupana.featherweight.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.github.radupana.featherweight.di.ServiceLocator
import com.github.radupana.featherweight.manager.AuthenticationManager
import com.github.radupana.featherweight.sync.SyncManager
import com.github.radupana.featherweight.sync.SyncState
import com.github.radupana.featherweight.sync.worker.SystemExerciseSyncWorker
import com.github.radupana.featherweight.sync.worker.UserDataSyncWorker
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

data class SyncUiState(
    val isAuthenticated: Boolean = false,
    val isSyncing: Boolean = false,
    val lastSyncTime: String? = null,
    val syncError: String? = null,
    val autoSyncEnabled: Boolean = true,
)

class SyncViewModel(
    private val context: Context,
    private val syncManager: SyncManager = ServiceLocator.getSyncManager(context),
    private val authManager: AuthenticationManager = ServiceLocator.getAuthenticationManager(context),
) : ViewModel() {
    private val _uiState = MutableStateFlow(SyncUiState())
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    init {
        Log.d("SyncViewModel", "init: Initializing SyncViewModel")
        updateAuthenticationState()
        loadAutoSyncPreference()

        // Check if user is already authenticated and trigger initial backup
        if (_uiState.value.isAuthenticated && _uiState.value.autoSyncEnabled) {
            Log.d("SyncViewModel", "init: User already authenticated, triggering initial backup")
            performBackgroundSync()
        }
    }

    private fun updateAuthenticationState() {
        val userId = authManager.getCurrentUserId()
        val wasAuthenticated = _uiState.value.isAuthenticated
        val isNowAuthenticated = userId != null

        Log.i("SyncViewModel", "updateAuthenticationState: userId=$userId, wasAuth=$wasAuthenticated, isAuth=$isNowAuthenticated")

        _uiState.value =
            _uiState.value.copy(
                isAuthenticated = isNowAuthenticated,
            )

        // If user just became authenticated, trigger backup
        if (!wasAuthenticated && isNowAuthenticated && _uiState.value.autoSyncEnabled) {
            Log.i("SyncViewModel", "updateAuthenticationState: User just authenticated, triggering backup")
            performBackgroundSync()
        }
    }

    private fun loadAutoSyncPreference() {
        val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        val autoSyncEnabled = prefs.getBoolean("auto_sync_enabled", true)
        _uiState.value = _uiState.value.copy(autoSyncEnabled = autoSyncEnabled)

        if (autoSyncEnabled && authManager.getCurrentUserId() != null) {
            schedulePeriodicSync()
        }
    }

    // Background sync happens automatically via SyncWorker
    // Users don't need to manually sync anymore
    private fun performBackgroundSync() {
        if (!_uiState.value.isAuthenticated) {
            Log.w("SyncViewModel", "performBackgroundSync: User not authenticated")
            return
        }

        viewModelScope.launch {
            Log.i("SyncViewModel", "performBackgroundSync: Starting automatic backup")
            try {
                // Add 60 second timeout for sync operation
                withTimeout(60_000L) {
                    Log.i("SyncViewModel", "performBackgroundSync: Calling syncManager for full backup")
                    // Perform full sync (both system exercises and user data)
                    syncManager.syncAll().fold(
                        onSuccess = { state ->
                            Log.i("SyncViewModel", "performBackgroundSync: Backup result - $state")
                            when (state) {
                                is SyncState.Success -> {
                                    Log.i("SyncViewModel", "performBackgroundSync: Backup successful")
                                    _uiState.value =
                                        _uiState.value.copy(
                                            lastSyncTime = formatTimestamp(state.timestamp),
                                            syncError = null,
                                        )
                                }
                                is SyncState.Skipped -> {
                                    Log.i("SyncViewModel", "performBackgroundSync: Sync skipped - ${state.reason}")
                                    // Don't update UI for skipped syncs
                                }
                                is SyncState.Error -> {
                                    Log.e("SyncViewModel", "performBackgroundSync: Backup error - ${state.message}")
                                    _uiState.value =
                                        _uiState.value.copy(
                                            syncError = state.message,
                                        )
                                }
                            }
                        },
                        onFailure = { error ->
                            Log.e("SyncViewModel", "performBackgroundSync: Backup failure - ${error.message}", error)
                            _uiState.value =
                                _uiState.value.copy(
                                    syncError = error.message ?: "Unknown error",
                                )
                        },
                    )
                }
            } catch (e: Exception) {
                Log.e("SyncViewModel", "performBackgroundSync: Backup timed out or failed - ${e.message}", e)
                _uiState.value =
                    _uiState.value.copy(
                        syncError =
                            if (e is kotlinx.coroutines.TimeoutCancellationException) {
                                "Backup timed out. Please check your internet connection."
                            } else {
                                e.message ?: "Unknown error"
                            },
                    )
            }
        }
    }

    fun restoreFromCloud() {
        if (!_uiState.value.isAuthenticated) return

        viewModelScope.launch {
            _uiState.value =
                _uiState.value.copy(
                    isSyncing = true,
                    syncError = null,
                )

            syncManager.restoreFromCloud().fold(
                onSuccess = { state ->
                    when (state) {
                        is SyncState.Success -> {
                            _uiState.value =
                                _uiState.value.copy(
                                    isSyncing = false,
                                    lastSyncTime = formatTimestamp(state.timestamp),
                                    syncError = null,
                                )
                        }
                        is SyncState.Skipped -> {
                            _uiState.value =
                                _uiState.value.copy(
                                    isSyncing = false,
                                )
                            // Don't update timestamp for skipped syncs
                        }
                        is SyncState.Error -> {
                            _uiState.value =
                                _uiState.value.copy(
                                    isSyncing = false,
                                    syncError = state.message,
                                )
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.value =
                        _uiState.value.copy(
                            isSyncing = false,
                            syncError = error.message ?: "Unknown error",
                        )
                },
            )
        }
    }

    fun toggleAutoSync(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(autoSyncEnabled = enabled)

        val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("auto_sync_enabled", enabled).apply()

        if (enabled && authManager.getCurrentUserId() != null) {
            schedulePeriodicSync()
        } else {
            cancelPeriodicSync()
        }
    }

    private fun schedulePeriodicSync() {
        val constraints =
            Constraints
                .Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

        // Schedule user data sync more frequently (every 2 hours)
        val userDataSyncRequest =
            PeriodicWorkRequestBuilder<UserDataSyncWorker>(2, TimeUnit.HOURS)
                .setConstraints(constraints)
                .addTag("user_data_sync")
                .build()

        // Schedule system exercise sync less frequently (weekly)
        val systemExerciseSyncRequest =
            PeriodicWorkRequestBuilder<SystemExerciseSyncWorker>(7, TimeUnit.DAYS)
                .setConstraints(constraints)
                .addTag("system_exercise_sync")
                .build()

        val workManager = WorkManager.getInstance(context)

        // Enqueue user data sync
        workManager.enqueueUniquePeriodicWork(
            "user_data_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            userDataSyncRequest,
        )

        // Enqueue system exercise sync
        workManager.enqueueUniquePeriodicWork(
            "system_exercise_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            systemExerciseSyncRequest,
        )
    }

    private fun cancelPeriodicSync() {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag("user_data_sync")
        workManager.cancelAllWorkByTag("system_exercise_sync")
        // Cancel old tag for backwards compatibility
        workManager.cancelAllWorkByTag("periodic_sync")
    }

    private fun formatTimestamp(timestamp: Timestamp): String {
        val instant = timestamp.toDate().toInstant()
        val localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        val now = LocalDateTime.now()

        val duration = Duration.between(localDateTime, now)

        return when {
            duration.toMinutes() < 1 -> "Just now"
            duration.toMinutes() < 60 -> "${duration.toMinutes()} minutes ago"
            duration.toHours() < 24 -> "${duration.toHours()} hours ago"
            else -> localDateTime.format(DateTimeFormatter.ofPattern("MMM d, h:mm a"))
        }
    }

    fun onUserSignedIn() {
        updateAuthenticationState()
        if (_uiState.value.autoSyncEnabled) {
            schedulePeriodicSync()
            performBackgroundSync()
        }
    }

    fun onUserSignedOut() {
        cancelPeriodicSync()
        _uiState.value =
            SyncUiState(
                isAuthenticated = false,
                autoSyncEnabled = _uiState.value.autoSyncEnabled,
            )
    }

    fun startBackgroundSync() {
        _uiState.value =
            _uiState.value.copy(
                isSyncing = true,
                syncError = null,
            )
    }

    fun onSyncCompleted(syncState: SyncState) {
        when (syncState) {
            is SyncState.Success -> {
                _uiState.value =
                    _uiState.value.copy(
                        isSyncing = false,
                        lastSyncTime = formatTimestamp(syncState.timestamp),
                        syncError = null,
                    )
            }
            is SyncState.Skipped -> {
                _uiState.value =
                    _uiState.value.copy(
                        isSyncing = false,
                    )
                // Don't update timestamp for skipped syncs
            }
            is SyncState.Error -> {
                _uiState.value =
                    _uiState.value.copy(
                        isSyncing = false,
                        syncError = syncState.message,
                    )
            }
        }
    }

    fun onSyncFailed(error: String) {
        _uiState.value =
            _uiState.value.copy(
                isSyncing = false,
                syncError = error,
            )
    }
}
