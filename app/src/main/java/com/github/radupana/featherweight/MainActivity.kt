package com.github.radupana.featherweight

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import com.github.radupana.featherweight.ui.screens.HomeScreen
import com.github.radupana.featherweight.ui.screens.WorkoutScreen
import com.github.radupana.featherweight.ui.theme.FeatherweightTheme

enum class Screen {
    HOME,
    WORKOUT,
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display for modern look
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            FeatherweightTheme {
                var currentScreen by remember { mutableStateOf(Screen.HOME) }
                var showTemplateDialog by remember { mutableStateOf(false) }

                when (currentScreen) {
                    Screen.HOME ->
                        HomeScreen(
                            onFreestyle = { currentScreen = Screen.WORKOUT },
                            onTemplate = { showTemplateDialog = true },
                        )

                    Screen.WORKOUT ->
                        WorkoutScreen(
                            onBack = { currentScreen = Screen.HOME },
                        )
                }

                if (showTemplateDialog) {
                    com.github.radupana.featherweight.ui.ChooseTemplateDialog(
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