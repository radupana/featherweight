package com.github.radupana.featherweight.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.github.radupana.featherweight.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    val context = LocalContext.current

    // Animation states
    var startAnimation by remember { mutableStateOf(false) }
    var impactOccurred by remember { mutableStateOf(false) }

    // Weight drop animation with physics
    val logoTranslationY by animateFloatAsState(
        targetValue = if (startAnimation) 0f else -800f,
        animationSpec =
            spring(
                dampingRatio = 0.35f, // Low damping for bounce effect
                stiffness = Spring.StiffnessLow, // Realistic weight drop
            ),
        finishedListener = {
            if (!impactOccurred) {
                impactOccurred = true
                // Trigger haptic feedback on impact
                triggerHapticFeedback(context)
            }
        },
        label = "logoTranslationY",
    )

    // Logo fade in slightly delayed
    val logoAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec =
            tween(
                durationMillis = 400,
                delayMillis = 100,
                easing = LinearEasing,
            ),
        label = "logoAlpha",
    )

    // Screen shake effect on impact
    val screenShakeX by animateFloatAsState(
        targetValue = 0f,
        animationSpec =
            if (impactOccurred) {
                // Create a damped oscillation for shake
                spring(
                    dampingRatio = 0.2f,
                    stiffness = Spring.StiffnessHigh,
                )
            } else {
                tween(0)
            },
        label = "screenShakeX",
    )

    // Small scale pulse after landing
    val scalePulse by animateFloatAsState(
        targetValue = if (impactOccurred) 1f else 0.95f,
        animationSpec =
            if (impactOccurred) {
                spring(
                    dampingRatio = 0.5f,
                    stiffness = Spring.StiffnessMedium,
                )
            } else {
                tween(0)
            },
        label = "scalePulse",
    )

    // Background color
    val backgroundColor = Color(0xFF1A1A1A)

    LaunchedEffect(Unit) {
        // Start animation immediately
        startAnimation = true
        // Total duration: ~1.6 seconds (drop + settle time)
        delay(1600)
        onSplashFinished()
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .graphicsLayer {
                    // Apply subtle screen shake
                    translationX = screenShakeX * 5f // 5 pixel max shake
                },
        contentAlignment = Alignment.Center,
    ) {
        // Logo that drops like a weight
        Image(
            painter = painterResource(id = R.drawable.featherweight_logo_black),
            contentDescription = "Featherweight Logo",
            modifier =
                Modifier
                    .fillMaxWidth(0.8f)
                    .aspectRatio(1f)
                    .scale(scalePulse)
                    .alpha(logoAlpha)
                    .graphicsLayer {
                        translationY = logoTranslationY
                    },
        )
    }
}

private fun triggerHapticFeedback(context: Context) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    vibrator?.let {
        // Modern haptic feedback - short sharp impact
        it.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}
