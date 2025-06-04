package com.github.radupana.featherweight

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.github.radupana.featherweight.ui.screens.HomeScreen
import com.github.radupana.featherweight.ui.screens.SplashScreen
import com.github.radupana.featherweight.ui.screens.WorkoutScreen
import com.github.radupana.featherweight.ui.theme.FeatherweightTheme

enum class Screen {
    SPLASH,
    HOME,
    WORKOUT,
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen (Android 12+ native splash)
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display for modern look
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            FeatherweightTheme {
                var currentScreen by remember { mutableStateOf(Screen.SPLASH) }
                var showTemplateDialog by remember { mutableStateOf(false) }

                when (currentScreen) {
                    Screen.SPLASH ->
                        SplashScreen(
                            onSplashFinished = { currentScreen = Screen.HOME },
                        )

                    Screen.HOME ->
                        HomeScreen(
                            onStartFreestyle = { currentScreen = Screen.WORKOUT },
                            onStartTemplate = { showTemplateDialog = true },
                        )

                    Screen.WORKOUT ->
                        WorkoutScreen(
                            onBack = { currentScreen = Screen.HOME },
                        )
                }

                if (showTemplateDialog) {
                    com.github.radupana.featherweight.ui.dialogs.ChooseTemplateDialog(
                        onClose = { showTemplateDialog = false },
                        onTemplateSelected = {
                            // TODO: implement template selection
                            showTemplateDialog = false
                        },
                    )
                }
            }
        }
    }
}
