package com.github.radupana.featherweight.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
import com.github.radupana.featherweight.data.PRType
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun PRCelebrationDialog(
    personalRecords: List<PersonalRecord>,
    onShare: (PersonalRecord) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
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
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    val confettiAnimation = rememberInfiniteTransition(label = "confetti")
    val confettiOffset by confettiAnimation.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confetti_offset"
    )
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,  // Force users to use "Continue Workout" button
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Confetti background
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                drawConfetti(confettiOffset)
            }
            
            // Main celebration card
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .scale(scale),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Celebration emoji and title
                    Text(
                        text = "🎉",
                        style = MaterialTheme.typography.displayLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        text = if (personalRecords.size == 1) "PERSONAL RECORD!" else "MULTIPLE PRS!",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // PR details
                    personalRecords.forEach { pr ->
                        PRDetailCard(
                            personalRecord = pr,
                            onShare = { onShare(pr) },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Continue Workout")
                        }
                        
                        Button(
                            onClick = { 
                                if (personalRecords.isNotEmpty()) {
                                    onShare(personalRecords.first())
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PRDetailCard(
    personalRecord: PersonalRecord,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Exercise name
            Text(
                text = personalRecord.exerciseName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // PR achievement
            Text(
                text = formatPRText(personalRecord),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // PR type badge
            AssistChip(
                onClick = { /* Do nothing */ },
                label = {
                    Text(
                        text = formatPRTypeText(personalRecord.recordType),
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                leadingIcon = {
                    Text(
                        text = getPRTypeEmoji(personalRecord.recordType),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            )
            
            // Previous record comparison
            if (personalRecord.previousWeight != null && personalRecord.previousDate != null) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Previous: ${personalRecord.previousWeight}kg × ${personalRecord.previousReps ?: 0}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "${personalRecord.previousDate?.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Improvement percentage
                if (personalRecord.improvementPercentage > 0) {
                    Text(
                        text = "+${String.format("%.1f", personalRecord.improvementPercentage)}% improvement",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        }
    }
}

private fun formatPRText(pr: PersonalRecord): String {
    return when (pr.recordType) {
        PRType.WEIGHT -> "${pr.weight}kg × ${pr.reps}"
        PRType.REPS -> "${pr.reps} reps @ ${pr.weight}kg"
        PRType.VOLUME -> "${pr.volume.toInt()}kg total"
        PRType.ESTIMATED_1RM -> "~${pr.estimated1RM?.toInt() ?: 0}kg 1RM"
    }
}

private fun formatPRTypeText(type: PRType): String {
    return when (type) {
        PRType.WEIGHT -> "Weight PR"
        PRType.REPS -> "Reps PR"
        PRType.VOLUME -> "Volume PR"
        PRType.ESTIMATED_1RM -> "1RM PR"
    }
}

private fun getPRTypeEmoji(type: PRType): String {
    return when (type) {
        PRType.WEIGHT -> "💪"
        PRType.REPS -> "🔥"
        PRType.VOLUME -> "📈"
        PRType.ESTIMATED_1RM -> "🏆"
    }
}

// Confetti particle data class
private data class ConfettiParticle(
    val x: Float,
    val y: Float,
    val color: Color,
    val size: Float,
    val rotation: Float
)

private fun DrawScope.drawConfetti(animationProgress: Float) {
    val colors = listOf(
        Color(0xFFFFD700), // Gold
        Color(0xFF4CAF50), // Green
        Color(0xFF2196F3), // Blue
        Color(0xFFFF5722), // Red
        Color(0xFF9C27B0), // Purple
        Color(0xFFFF9800)  // Orange
    )
    
    // Generate confetti particles
    val particles = (0..30).map {
        ConfettiParticle(
            x = Random.nextFloat() * size.width,
            y = -50f + (animationProgress * (size.height + 100f)) + Random.nextFloat() * 100f,
            color = colors.random(),
            size = Random.nextFloat() * 8f + 4f,
            rotation = Random.nextFloat() * 360f
        )
    }
    
    // Draw confetti
    particles.forEach { particle ->
        if (particle.y < size.height + 50f) {
            val alpha = when {
                particle.y < 0 -> 0f
                particle.y > size.height -> 0f
                else -> 1f - (particle.y / size.height) * 0.3f
            }
            
            drawCircle(
                color = particle.color.copy(alpha = alpha),
                radius = particle.size,
                center = Offset(
                    particle.x + sin(particle.y * 0.01f) * 20f,
                    particle.y
                )
            )
        }
    }
}