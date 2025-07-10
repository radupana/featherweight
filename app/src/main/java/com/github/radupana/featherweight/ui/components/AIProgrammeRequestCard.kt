package com.github.radupana.featherweight.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.radupana.featherweight.data.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.*

@Composable
fun AIProgrammeRequestCard(
    request: AIProgrammeRequest,
    onPreview: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val padding = if (isCompact) 12.dp else 16.dp
    var isExpanded by remember { mutableStateOf(false) }

    // Calculate time elapsed
    val timeElapsed =
        remember(request.createdAt) {
            val minutes = (System.currentTimeMillis() - request.createdAt) / 60000
            when {
                minutes < 1 -> "Just now"
                minutes < 60 -> "$minutes min ago"
                minutes < 1440 -> "${minutes / 60} hours ago"
                else -> "${minutes / 1440} days ago"
            }
        }

    // Extract programme name if available
    val programmeName =
        remember(request.generatedProgrammeJson) {
            if (request.status == GenerationStatus.COMPLETED && !request.generatedProgrammeJson.isNullOrEmpty()) {
                try {
                    val json = Json { ignoreUnknownKeys = true }
                    val programmeData = json.parseToJsonElement(request.generatedProgrammeJson)
                    programmeData.jsonObject["name"]?.jsonPrimitive?.content
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        }

    // Parse request details
    val requestDetails =
        remember(request.requestPayload) {
            try {
                val json = Json { ignoreUnknownKeys = true }
                val payload = json.parseToJsonElement(request.requestPayload).jsonObject

                RequestSummary(
                    goal =
                        payload["goal"]?.jsonPrimitive?.content?.let { goalValue ->
                            ProgrammeGoal.values().find { it.name == goalValue }?.displayName
                        } ?: "Unknown",
                    experience =
                        payload["experienceLevel"]?.jsonPrimitive?.content?.let { expValue ->
                            ExperienceLevel.values().find { it.name == expValue }?.displayName
                        } ?: "Unknown",
                    frequency =
                        payload["frequency"]?.jsonPrimitive?.content?.let { freqValue ->
                            TrainingFrequency.values().find { it.name == freqValue }?.displayName
                        } ?: "Unknown",
                    duration =
                        payload["sessionDuration"]?.jsonPrimitive?.content?.let { durValue ->
                            SessionDuration.values().find { it.name == durValue }?.displayName
                        } ?: "Unknown",
                    equipment =
                        payload["equipment"]?.jsonPrimitive?.content?.let { equipValue ->
                            EquipmentAvailability.values().find { it.name == equipValue }?.displayName
                        } ?: "Unknown",
                    customInstructions = payload["customInstructions"]?.jsonPrimitive?.content,
                )
            } catch (e: Exception) {
                null
            }
        }

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = if (isCompact) 4.dp else 8.dp)
                .clickable(
                    enabled =
                        request.status == GenerationStatus.PROCESSING ||
                            request.status == GenerationStatus.COMPLETED,
                ) {
                    when (request.status) {
                        GenerationStatus.PROCESSING -> isExpanded = !isExpanded
                        GenerationStatus.COMPLETED -> onPreview()
                        else -> {}
                    }
                },
        colors =
            CardDefaults.cardColors(
                containerColor =
                    when (request.status) {
                        GenerationStatus.PROCESSING -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        GenerationStatus.NEEDS_CLARIFICATION -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                        GenerationStatus.COMPLETED -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                        GenerationStatus.FAILED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    },
            ),
    ) {
        Column {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(padding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Status icon
                    when (request.status) {
                        GenerationStatus.PROCESSING -> {
                            val infiniteTransition = rememberInfiniteTransition()
                            val rotation by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec =
                                    infiniteRepeatable(
                                        animation = tween(1500, easing = LinearEasing),
                                        repeatMode = RepeatMode.Restart,
                                    ),
                            )
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = "Generating",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier =
                                    Modifier
                                        .size(24.dp)
                                        .rotate(rotation),
                            )
                        }
                        GenerationStatus.NEEDS_CLARIFICATION -> {
                            Icon(
                                Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = "Needs Clarification",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        GenerationStatus.COMPLETED -> {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Ready",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        GenerationStatus.FAILED -> {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = "Failed",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = programmeName ?: "AI Programme",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )

                        val statusText =
                            when (request.status) {
                                GenerationStatus.PROCESSING -> "Generating... ($timeElapsed)"
                                GenerationStatus.NEEDS_CLARIFICATION -> "Needs clarification"
                                GenerationStatus.COMPLETED -> "Ready to preview!"
                                GenerationStatus.FAILED -> request.errorMessage ?: "Generation failed"
                            }

                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color =
                                when (request.status) {
                                    GenerationStatus.PROCESSING -> MaterialTheme.colorScheme.onSurfaceVariant
                                    GenerationStatus.NEEDS_CLARIFICATION -> MaterialTheme.colorScheme.tertiary
                                    GenerationStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                                    GenerationStatus.FAILED -> MaterialTheme.colorScheme.error
                                },
                        )

                        if (request.status == GenerationStatus.PROCESSING) {
                            Text(
                                text = "Tap for details ↓",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            )
                        }

                        if (request.attemptCount > 1) {
                            Text(
                                text = "Attempt ${request.attemptCount}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            )
                        }
                    }
                }

                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    when (request.status) {
                        GenerationStatus.PROCESSING -> {
                            // Show expand/collapse icon
                            Icon(
                                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        GenerationStatus.NEEDS_CLARIFICATION -> {
                            IconButton(onClick = onRetry) {
                                Icon(
                                    Icons.Default.QuestionAnswer,
                                    contentDescription = "Clarify",
                                    tint = MaterialTheme.colorScheme.tertiary,
                                )
                            }
                            IconButton(onClick = onDelete) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                        GenerationStatus.COMPLETED -> {
                            IconButton(onClick = onPreview) {
                                Icon(
                                    Icons.Default.Visibility,
                                    contentDescription = "Preview",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        GenerationStatus.FAILED -> {
                            IconButton(onClick = onRetry) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Retry",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                            IconButton(onClick = onDelete) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }

            // Expandable details section
            AnimatedVisibility(
                visible = isExpanded && request.status == GenerationStatus.PROCESSING,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                requestDetails?.let { details ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = padding)
                                .padding(bottom = padding),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                "Your Request:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )

                            RequestDetailRow("Goal", details.goal)
                            RequestDetailRow("Experience", details.experience)
                            RequestDetailRow("Frequency", details.frequency)
                            RequestDetailRow("Duration", details.duration)
                            RequestDetailRow("Equipment", details.equipment)

                            details.customInstructions?.let {
                                if (it.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Custom Instructions:",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Currently: Generating your personalized programme...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestDetailRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "• $label:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )
    }
}

private data class RequestSummary(
    val goal: String,
    val experience: String,
    val frequency: String,
    val duration: String,
    val equipment: String,
    val customInstructions: String?,
)
