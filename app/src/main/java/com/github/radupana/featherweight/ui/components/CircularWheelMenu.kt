package com.github.radupana.featherweight.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.res.painterResource
import com.github.radupana.featherweight.R
import androidx.compose.material.icons.filled.SportsGymnastics

data class WheelSegment(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

@Composable
fun CircularWheelMenu(
    segments: List<WheelSegment>,
    userProfileImage: String? = null,
    userName: String = "",
    userStats: String = "",
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val wheelSize = 280.dp
    val density = LocalDensity.current
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Static wheel with properly aligned segments
        Canvas(
            modifier = Modifier.size(wheelSize)
        ) {
            drawWheel(segments)
        }
        
        // Clickable areas for each segment - properly aligned
        val segmentAngle = 360f / segments.size
        segments.forEachIndexed { index, segment ->
            // Calculate the center angle of each segment
            val centerAngle = (index * segmentAngle) - 90f + (segmentAngle / 2f)
            val angleRad = centerAngle * PI / 180
            
            // Position icons at 65% of radius for better visual balance
            val radius = with(density) { (wheelSize.toPx() / 2) * 0.65f }
            val offsetX = (cos(angleRad) * radius).toFloat()
            val offsetY = (sin(angleRad) * radius).toFloat()
            
            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .size(70.dp)
                    .clickable { segment.onClick() },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        segment.icon,
                        contentDescription = segment.label,
                        modifier = Modifier.size(24.dp),
                        tint = Color.White
                    )
                    Text(
                        segment.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        // Center profile button
        Card(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .clickable { onProfileClick() },
            elevation = CardDefaults.cardElevation(6.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.AccountCircle,
                    contentDescription = "Profile",
                    modifier = Modifier.size(50.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun DrawScope.drawWheel(segments: List<WheelSegment>) {
    val segmentAngle = 360f / segments.size
    val center = size.center
    val radius = size.minDimension / 2
    val innerRadius = radius * 0.32f // Leave space for center profile
    
    segments.forEachIndexed { index, segment ->
        val startAngle = index * segmentAngle - 90f
        
        // Draw segment fill
        drawArc(
            brush = Brush.radialGradient(
                colors = listOf(
                    segment.color.copy(alpha = 0.4f),
                    segment.color.copy(alpha = 0.6f)
                ),
                center = center,
                radius = radius
            ),
            startAngle = startAngle,
            sweepAngle = segmentAngle - 1f,
            useCenter = true,
            size = size
        )
        
        // Draw segment border
        drawArc(
            color = segment.color,
            startAngle = startAngle,
            sweepAngle = segmentAngle - 1f,
            useCenter = false,
            size = size,
            style = Stroke(width = 2.dp.toPx())
        )
    }
    
    // Draw inner circle to create donut shape
    drawCircle(
        color = Color.Black,
        radius = innerRadius,
        center = center
    )
}

@Composable
fun WheelHomeScreen(
    onStartFreestyle: () -> Unit,
    onBrowseProgrammes: () -> Unit,
    onNavigateToActiveProgramme: (() -> Unit)? = null,
    onStartProgrammeWorkout: () -> Unit,
    onGenerateAIProgramme: () -> Unit,
    onViewHistory: () -> Unit,
    onViewAnalytics: () -> Unit,
    onProfileClick: () -> Unit = {},
    onContinueWorkout: (() -> Unit)? = null,
    activeWorkout: String? = null,
    activeProgrammeName: String? = null,
    nextWorkoutName: String? = null,
    lastWorkoutInfo: LastWorkoutInfo? = null,
    modifier: Modifier = Modifier
) {
    // Dynamic segments based on current state
    val primaryColor = MaterialTheme.colorScheme.primary
    val hasAnyInProgressWorkout = activeWorkout != null
    
    val segments = remember(hasAnyInProgressWorkout, activeProgrammeName, nextWorkoutName, primaryColor) {
        buildList {
            // Primary action always at top
            when {
                hasAnyInProgressWorkout -> {
                    // Continue ANY active workout - will route to most recent
                    add(
                        WheelSegment(
                            id = "continue",
                            label = "Continue\n$activeWorkout",
                            icon = Icons.Filled.PlayArrow,
                            color = primaryColor,
                            onClick = onContinueWorkout ?: {}
                        )
                    )
                }
                nextWorkoutName != null && activeProgrammeName != null -> {
                    // Next programme workout
                    add(
                        WheelSegment(
                            id = "next",
                            label = "Start\n$nextWorkoutName",
                            icon = Icons.Filled.FitnessCenter,
                            color = primaryColor,
                            onClick = onStartProgrammeWorkout
                        )
                    )
                }
                else -> {
                    // Generic start workout (freestyle)
                    add(
                        WheelSegment(
                            id = "start",
                            label = "Start\nWorkout",
                            icon = Icons.Filled.PlayArrow,
                            color = primaryColor,
                            onClick = onStartFreestyle
                        )
                    )
                }
            }
            
            // Second segment - left side - ALWAYS Programmes
            add(
                WheelSegment(
                    id = "programmes",
                    label = "Programmes",
                    icon = Icons.Filled.Schedule,
                    color = Color(0xFF7C4DFF),
                    onClick = onBrowseProgrammes
                )
            )
            
            // Third segment - right side - ALWAYS Workouts
            add(
                WheelSegment(
                    id = "workouts",
                    label = "Workouts",
                    icon = Icons.Filled.FitnessCenter,
                    color = Color(0xFFE53935),
                    onClick = onViewHistory
                )
            )
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularWheelMenu(
            segments = segments,
            userProfileImage = null,
            userName = "Athlete",
            userStats = "7 day streak",
            onProfileClick = onProfileClick
        )
        
        // Last workout summary card
        lastWorkoutInfo?.let { info ->
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(0.9f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Last: ${info.name} (${info.daysAgo})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = info.exercises,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

data class LastWorkoutInfo(
    val name: String,
    val daysAgo: String,
    val exercises: String
)