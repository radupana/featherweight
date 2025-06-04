package com.github.radupana.featherweight

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.github.radupana.featherweight.ui.screens.HistoryScreen
import com.github.radupana.featherweight.ui.screens.HomeScreen
import com.github.radupana.featherweight.ui.screens.SplashScreen
import com.github.radupana.featherweight.ui.screens.WorkoutScreen
import com.github.radupana.featherweight.ui.theme.FeatherweightTheme

enum class Screen {
    SPLASH,
    HOME,
    WORKOUT,
    HISTORY,
}

data class NavigationItem(
    val screen: Screen,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

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

                    else -> {
                        // Main app with bottom navigation
                        MainAppWithNavigation(
                            currentScreen = currentScreen,
                            onScreenChange = { screen -> currentScreen = screen },
                            onStartTemplate = { showTemplateDialog = true },
                        )
                    }
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

@Composable
fun MainAppWithNavigation(
    currentScreen: Screen,
    onScreenChange: (Screen) -> Unit,
    onStartTemplate: () -> Unit,
) {
    val navigationItems =
        listOf(
            NavigationItem(Screen.HOME, "Home", Icons.Filled.Home),
            NavigationItem(Screen.WORKOUT, "Workout", Icons.Filled.FitnessCenter),
            NavigationItem(Screen.HISTORY, "History", Icons.Filled.History),
        )

    Scaffold(
        bottomBar = {
            if (currentScreen != Screen.SPLASH) {
                NavigationBar {
                    navigationItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentScreen == item.screen,
                            onClick = { onScreenChange(item.screen) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        when (currentScreen) {
            Screen.HOME ->
                HomeScreen(
                    onStartFreestyle = { onScreenChange(Screen.WORKOUT) },
                    onStartTemplate = onStartTemplate,
                    modifier = Modifier.padding(innerPadding),
                )

            Screen.WORKOUT ->
                WorkoutScreen(
                    onBack = { onScreenChange(Screen.HOME) },
                    modifier = Modifier.padding(innerPadding),
                )

            Screen.HISTORY ->
                HistoryScreen(
                    modifier = Modifier.padding(innerPadding),
                )

            Screen.SPLASH -> {
                // Should not reach here
            }
        }
    }
}

// Extension function to add modifier to screens that don't have it
@Composable
fun HomeScreen(
    onStartFreestyle: () -> Unit,
    onStartTemplate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HomeScreen(
        onStartFreestyle = onStartFreestyle,
        onStartTemplate = onStartTemplate,
    )
}

@Composable
fun WorkoutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WorkoutScreen(
        onBack = onBack,
    )
}

@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    HistoryScreen()
}
