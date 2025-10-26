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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SyncHeader(syncState)

            if (!syncState.isAuthenticated) {
                Text(
                    text = "Sign in to enable cloud sync",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                SyncStatus(syncState)
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
private fun SyncStatus(syncState: SyncUiState) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Show sync error if present
        syncState.syncError?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        // Show last sync time
        syncState.lastSyncTime?.let {
            Text(
                text = if (syncState.isSyncing) "Syncing..." else "Last updated: $it",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // If syncing, show progress indicator
        if (syncState.isSyncing) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Updating cloud data...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
