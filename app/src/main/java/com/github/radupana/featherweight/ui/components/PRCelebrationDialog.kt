package com.github.radupana.featherweight.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.radupana.featherweight.data.PersonalRecord
import com.github.radupana.featherweight.util.WeightFormatter
import java.time.format.DateTimeFormatter
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun PRCelebrationDialog(
    personalRecords: List<PersonalRecord>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (personalRecords.isEmpty()) return

    val haptic = LocalHapticFeedback.current

    // Trigger haptic feedback when dialog appears
    LaunchedEffect(personalRecords) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    // Animation states
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        label = "scale",
    )

    val confettiAnimation = rememberInfiniteTransition(label = "confetti")
    val confettiOffset by confettiAnimation.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 3000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "confetti_offset",
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false, // Force users to use "Continue Workout" button
                usePlatformDefaultWidth = false,
            ),
    ) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            // Confetti background
            Canvas(
                modifier = Modifier.fillMaxSize(),
            ) {
                drawConfetti(confettiOffset)
            }

            // Main celebration card
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
                    // Celebration emoji and title
                    Text(
                        text = "🎉",
                        style = MaterialTheme.typography.displayLarge,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )

                    Text(
                        text = if (personalRecords.size == 1) "PERSONAL RECORD!" else "MULTIPLE PRS!",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )

                    // PR details
                    personalRecords.forEach { pr ->
                        PRDetailCard(
                            personalRecord = pr,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action button
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
            // Exercise name
            Text(
                text = personalRecord.exerciseName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // PR achievement
            Text(
                text = formatPRText(personalRecord),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )

            // Previous record comparison
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

                // Improvement percentage
                if (personalRecord.improvementPercentage > 0) {
                    Text(
                        text = "+${WeightFormatter.formatDecimal(personalRecord.improvementPercentage, 1)}% improvement",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4CAF50),
                    )
                }
            }
        }
    }
}

private fun formatPRText(pr: PersonalRecord): String = "${WeightFormatter.formatWeightWithUnit(pr.weight)} × ${pr.reps}"

// Confetti particle data class
private data class ConfettiParticle(
    val x: Float,
    val y: Float,
    val color: Color,
    val size: Float,
    val rotation: Float,
)

private fun DrawScope.drawConfetti(animationProgress: Float) {
    val colors =
        listOf(
            Color(0xFFFFD700), // Gold
            Color(0xFF4CAF50), // Green
            Color(0xFF2196F3), // Blue
            Color(0xFFFF5722), // Red
            Color(0xFF9C27B0), // Purple
            Color(0xFFFF9800), // Orange
        )

    // Generate confetti particles
    val particles =
        (0..30).map {
            ConfettiParticle(
                x = Random.nextFloat() * size.width,
                y = -50f + (animationProgress * (size.height + 100f)) + Random.nextFloat() * 100f,
                color = colors.random(),
                size = Random.nextFloat() * 8f + 4f,
                rotation = Random.nextFloat() * 360f,
            )
        }

    // Draw confetti
    particles.forEach { particle ->
        if (particle.y < size.height + 50f) {
            val alpha =
                when {
                    particle.y < 0 -> 0f
                    particle.y > size.height -> 0f
                    else -> 1f - (particle.y / size.height) * 0.3f
                }

            drawCircle(
                color = particle.color.copy(alpha = alpha),
                radius = particle.size,
                center =
                    Offset(
                        particle.x + sin(particle.y * 0.01f) * 20f,
                        particle.y,
                    ),
            )
        }
    }
}
