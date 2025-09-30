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
import com.google.gson.JsonSyntaxException
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Composable
fun ParseRequestCard(
    request: ParseRequest,
    onView: (ParsedProgramme, String) -> Unit,
    onViewRawText: (String) -> Unit,
    onEditAndRetry: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(false) }
    var isErrorExpanded by remember { mutableStateOf(false) }

    val timeElapsed = remember(request.createdAt) { formatTimeElapsed(request.createdAt) }
    val programmeName = remember(request.resultJson) { extractProgrammeName(request) }
    val (statusColor, statusIcon) = getStatusColorAndIcon(request.status)

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(
                    enabled = request.status == ParseStatus.PROCESSING || request.status == ParseStatus.COMPLETED || request.status == ParseStatus.FAILED,
                ) {
                    when (request.status) {
                        ParseStatus.PROCESSING -> isExpanded = !isExpanded
                        ParseStatus.COMPLETED -> {
                            if (!request.resultJson.isNullOrEmpty()) {
                                try {
                                    val programme = Gson().fromJson(request.resultJson, ParsedProgramme::class.java)
                                    onView(programme, request.id)
                                } catch (e: JsonSyntaxException) {
                                    Log.w("ParseRequestCard", "Failed to parse programme from result JSON for view action", e)
                                }
                            }
                        }
                        ParseStatus.FAILED -> isErrorExpanded = !isErrorExpanded
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
                    ParseRequestHeader(
                        status = request.status,
                        statusIcon = statusIcon,
                        statusColor = statusColor,
                        programmeName = programmeName,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    ParseRequestDetails(
                        status = request.status,
                        timeElapsed = timeElapsed,
                        error = request.error,
                        isErrorExpanded = isErrorExpanded,
                    )
                }

                ParseRequestActionButtons(
                    status = request.status,
                    isExpanded = isExpanded,
                    onToggleExpanded = { isExpanded = !isExpanded },
                    onView = {
                        parseAndViewProgramme(request.resultJson, onView, request.id)
                    },
                    onViewRawText = { onViewRawText(request.rawText) },
                    onEditAndRetry = { onEditAndRetry(request.rawText) },
                    onDelete = onDelete,
                )
            }

            ExpandableProcessingDetails(
                isExpanded = isExpanded,
                status = request.status,
                rawText = request.rawText,
            )
        }
    }
}

private fun formatTimeElapsed(createdAt: LocalDateTime): String {
    val minutes = ChronoUnit.MINUTES.between(createdAt, LocalDateTime.now())
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 1440 -> "${minutes / 60} hours ago"
        else -> "${minutes / 1440} days ago"
    }
}

private fun extractProgrammeName(request: ParseRequest): String? =
    if (request.status == ParseStatus.COMPLETED && !request.resultJson.isNullOrEmpty()) {
        try {
            val programme = Gson().fromJson(request.resultJson, ParsedProgramme::class.java)
            programme.name
        } catch (e: JsonSyntaxException) {
            Log.w("ParseRequestCard", "Failed to parse programme name from result JSON", e)
            null
        }
    } else {
        null
    }

@Composable
private fun getStatusColorAndIcon(status: ParseStatus): Pair<androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.vector.ImageVector> =
    when (status) {
        ParseStatus.PROCESSING -> MaterialTheme.colorScheme.primary to Icons.Filled.AutoAwesome
        ParseStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary to Icons.Filled.CheckCircle
        ParseStatus.FAILED -> MaterialTheme.colorScheme.error to Icons.Filled.Error
        ParseStatus.IMPORTED -> MaterialTheme.colorScheme.primary to Icons.Filled.CheckCircle
    }

private fun parseAndViewProgramme(
    resultJson: String?,
    onView: (ParsedProgramme, String) -> Unit,
    requestId: String,
) {
    if (!resultJson.isNullOrEmpty()) {
        try {
            val programme = Gson().fromJson(resultJson, ParsedProgramme::class.java)
            onView(programme, requestId)
        } catch (e: JsonSyntaxException) {
            Log.w("ParseRequestCard", "Failed to parse programme from result JSON", e)
        }
    }
}

@Composable
private fun ParseRequestHeader(
    status: ParseStatus,
    statusIcon: androidx.compose.ui.graphics.vector.ImageVector,
    statusColor: androidx.compose.ui.graphics.Color,
    programmeName: String?,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (status == ParseStatus.PROCESSING) {
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
            text = getStatusTitle(status, programmeName),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun getStatusTitle(
    status: ParseStatus,
    programmeName: String?,
): String =
    when (status) {
        ParseStatus.PROCESSING -> "Parsing Programme..."
        ParseStatus.COMPLETED -> programmeName ?: "Programme Ready"
        ParseStatus.FAILED -> "Parsing Failed"
        ParseStatus.IMPORTED -> programmeName ?: "Programme Imported"
    }

@Composable
private fun ParseRequestDetails(
    status: ParseStatus,
    timeElapsed: String,
    error: String?,
    isErrorExpanded: Boolean = false,
) {
    Column {
        Text(
            text = getStatusMessage(status, timeElapsed),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (status == ParseStatus.FAILED) {
            Spacer(modifier = Modifier.height(4.dp))
            FailedRequestDetails(
                error = error,
                isErrorExpanded = isErrorExpanded,
            )
        }
    }
}

private fun getStatusMessage(
    status: ParseStatus,
    timeElapsed: String,
): String =
    when (status) {
        ParseStatus.PROCESSING -> "Processing your programme text • $timeElapsed"
        ParseStatus.COMPLETED -> "Ready to review • $timeElapsed"
        ParseStatus.FAILED -> "Failed • $timeElapsed"
        ParseStatus.IMPORTED -> "Imported • $timeElapsed"
    }

@Composable
private fun FailedRequestDetails(
    error: String?,
    isErrorExpanded: Boolean = false,
) {
    val errorMessage = error ?: "Unknown error"

    Column {
        // Show collapsed or expanded error based on state
        if (isErrorExpanded) {
            // Full error message when expanded
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        } else {
            // Truncated error when collapsed
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // Show indicator if text is truncated
            if (errorMessage.length > 50) {
                Text(
                    text = "Tap to see full error",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Light,
                )
            }
        }
    }
}

@Composable
private fun ParseRequestActionButtons(
    status: ParseStatus,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onView: () -> Unit,
    onViewRawText: () -> Unit,
    onEditAndRetry: () -> Unit,
    onDelete: () -> Unit,
) {
    Row {
        when (status) {
            ParseStatus.PROCESSING -> {
                IconButton(
                    onClick = onToggleExpanded,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                    )
                }
            }
            ParseStatus.COMPLETED -> {
                Row {
                    IconButton(
                        onClick = onView,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Visibility,
                            contentDescription = "View Programme",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }

                    IconButton(
                        onClick = onEditAndRetry,
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
            ParseStatus.FAILED -> {
                Row {
                    IconButton(
                        onClick = onViewRawText,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Visibility,
                            contentDescription = "View submitted text",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }

                    IconButton(
                        onClick = onEditAndRetry,
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
            onClick = onDelete,
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

@Composable
private fun ExpandableProcessingDetails(
    isExpanded: Boolean,
    status: ParseStatus,
    rawText: String,
) {
    AnimatedVisibility(
        visible = isExpanded && status == ParseStatus.PROCESSING,
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
                text = rawText.take(200) + if (rawText.length > 200) "..." else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
