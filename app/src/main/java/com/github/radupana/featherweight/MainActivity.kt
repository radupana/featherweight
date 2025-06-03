package com.github.radupana.featherweight

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.github.radupana.featherweight.ui.dialogs.ChooseTemplateDialog
import com.github.radupana.featherweight.ui.screens.HistoryScreen
import com.github.radupana.featherweight.ui.screens.HomeScreen
import com.github.radupana.featherweight.ui.screens.WorkoutScreen
import com.github.radupana.featherweight.ui.theme.FeatherweightTheme

enum class BottomNavTab(
    val route: String,
    val title: String,
    val icon: ImageVector,
) {
    HOME("home", "Home", Icons.Filled.Home),
    WORKOUT("workout", "Workout", Icons.Filled.FitnessCenter),
    HISTORY("history", "History", Icons.Filled.History),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            FeatherweightTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    var selectedTab by remember { mutableStateOf(BottomNavTab.HOME) }
    var showTemplateDialog by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
            ) {
                BottomNavTab.entries.forEach { tab ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                tab.icon,
                                contentDescription = tab.title,
                            )
                        },
                        label = {
                            Text(
                                tab.title,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        colors =
                            NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                BottomNavTab.HOME ->
                    HomeScreen(
                        onStartFreestyle = { selectedTab = BottomNavTab.WORKOUT },
                        onStartTemplate = { showTemplateDialog = true },
                    )

                BottomNavTab.WORKOUT ->
                    WorkoutScreen(
                        onBack = { selectedTab = BottomNavTab.HOME },
                    )

                BottomNavTab.HISTORY -> HistoryScreen()
            }
        }

        // Template dialog (existing functionality)
        if (showTemplateDialog) {
            ChooseTemplateDialog(
                onClose = { showTemplateDialog = false },
                onTemplateSelected = {
                    // TODO: implement template selection and navigate to workout
                    selectedTab = BottomNavTab.WORKOUT
                    showTemplateDialog = false
                },
            )
        }
    }
}
