package com.github.radupana.featherweight.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.radupana.featherweight.viewmodel.SyncUiState

@Composable
fun SyncSection(
    syncState: SyncUiState,
    onSyncNow: () -> Unit,
    onRestoreFromCloud: () -> Unit,
    onToggleAutoSync: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SyncHeader(syncState)

            if (!syncState.isAuthenticated) {
                Text(
                    text = "Sign in to enable cloud sync",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                SyncControls(
                    syncState = syncState,
                    onSyncNow = onSyncNow,
                    onRestoreFromCloud = onRestoreFromCloud,
                    onToggleAutoSync = onToggleAutoSync,
                )
            }
        }
    }
}

@Composable
private fun SyncHeader(syncState: SyncUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Cloud Sync",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

        SyncStatusIcon(syncState)
    }
}

@Composable
private fun SyncStatusIcon(syncState: SyncUiState) {
    val (icon, tint) =
        when {
            !syncState.isAuthenticated -> Icons.Default.CloudOff to MaterialTheme.colorScheme.onSurfaceVariant
            syncState.isSyncing -> Icons.Default.CloudSync to MaterialTheme.colorScheme.primary
            syncState.syncError != null -> Icons.Default.Error to MaterialTheme.colorScheme.error
            syncState.lastSyncTime != null -> Icons.Default.CloudDone to MaterialTheme.colorScheme.primary
            else -> Icons.Default.CloudQueue to MaterialTheme.colorScheme.onSurfaceVariant
        }

    Icon(
        imageVector = icon,
        contentDescription = "Sync status",
        tint = tint,
        modifier = Modifier.size(24.dp),
    )
}

@Composable
private fun SyncControls(
    syncState: SyncUiState,
    onSyncNow: () -> Unit,
    onRestoreFromCloud: () -> Unit,
    onToggleAutoSync: (Boolean) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        syncState.lastSyncTime?.let {
            Text(
                text = "Last synced: $it",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        syncState.syncError?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Auto-sync",
                style = MaterialTheme.typography.bodyLarge,
            )
            Switch(
                checked = syncState.autoSyncEnabled,
                onCheckedChange = onToggleAutoSync,
                enabled = !syncState.isSyncing,
            )
        }

        SyncButtons(
            isSyncing = syncState.isSyncing,
            onSyncNow = onSyncNow,
            onRestoreFromCloud = onRestoreFromCloud,
        )

        if (syncState.autoSyncEnabled) {
            Text(
                text = "Data syncs automatically every 6 hours",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SyncButtons(
    isSyncing: Boolean,
    onSyncNow: () -> Unit,
    onRestoreFromCloud: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = onSyncNow,
            enabled = !isSyncing,
            modifier = Modifier.weight(1f),
        ) {
            if (isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    Icons.Default.Sync,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = if (isSyncing) "Syncing..." else "Sync Now",
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        OutlinedButton(
            onClick = onRestoreFromCloud,
            enabled = !isSyncing,
            modifier = Modifier.weight(1f),
        ) {
            Text("Restore")
        }
    }
}
