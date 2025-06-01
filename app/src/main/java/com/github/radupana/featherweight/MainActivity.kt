package com.github.radupana.featherweight

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.github.radupana.featherweight.ui.HomeScreen
import com.github.radupana.featherweight.ui.WorkoutScreen

enum class Screen {
    HOME,
    WORKOUT,
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
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
