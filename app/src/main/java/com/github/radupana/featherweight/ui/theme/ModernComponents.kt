package com.github.radupana.featherweight.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Modern Glassmorphic Card
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    elevation: Dp = 4.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        label = "card_scale",
    )

    Card(
        modifier =
            modifier
                .scale(scale)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onClick()
                        }
                    } else {
                        Modifier
                    },
                ),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = FeatherweightColors.cardGlassBackground(),
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = elevation,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = FeatherweightColors.cardGlassBorder(),
                        shape = RoundedCornerShape(16.dp),
                    )
                    .padding(16.dp),
            content = content,
        )
    }

    LaunchedEffect(onClick) {
        if (onClick != null) {
            isPressed = false
        }
    }
}

// Gradient Button
@Composable
fun GradientButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    startColor: Color = FeatherweightColors.primaryGradientStart,
    endColor: Color = FeatherweightColors.primaryGradientEnd,
    content: @Composable RowScope.() -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessHigh,
            ),
        label = "button_scale",
    )

    val alpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.6f,
        label = "button_alpha",
    )

    Box(
        modifier =
            modifier
                .scale(scale)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    brush =
                        Brush.horizontalGradient(
                            colors =
                                listOf(
                                    startColor.copy(alpha = alpha),
                                    endColor.copy(alpha = alpha),
                                ),
                        ),
                )
                .clickable(
                    enabled = enabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    isPressed = true
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            content = content,
        )
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(150)
            isPressed = false
        }
    }
}

// Success Progress Indicator with Gradient
@Composable
fun GradientProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    progressGradientStart: Color = FeatherweightColors.successGradientStart,
    progressGradientEnd: Color = FeatherweightColors.successGradientEnd,
    strokeWidth: Dp = 8.dp,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec =
            tween(
                durationMillis = 1000,
                easing = EaseOutCubic,
            ),
        label = "progress_animation",
    )

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(strokeWidth)
                .clip(RoundedCornerShape(strokeWidth / 2))
                .background(trackColor),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(strokeWidth / 2))
                    .background(
                        brush =
                            Brush.horizontalGradient(
                                colors = listOf(progressGradientStart, progressGradientEnd),
                            ),
                    ),
        )
    }
}

// Shimmer Loading Effect
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    isLoading: Boolean = true,
) {
    if (isLoading) {
        val shimmerColors =
            listOf(
                FeatherweightColors.shimmerBase(),
                FeatherweightColors.shimmerHighlight(),
                FeatherweightColors.shimmerBase(),
            )

        val transition = rememberInfiniteTransition(label = "shimmer")
        val translateAnim by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "shimmer_translate",
        )

        Box(
            modifier =
                modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        brush =
                            Brush.horizontalGradient(
                                colors = shimmerColors,
                                startX = translateAnim - 300f,
                                endX = translateAnim,
                            ),
                    ),
        )
    }
}

// Breathing Animation for Active Elements
@Composable
fun BreathingGlow(
    modifier: Modifier = Modifier,
    glowColor: Color = FeatherweightColors.primaryGradientStart,
    content: @Composable () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(2000, easing = EaseInOutCubic),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "breathing_alpha",
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(2000, easing = EaseInOutCubic),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "breathing_scale",
    )

    Box(
        modifier =
            modifier
                .scale(scale)
                .blur(radius = 20.dp)
                .background(
                    color = glowColor.copy(alpha = alpha),
                    shape = RoundedCornerShape(16.dp),
                ),
    ) {
        content()
    }
}
