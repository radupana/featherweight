package com.github.radupana.featherweight.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
