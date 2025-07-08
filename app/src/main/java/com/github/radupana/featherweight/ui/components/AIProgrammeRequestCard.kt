package com.github.radupana.featherweight.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.radupana.featherweight.data.AIProgrammeRequest
import com.github.radupana.featherweight.data.GenerationStatus
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AIProgrammeRequestCard(
    request: AIProgrammeRequest,
    onPreview: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val padding = if (isCompact) 12.dp else 16.dp
    
    // Calculate time elapsed
    val timeElapsed = remember(request.createdAt) {
        val minutes = (System.currentTimeMillis() - request.createdAt) / 60000
        when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "$minutes min ago"
            minutes < 1440 -> "${minutes / 60} hours ago"
            else -> "${minutes / 1440} days ago"
        }
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = if (isCompact) 4.dp else 8.dp)
            .clickable(enabled = request.status == GenerationStatus.COMPLETED) { 
                onPreview() 
            },
        colors = CardDefaults.cardColors(
            containerColor = when (request.status) {
                GenerationStatus.PROCESSING -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                GenerationStatus.COMPLETED -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                GenerationStatus.FAILED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status icon
                when (request.status) {
                    GenerationStatus.PROCESSING -> {
                        val infiniteTransition = rememberInfiniteTransition()
                        val rotation by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            )
                        )
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "Generating",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(24.dp)
                                .rotate(rotation)
                        )
                    }
                    GenerationStatus.COMPLETED -> {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Ready",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    GenerationStatus.FAILED -> {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = "Failed",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "AI Programme",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    val statusText = when (request.status) {
                        GenerationStatus.PROCESSING -> "Generating... ($timeElapsed)"
                        GenerationStatus.COMPLETED -> "Ready to preview!"
                        GenerationStatus.FAILED -> request.errorMessage ?: "Generation failed"
                    }
                    
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = when (request.status) {
                            GenerationStatus.PROCESSING -> MaterialTheme.colorScheme.onSurfaceVariant
                            GenerationStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                            GenerationStatus.FAILED -> MaterialTheme.colorScheme.error
                        }
                    )
                    
                    if (request.attemptCount > 1) {
                        Text(
                            text = "Attempt ${request.attemptCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                when (request.status) {
                    GenerationStatus.PROCESSING -> {
                        // Show loading indicator, no actions
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    GenerationStatus.COMPLETED -> {
                        IconButton(onClick = onPreview) {
                            Icon(
                                Icons.Default.Visibility,
                                contentDescription = "Preview",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    GenerationStatus.FAILED -> {
                        IconButton(onClick = onRetry) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Retry",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = onDelete) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}