package com.github.radupana.featherweight.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.github.radupana.featherweight.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    // Animation states
    var startAnimation by remember { mutableStateOf(false) }

    // Logo animation
    val logoScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.3f,
        animationSpec =
            tween(
                durationMillis = 800,
                easing = FastOutSlowInEasing,
            ),
        label = "logoScale",
    )

    val logoAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec =
            tween(
                durationMillis = 800,
                delayMillis = 200,
                easing = FastOutSlowInEasing,
            ),
        label = "logoAlpha",
    )

    // Solid dark background to match logo
    val backgroundColor = Color(0xFF1A1A1A) // Dark background matching your logo

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2500) // Total splash duration
        onSplashFinished()
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(backgroundColor),
        // Use solid dark background
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Large app logo taking up most of the screen
            Image(
                painter = painterResource(id = R.drawable.featherweight_logo_black),
                contentDescription = "Featherweight Logo",
                modifier =
                    Modifier
                        .fillMaxWidth(0.8f) // Takes up 80% of screen width
                        .aspectRatio(1f) // Keep it square
                        .scale(logoScale)
                        .alpha(logoAlpha),
            )
        }

        // Loading indicator at bottom
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(bottom = 64.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            if (startAnimation) {
                CircularProgressIndicator(
                    modifier =
                        Modifier
                            .size(32.dp)
                            .alpha(logoAlpha),
                    color = Color.White, // White spinner on dark background
                    strokeWidth = 3.dp,
                )
            }
        }
    }
}
