package com.github.radupana.featherweight.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.radupana.featherweight.ui.utils.NavigationContext
import com.github.radupana.featherweight.ui.utils.rememberKeyboardState
import com.github.radupana.featherweight.ui.utils.systemBarsPadding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveSearchLayout(
    title: String,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    filters: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val isKeyboardVisible by rememberKeyboardState()
    val focusRequester = remember { FocusRequester() }
    var isSearchFocused by remember { mutableStateOf(false) }

    val isCompactMode = isKeyboardVisible || isSearchFocused

    Scaffold(
        modifier = modifier,
        topBar = {
            AdaptiveTopBar(
                title = title,
                searchQuery = searchQuery,
                onSearchChange = onSearchChange,
                onBack = onBack,
                isCompact = isCompactMode,
                focusRequester = focusRequester,
                onSearchFocusChanged = { isSearchFocused = it },
                actions = actions,
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .systemBarsPadding(NavigationContext.FULL_SCREEN),
        ) {
            AnimatedVisibility(
                visible = !isCompactMode,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut(),
            ) {
                filters()
            }

            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdaptiveTopBar(
    title: String,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onBack: () -> Unit,
    isCompact: Boolean,
    focusRequester: FocusRequester,
    onSearchFocusChanged: (Boolean) -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    TopAppBar(
        title = {
            AnimatedContent(
                targetState = isCompact,
                transitionSpec = {
                    slideInHorizontally { -it } + fadeIn() togetherWith
                        slideOutHorizontally { it } + fadeOut()
                },
                label = "title_search_transition",
            ) { compact ->
                if (compact) {
                    CompactSearchBar(
                        query = searchQuery,
                        onQueryChange = onSearchChange,
                        focusRequester = focusRequester,
                        onFocusChanged = onSearchFocusChanged,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Text(
                        title,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            AnimatedVisibility(
                visible = !isCompact,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                Row {
                    actions()
                }
            }
        },
    )
}

@Composable
private fun CompactSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier =
            modifier
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    onFocusChanged(focusState.isFocused)
                },
        placeholder = { Text("Search...") },
        leadingIcon = {
            Icon(Icons.Filled.Search, contentDescription = "Search")
        },
        trailingIcon =
            if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear")
                    }
                }
            } else {
                null
            },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            ),
    )
}

@Composable
fun CompactSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search exercises...",
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Icon(Icons.Filled.Search, contentDescription = "Search")
        },
        trailingIcon =
            if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear")
                    }
                }
            } else {
                null
            },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
    )
}
