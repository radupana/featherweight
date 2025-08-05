package com.github.radupana.featherweight.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.radupana.featherweight.data.AIProgrammeRequest
import com.github.radupana.featherweight.data.EquipmentAvailability
import com.github.radupana.featherweight.data.ExperienceLevel
import com.github.radupana.featherweight.data.GenerationStatus
import com.github.radupana.featherweight.data.ProgrammeGoal
import com.github.radupana.featherweight.data.SessionDuration
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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

                // Log the payload for debugging
                println("🔍 AIProgrammeRequestCard: Parsing request payload")
                println("📦 Raw payload: $payload")

                RequestSummary(
                    goal =
                        payload["selectedGoal"]?.jsonPrimitive?.content?.let { goalValue ->
                            println("🎯 Goal value from JSON: $goalValue")
                            ProgrammeGoal.values().find { it.name == goalValue }?.displayName
                        } ?: run {
                            println("❌ Goal not found in payload")
                            "Unknown"
                        },
                    experience =
                        payload["selectedExperience"]?.jsonPrimitive?.content?.let { expValue ->
                            println("💪 Experience value from JSON: $expValue")
                            ExperienceLevel.values().find { it.name == expValue }?.displayName
                        } ?: run {
                            println("❌ Experience not found in payload")
                            "Unknown"
                        },
                    frequency =
                        payload["selectedFrequency"]?.jsonPrimitive?.intOrNull?.let { freqValue ->
                            println("📅 Frequency value from JSON: $freqValue")
                            "$freqValue days/week"
                        } ?: run {
                            println("❌ Frequency not found in payload")
                            "Unknown"
                        },
                    duration =
                        payload["selectedDuration"]?.jsonPrimitive?.content?.let { durValue ->
                            println("⏱️ Duration value from JSON: $durValue")
                            SessionDuration.values().find { it.name == durValue }?.displayName
                        } ?: run {
                            println("❌ Duration not found in payload")
                            "Unknown"
                        },
                    equipment =
                        payload["selectedEquipment"]?.jsonPrimitive?.content?.let { equipValue ->
                            println("🏋️ Equipment value from JSON: $equipValue")
                            EquipmentAvailability.values().find { it.name == equipValue }?.displayName
                        } ?: run {
                            println("❌ Equipment not found in payload")
                            "Unknown"
                        },
                    customInstructions = payload["userInput"]?.jsonPrimitive?.content,
                )
            } catch (e: Exception) {
                println("❌ ERROR parsing request payload: ${e.message}")
                e.printStackTrace()
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
