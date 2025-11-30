package com.github.radupana.featherweight.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import com.github.radupana.featherweight.data.PRType
import com.github.radupana.featherweight.data.PersonalRecord
import com.github.radupana.featherweight.ui.theme.FeatherweightColors
import com.github.radupana.featherweight.util.WeightFormatter
import java.time.format.DateTimeFormatter

@Composable
fun PRCelebrationDialog(
    personalRecords: List<PersonalRecord>,
    exerciseNames: Map<String, String>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (personalRecords.isEmpty()) return

    val haptic = LocalHapticFeedback.current

    LaunchedEffect(personalRecords) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    val primaryPR = personalRecords.firstOrNull() ?: return

    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        label = "scale",
    )

    val composition by rememberLottieComposition(
        LottieCompositionSpec.Asset("trophy_animation.json"),
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1,
        isPlaying = true,
        restartOnPlay = true,
    )

    val goldColor = FeatherweightColors.gold()
    val dynamicProperties =
        rememberLottieDynamicProperties(
            rememberLottieDynamicProperty(
                property = LottieProperty.STROKE_COLOR,
                value = goldColor.toArgb(),
                keyPath = arrayOf("**"),
            ),
        )

    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
            ),
    ) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth(0.9f)
                        .scale(scale),
                shape = RoundedCornerShape(20.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    LottieAnimation(
                        composition = composition,
                        progress = { progress },
                        dynamicProperties = dynamicProperties,
                        modifier =
                            Modifier
                                .size(100.dp)
                                .padding(bottom = 8.dp),
                    )

                    Text(
                        text = "New Personal Record!",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )

                    PRDetailCard(
                        personalRecord = primaryPR,
                        exerciseNames = exerciseNames,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Continue Workout")
                    }
                }
            }
        }
    }
}

@Composable
private fun PRDetailCard(
    personalRecord: PersonalRecord,
    exerciseNames: Map<String, String>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Use unified key (no more collisions since tables are merged)
            val key = "exercise_${personalRecord.exerciseId}"
            val exerciseName = exerciseNames[key] ?: "Unknown Exercise"
            Text(
                text = exerciseName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (personalRecord.recordType == PRType.WEIGHT) {
                Text(
                    text = formatPRText(personalRecord),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )

                val notes = personalRecord.notes ?: ""
                if (notes.contains("could potentially lift")) {
                    // Match both kg and lbs formats
                    val potentialMatch = "\\(Based on your ([0-9.]+(?:\\.[0-9]+)?)(kg|lbs) 1RM.*\\)".toRegex().find(notes)
                    if (potentialMatch != null) {
                        val weight = potentialMatch.groupValues[1]
                        val unit = potentialMatch.groupValues[2]
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your $weight$unit 1RM suggests you could lift more!",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                } else if (personalRecord.estimated1RM != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "New One Rep Max: ${WeightFormatter.formatWeightWithUnit(personalRecord.estimated1RM)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            } else if (personalRecord.recordType == PRType.ESTIMATED_1RM) {
                Text(
                    text = WeightFormatter.formatWeightWithUnit(personalRecord.estimated1RM ?: 0f),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Estimated One Rep Max",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Achieved with: ${formatPRText(personalRecord)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }

            if (personalRecord.previousWeight != null && personalRecord.previousDate != null) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Previous: ${
                        WeightFormatter.formatWeightWithUnit(
                            personalRecord.previousWeight,
                        )
                    } × ${personalRecord.previousReps ?: 0}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Text(
                    text = "${personalRecord.previousDate?.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (personalRecord.improvementPercentage > 0) {
                    Text(
                        text = "+${WeightFormatter.formatDecimal(personalRecord.improvementPercentage, 1)}% improvement",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = FeatherweightColors.success(),
                    )
                }
            }
        }
    }
}

private fun formatPRText(pr: PersonalRecord): String {
    val baseText = "${WeightFormatter.formatWeightWithUnit(pr.weight)} × ${pr.reps}"
    return if (pr.rpe != null) {
        "$baseText @ RPE ${WeightFormatter.formatRPE(pr.rpe)}"
    } else {
        baseText
    }
}
