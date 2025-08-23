package com.github.radupana.featherweight.ui.components

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.radupana.featherweight.data.ParseRequest
import com.github.radupana.featherweight.data.ParseStatus
import com.github.radupana.featherweight.data.ParsedProgramme
import com.google.gson.Gson
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Composable
fun ParseRequestCard(
    request: ParseRequest,
    onView: (ParsedProgramme, Long) -> Unit,  // Pass programme and request ID
    onViewRawText: (String) -> Unit, // View what was submitted
    onEditAndRetry: (String) -> Unit, // Edit the text and try again
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(false) }

    // Calculate time elapsed
    val timeElapsed =
        remember(request.createdAt) {
            val minutes = ChronoUnit.MINUTES.between(request.createdAt, LocalDateTime.now())
            when {
                minutes < 1 -> "Just now"
                minutes < 60 -> "$minutes min ago"
                minutes < 1440 -> "${minutes / 60} hours ago"
                else -> "${minutes / 1440} days ago"
            }
        }

    // Parse programme name if completed
    val programmeName =
        remember(request.resultJson) {
            if (request.status == ParseStatus.COMPLETED && !request.resultJson.isNullOrEmpty()) {
                try {
                    val programme = Gson().fromJson(request.resultJson, ParsedProgramme::class.java)
                    programme.name
                } catch (e: Exception) {
                    Log.w("ParseRequestCard", "Failed to parse programme name from result JSON", e)
                    null
                }
            } else {
                null
            }
        }

    // Get status color and icon
    val (statusColor, statusIcon) =
        when (request.status) {
            ParseStatus.PROCESSING -> MaterialTheme.colorScheme.primary to Icons.Filled.AutoAwesome
            ParseStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary to Icons.Filled.CheckCircle
            ParseStatus.FAILED -> MaterialTheme.colorScheme.error to Icons.Filled.Error
            ParseStatus.IMPORTED -> MaterialTheme.colorScheme.primary to Icons.Filled.CheckCircle
        }

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(
                    enabled = request.status == ParseStatus.PROCESSING || request.status == ParseStatus.COMPLETED,
                ) {
                    when (request.status) {
                        ParseStatus.PROCESSING -> isExpanded = !isExpanded
                        ParseStatus.COMPLETED -> {
                            if (!request.resultJson.isNullOrEmpty()) {
                                try {
                                    val programme = Gson().fromJson(request.resultJson, ParsedProgramme::class.java)
                                    onView(programme, request.id)
                                } catch (e: Exception) {
                                    Log.w("ParseRequestCard", "Failed to parse programme from result JSON for view action", e)
                                }
                            }
                        }
                        ParseStatus.FAILED -> {}
                        ParseStatus.IMPORTED -> {} // Already imported, no action
                    }
                },
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (request.status == ParseStatus.PROCESSING) {
                            val infiniteTransition = rememberInfiniteTransition(label = "rotation")
                            val angle by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec =
                                    infiniteRepeatable(
                                        animation = tween(durationMillis = 2000, easing = LinearEasing),
                                    ),
                                label = "rotation",
                            )
                            Icon(
                                imageVector = statusIcon,
                                contentDescription = null,
                                tint = statusColor,
                                modifier =
                                    Modifier
                                        .size(20.dp)
                                        .rotate(angle),
                            )
                        } else {
                            Icon(
                                imageVector = statusIcon,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text =
                                when (request.status) {
                                    ParseStatus.PROCESSING -> "Parsing Programme..."
                                    ParseStatus.COMPLETED -> programmeName ?: "Programme Ready"
                                    ParseStatus.FAILED -> "Parsing Failed"
                                    ParseStatus.IMPORTED -> programmeName ?: "Programme Imported"
                                },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Column {
                        Text(
                            text =
                                when (request.status) {
                                    ParseStatus.PROCESSING -> "Processing your programme text • $timeElapsed"
                                    ParseStatus.COMPLETED -> "Ready to review • $timeElapsed"
                                    ParseStatus.FAILED -> "Failed • $timeElapsed"
                                    ParseStatus.IMPORTED -> "Imported • $timeElapsed"
                                },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        // Show detailed error and suggestions for failed requests
                        if (request.status == ParseStatus.FAILED) {
                            Spacer(modifier = Modifier.height(4.dp))

                            val errorMessage = request.error ?: "Unknown error"
                            val suggestion =
                                when {
                                    errorMessage.contains("too complex") || errorMessage.contains("2 weeks") ->
                                        "💡 Try: Split into 2-week chunks"
                                    errorMessage.contains("format") || errorMessage.contains("simplifying") ->
                                        "💡 Try: Simplify format (Week 1, Monday, Exercise 3x5 @ 80kg)"
                                    errorMessage.contains("Server error") || errorMessage.contains("try again") ->
                                        "💡 Server is busy. Wait a moment and try again"
                                    request.rawText.length > 5000 ->
                                        "💡 Try: Import first 2 weeks, then remaining weeks"
                                    else ->
                                        "💡 Try: Check format and simplify if needed"
                                }

                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )

                            Text(
                                text = suggestion,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }

                // Action buttons
                Row {
                    when (request.status) {
                        ParseStatus.PROCESSING -> {
                            IconButton(
                                onClick = { isExpanded = !isExpanded },
                                modifier = Modifier.size(40.dp),
                            ) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                                )
                            }
                        }
                        ParseStatus.COMPLETED -> {
                            IconButton(
                                onClick = {
                                    if (!request.resultJson.isNullOrEmpty()) {
                                        try {
                                            val programme = Gson().fromJson(request.resultJson, ParsedProgramme::class.java)
                                            onView(programme, request.id)
                                        } catch (e: Exception) {
                                            Log.w("ParseRequestCard", "Failed to parse programme from result JSON in completed action button", e)
                                        }
                                    }
                                },
                                modifier = Modifier.size(40.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Visibility,
                                    contentDescription = "View Programme",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        ParseStatus.FAILED -> {
                            // Show different actions based on failure type
                            Row {
                                // View raw text to understand what was submitted
                                IconButton(
                                    onClick = { onViewRawText(request.rawText) },
                                    modifier = Modifier.size(40.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Visibility,
                                        contentDescription = "View submitted text",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }

                                // Edit and retry with modified text
                                IconButton(
                                    onClick = { onEditAndRetry(request.rawText) },
                                    modifier = Modifier.size(40.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = "Edit and retry",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                        ParseStatus.IMPORTED -> {
                            // Programme already imported, no actions needed
                        }
                    }

                    IconButton(
                        onClick = {
                            // Add immediate feedback before deleting
                            onDelete()
                        },
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete request",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            // Expandable details for processing requests
            AnimatedVisibility(
                visible = isExpanded && request.status == ParseStatus.PROCESSING,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                ) {
                    Text(
                        text = "Programme Text Preview:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = request.rawText.take(200) + if (request.rawText.length > 200) "..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
