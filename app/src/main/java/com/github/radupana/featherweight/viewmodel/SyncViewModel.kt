package com.github.radupana.featherweight.viewmodel

import android.content.Context
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
import com.github.radupana.featherweight.sync.worker.SyncWorker
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
        updateAuthenticationState()
        loadAutoSyncPreference()
    }

    private fun updateAuthenticationState() {
        _uiState.value =
            _uiState.value.copy(
                isAuthenticated = authManager.getCurrentUserId() != null,
            )
    }

    private fun loadAutoSyncPreference() {
        val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        val autoSyncEnabled = prefs.getBoolean("auto_sync_enabled", true)
        _uiState.value = _uiState.value.copy(autoSyncEnabled = autoSyncEnabled)

        if (autoSyncEnabled && authManager.getCurrentUserId() != null) {
            schedulePeriodicSync()
        }
    }

    fun syncNow() {
        if (!_uiState.value.isAuthenticated) return

        viewModelScope.launch {
            _uiState.value =
                _uiState.value.copy(
                    isSyncing = true,
                    syncError = null,
                )

            syncManager.syncAll().fold(
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

        val syncRequest =
            PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .addTag("periodic_sync")
                .build()

        WorkManager
            .getInstance(context)
            .enqueueUniquePeriodicWork(
                "periodic_sync",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest,
            )
    }

    private fun cancelPeriodicSync() {
        WorkManager
            .getInstance(context)
            .cancelAllWorkByTag("periodic_sync")
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
            syncNow()
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
}
