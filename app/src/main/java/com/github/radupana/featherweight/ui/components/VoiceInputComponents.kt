package com.github.radupana.featherweight.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.radupana.featherweight.data.voice.ParsedSetData
import com.github.radupana.featherweight.data.voice.VoiceInputState
import com.github.radupana.featherweight.model.WeightUnit
import com.github.radupana.featherweight.service.ExerciseMatchResult
import com.github.radupana.featherweight.service.ExerciseMatchSuggestions
import com.github.radupana.featherweight.viewmodel.ConfirmableExercise

@Composable
fun VoiceInputButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        enabled = enabled,
        colors =
            IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
    ) {
        Icon(
            Icons.Filled.Mic,
            contentDescription = "Voice input",
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
fun VoiceInputOverlay(
    state: VoiceInputState,
    amplitude: Int,
    onStopRecording: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = state !is VoiceInputState.Idle && state !is VoiceInputState.Ready,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable(enabled = false) {},
            contentAlignment = Alignment.Center,
        ) {
            when (state) {
                is VoiceInputState.Preparing -> {
                    ProcessingContent(
                        message = "Preparing...",
                        partialText = null,
                        onCancel = onCancel,
                    )
                }
                is VoiceInputState.Listening -> {
                    ListeningContent(
                        amplitude = amplitude,
                        onStopRecording = onStopRecording,
                        onCancel = onCancel,
                    )
                }
                is VoiceInputState.Transcribing -> {
                    ProcessingContent(
                        message = "Transcribing audio...",
                        partialText = state.partialText,
                        onCancel = onCancel,
                    )
                }
                is VoiceInputState.Parsing -> {
                    ProcessingContent(
                        message = "Parsing workout...",
                        partialText = null, // Don't show transcription - user can't act on it
                        onCancel = onCancel,
                    )
                }
                is VoiceInputState.Error -> {
                    ErrorContent(
                        message = state.message,
                        canRetry = state.canRetry,
                        onRetry = onRetry,
                        onCancel = onCancel,
                    )
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun ListeningContent(
    amplitude: Int,
    onStopRecording: () -> Unit,
    onCancel: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "listening")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(500),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "pulse",
    )

    val amplitudeScale = 1f + (amplitude / 32767f) * 0.3f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            text = "Listening...",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Medium,
        )

        // Pulsing mic circle - tap to stop
        Box(
            modifier =
                Modifier
                    .size(120.dp)
                    .scale(scale * amplitudeScale)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        shape = CircleShape,
                    ).clickable { onStopRecording() },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(80.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Mic,
                    contentDescription = "Tap to stop recording",
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }

        Text(
            text = "Tap the circle or Stop button to finish",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedButton(
                onClick = onCancel,
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White,
                    ),
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Cancel", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cancel")
            }

            Button(
                onClick = onStopRecording,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
            ) {
                Icon(Icons.Filled.Stop, contentDescription = "Stop recording", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Stop")
            }
        }
    }
}

@Composable
private fun ProcessingContent(
    message: String,
    partialText: String?,
    onCancel: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.padding(32.dp),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 4.dp,
        )

        Text(
            text = message,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Medium,
        )

        if (!partialText.isNullOrBlank()) {
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth(0.9f)
                        .padding(horizontal = 16.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.1f),
                    ),
            ) {
                Text(
                    text = "\"$partialText\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(16.dp),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        OutlinedButton(
            onClick = onCancel,
            colors =
                ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White,
                ),
        ) {
            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Cancel")
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(64.dp)
                        .background(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                            shape = CircleShape,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = "Error",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
            }

            Text(
                text = "Something went wrong",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.Medium,
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                OutlinedButton(
                    onClick = onCancel,
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Cancel", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Dismiss")
                }

                if (canRetry) {
                    Button(
                        onClick = onRetry,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Retry", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Try Again")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceInputConfirmationSheet(
    confirmableExercises: List<ConfirmableExercise>,
    transcription: String,
    onExerciseConfirm: (Int, String, String) -> Unit,
    onExerciseSelect: (Int) -> Unit,
    onConfirmAll: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
        ) {
            Text(
                text = "Confirm Exercises",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Text(
                text = "\"$transcription\"",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f, fill = false),
            ) {
                items(confirmableExercises.size) { index ->
                    val exercise = confirmableExercises[index]
                    VoiceParsedExerciseCard(
                        exercise = exercise,
                        onConfirm = { exerciseId, exerciseName ->
                            onExerciseConfirm(index, exerciseId, exerciseName)
                        },
                        onChangeExercise = { onExerciseSelect(index) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val allConfirmed = confirmableExercises.all { it.isConfirmed }

            Button(
                onClick = onConfirmAll,
                enabled = allConfirmed,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "Add exercises",
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (allConfirmed) {
                        "Add ${confirmableExercises.size} Exercise(s)"
                    } else {
                        "Confirm all exercises to continue"
                    },
                )
            }
        }
    }
}

@Composable
fun VoiceParsedExerciseCard(
    exercise: ConfirmableExercise,
    onConfirm: (String, String) -> Unit,
    onChangeExercise: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bestMatch = exercise.matchSuggestions.bestMatch
    val hasAutoMatch = bestMatch?.isAutoMatch == true

    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (exercise.isConfirmed) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
            ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ExerciseCardHeader(exercise = exercise)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatSetsPreview(exercise.parsedData.sets),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            ExerciseCardActions(
                exercise = exercise,
                hasAutoMatch = hasAutoMatch,
                bestMatch = bestMatch,
                onConfirm = onConfirm,
                onChangeExercise = onChangeExercise,
            )
        }
    }
}

@Composable
private fun ExerciseCardHeader(exercise: ConfirmableExercise) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = exercise.selectedExerciseName ?: exercise.parsedData.interpretedName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (exercise.parsedData.spokenName != exercise.parsedData.interpretedName) {
                Text(
                    text = "Heard: \"${exercise.parsedData.spokenName}\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (exercise.isConfirmed) {
            Icon(
                Icons.Filled.Check,
                contentDescription = "Confirmed",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun ExerciseCardActions(
    exercise: ConfirmableExercise,
    hasAutoMatch: Boolean,
    bestMatch: ExerciseMatchResult?,
    onConfirm: (String, String) -> Unit,
    onChangeExercise: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!exercise.isConfirmed) {
            if (hasAutoMatch && bestMatch != null) {
                Button(
                    onClick = { onConfirm(bestMatch.exerciseId, bestMatch.exerciseName) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Check, contentDescription = "Confirm exercise", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Confirm")
                }
            }
            OutlinedButton(onClick = onChangeExercise, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Edit, contentDescription = "Change exercise", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (hasAutoMatch) "Change" else "Select Exercise")
            }
        } else {
            OutlinedButton(onClick = onChangeExercise, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Edit, contentDescription = "Change exercise", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Change Exercise")
            }
        }
    }
    if (!exercise.isConfirmed && exercise.matchSuggestions.suggestions.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        SuggestionsList(
            suggestions = exercise.matchSuggestions.suggestions.take(3),
            onSelect = { result -> onConfirm(result.exerciseId, result.exerciseName) },
        )
    }
}

@Composable
private fun SuggestionsList(
    suggestions: List<ExerciseMatchResult>,
    onSelect: (ExerciseMatchResult) -> Unit,
) {
    Column {
        Text(
            text = "Suggestions:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        suggestions.forEach { suggestion ->
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(suggestion) },
                color = Color.Transparent,
            ) {
                Text(
                    text = suggestion.exerciseName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }
    }
}

private fun formatSetsPreview(sets: List<ParsedSetData>): String {
    if (sets.isEmpty()) return "No sets"

    val unit = sets.first().weightUnit
    val unitStr = if (unit == WeightUnit.KG) "kg" else "lbs"

    val groupedSets = sets.groupBy { "${it.reps}x${it.weight}" }

    return groupedSets.entries.joinToString(", ") { (_, groupedSetList) ->
        val first = groupedSetList.first()
        val count = groupedSetList.size
        if (count > 1) {
            "${count}x${first.reps} @ ${first.weight.toInt()}$unitStr"
        } else {
            "${first.reps} @ ${first.weight.toInt()}$unitStr"
        }
    }
}

@Composable
fun ExerciseSelectionDialog(
    suggestions: ExerciseMatchSuggestions,
    spokenName: String,
    interpretedName: String,
    onSelect: (String, String) -> Unit,
    onSearchExercises: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Exercise") },
        text = {
            Column {
                TranscriptionCard(spokenName = spokenName, interpretedName = interpretedName)
                SuggestionsList(suggestions = suggestions, onSelect = onSelect)
                TextButton(onClick = onSearchExercises, modifier = Modifier.fillMaxWidth()) {
                    Text("Search all exercises...")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun TranscriptionCard(
    spokenName: String,
    interpretedName: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Heard:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "\"$spokenName\"",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            if (interpretedName != spokenName) {
                Text(
                    text = "Interpreted as: $interpretedName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun SuggestionsList(
    suggestions: ExerciseMatchSuggestions,
    onSelect: (String, String) -> Unit,
) {
    if (suggestions.suggestions.isEmpty()) return

    Text(
        text = "Top matches:",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp),
    )

    suggestions.suggestions.take(5).forEachIndexed { index, result ->
        SuggestionRow(index = index, result = result, onSelect = onSelect)
    }

    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun SuggestionRow(
    index: Int,
    result: ExerciseMatchResult,
    onSelect: (String, String) -> Unit,
) {
    val isTopMatch = index == 0 && result.isAutoMatch
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onSelect(result.exerciseId, result.exerciseName) },
        color = Color.Transparent,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 10.dp)) {
            RankIndicator(index = index, isTopMatch = isTopMatch)
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = result.exerciseName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (isTopMatch) {
                Text(
                    text = "Best",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun RankIndicator(
    index: Int,
    isTopMatch: Boolean,
) {
    Box(
        modifier =
            Modifier
                .size(24.dp)
                .background(
                    color = if (isTopMatch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "${index + 1}",
            style = MaterialTheme.typography.labelSmall,
            color = if (isTopMatch) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
