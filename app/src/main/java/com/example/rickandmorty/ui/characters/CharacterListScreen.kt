package com.example.rickandmorty.ui.characters

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.ColorImage
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import com.example.rickandmorty.R
import com.example.rickandmorty.domain.model.Character
import com.example.rickandmorty.domain.model.CharacterStatusFilter
import com.example.rickandmorty.ui.characters.components.CharacterCard
import com.example.rickandmorty.ui.theme.RickAndMortyAppTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@Composable
fun CharacterListScreen(
    viewModel: CharacterListViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CharacterListContent(
        uiState = uiState,
        modifier = modifier,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onStatusSelected = viewModel::onStatusSelected,
        onSpeciesChanged = viewModel::onSpeciesChanged,
        onClearFilters = viewModel::clearFilters,
        onRefresh = viewModel::refresh,
        onLoadNextPage = viewModel::loadNextPage,
        onRetryInitialLoad = viewModel::retryInitialLoad,
        onRetryPagination = viewModel::retryPagination,
        onLogout = onLogout,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CharacterListContent(
    uiState: CharacterListUiState,
    modifier: Modifier = Modifier,
    onSearchQueryChanged: (String) -> Unit = {},
    onStatusSelected: (CharacterStatusFilter) -> Unit = {},
    onSpeciesChanged: (String) -> Unit = {},
    onClearFilters: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onLoadNextPage: () -> Unit = {},
    onRetryInitialLoad: () -> Unit = {},
    onRetryPagination: () -> Unit = {},
    onLogout: () -> Unit = {},
) {
    var areFiltersVisible by rememberSaveable { mutableStateOf(true) }
    var isLogoutConfirmationVisible by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val directionThreshold = with(LocalDensity.current) { FILTER_VISIBILITY_THRESHOLD.toPx() }
    val filterVisibilityConnection = remember(directionThreshold, areFiltersVisible) {
        FilterVisibilityNestedScrollConnection(
            thresholdPx = directionThreshold,
            onShowFilters = { areFiltersVisible = true },
            onHideFilters = { areFiltersVisible = false },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CharacterListHeader(
                searchQuery = uiState.searchQuery,
                hasActiveFilters = uiState.hasActiveFilters,
                areFiltersVisible = areFiltersVisible,
                onSearchQueryChanged = onSearchQueryChanged,
                onToggleFilters = { areFiltersVisible = !areFiltersVisible },
                onLogout = { isLogoutConfirmationVisible = true },
            )
        },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            val horizontalPadding = if (maxWidth >= TABLET_WIDTH) 24.dp else 16.dp
            Column(
                modifier = Modifier
                    .widthIn(max = MAX_CONTENT_WIDTH)
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding),
            ) {
                AnimatedVisibility(
                    visible = areFiltersVisible,
                    modifier = Modifier.testTag(FILTER_PANEL_TEST_TAG),
                    enter = expandVertically(
                        animationSpec = tween(FILTER_ANIMATION_MILLIS),
                        expandFrom = Alignment.Top,
                    ) + fadeIn(animationSpec = tween(FILTER_ANIMATION_MILLIS)),
                    exit = shrinkVertically(
                        animationSpec = tween(FILTER_ANIMATION_MILLIS),
                        shrinkTowards = Alignment.Top,
                    ) + fadeOut(animationSpec = tween(FILTER_ANIMATION_MILLIS)),
                ) {
                    CharacterFilterPanel(
                        uiState = uiState,
                        onStatusSelected = onStatusSelected,
                        onSpeciesChanged = onSpeciesChanged,
                        onClearFilters = onClearFilters,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                if (uiState.isFilterPending) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                    )
                }
                if (!uiState.isInitialLoading && uiState.initialError == null) {
                    ResultsSummary(
                        totalCharacters = uiState.totalCharacters,
                        hasActiveFilters = uiState.hasActiveFilters,
                    )
                }
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .nestedScroll(filterVisibilityConnection),
                ) {
                    when {
                        uiState.isInitialLoading && uiState.characters.isEmpty() -> InitialLoadingState(
                            modifier = Modifier.fillMaxSize(),
                        )

                        uiState.initialError != null && uiState.characters.isEmpty() -> InitialErrorState(
                            error = uiState.initialError,
                            onRetry = onRetryInitialLoad,
                            modifier = Modifier.fillMaxSize(),
                        )

                        uiState.characters.isEmpty() -> EmptyState(
                            hasActiveFilters = uiState.hasActiveFilters,
                            onClearFilters = onClearFilters,
                            modifier = Modifier.fillMaxSize(),
                        )

                        else -> CharacterList(
                            uiState = uiState,
                            listState = listState,
                            onLoadNextPage = onLoadNextPage,
                            onRetryPagination = onRetryPagination,
                            onRefresh = onRefresh,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }

    if (isLogoutConfirmationVisible) {
        LogoutConfirmationDialog(
            onDismiss = { isLogoutConfirmationVisible = false },
            onConfirm = {
                isLogoutConfirmationVisible = false
                onLogout()
            },
        )
    }
}

@Composable
private fun CharacterListHeader(
    searchQuery: String,
    hasActiveFilters: Boolean,
    areFiltersVisible: Boolean,
    onSearchQueryChanged: (String) -> Unit,
    onToggleFilters: () -> Unit,
    onLogout: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PortalMark(modifier = Modifier.size(28.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.character_list_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        text = stringResource(R.string.character_list_subtitle),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                IconButton(
                    onClick = onLogout,
                    modifier = Modifier.testTag(LOGOUT_BUTTON_TEST_TAG),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_logout),
                        contentDescription = stringResource(R.string.auth_logout),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChanged,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp)
                        .testTag(SEARCH_FIELD_TEST_TAG),
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    label = { Text(stringResource(R.string.character_search_label)) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_search),
                            contentDescription = null,
                        )
                    },
                    trailingIcon = if (searchQuery.isEmpty()) null else {
                        {
                            IconButton(onClick = { onSearchQueryChanged("") }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_close),
                                    contentDescription = stringResource(R.string.character_clear_search),
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                )
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                ) {
                    Box {
                        IconButton(
                            onClick = onToggleFilters,
                            modifier = Modifier
                                .size(52.dp)
                                .testTag(FILTER_TOGGLE_TEST_TAG),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_filter),
                                contentDescription = stringResource(
                                    if (areFiltersVisible) {
                                        R.string.character_hide_filters
                                    } else {
                                        R.string.character_show_filters
                                    },
                                ),
                            )
                        }
                        if (hasActiveFilters) {
                            val activeIndicatorColor = MaterialTheme.colorScheme.tertiary
                            Canvas(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(7.dp)
                                    .size(9.dp),
                            ) {
                                drawCircle(color = activeIndicatorColor)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogoutConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.auth_logout_confirmation_title)) },
        text = { Text(stringResource(R.string.auth_logout_confirmation_message)) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.auth_logout_confirmation_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.auth_logout_confirmation_cancel))
            }
        },
    )
}

@Composable
private fun CharacterFilterPanel(
    uiState: CharacterListUiState,
    onStatusSelected: (CharacterStatusFilter) -> Unit,
    onSpeciesChanged: (String) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.character_filters_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                if (uiState.hasActiveFilters) {
                    TextButton(onClick = onClearFilters) {
                        Text(stringResource(R.string.character_clear_filters))
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CharacterStatusFilter.entries.forEach { status ->
                    FilterChip(
                        selected = uiState.selectedStatus == status,
                        onClick = { onStatusSelected(status) },
                        label = { Text(status.filterLabel()) },
                    )
                }
            }
            OutlinedTextField(
                value = uiState.speciesQuery,
                onValueChange = onSpeciesChanged,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.character_species_filter_label)) },
                trailingIcon = if (uiState.speciesQuery.isEmpty()) null else {
                    {
                        IconButton(onClick = { onSpeciesChanged("") }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = stringResource(R.string.character_clear_species),
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )
        }
    }
}

@Composable
private fun ResultsSummary(
    totalCharacters: Int,
    hasActiveFilters: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = pluralStringResource(
                R.plurals.character_results_count,
                totalCharacters,
                totalCharacters,
            ),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (hasActiveFilters) {
            Text(
                text = stringResource(R.string.character_filtered_results),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun CharacterList(
    uiState: CharacterListUiState,
    listState: LazyListState,
    onLoadNextPage: () -> Unit,
    onRetryPagination: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PaginationEffect(
        listState = listState,
        itemCount = uiState.characters.size,
        canLoadMore = !uiState.isLoadingMore &&
            !uiState.isRefreshing &&
            !uiState.isFilterPending &&
            !uiState.isLastPage &&
            uiState.paginationError == null,
        onLoadNextPage = onLoadNextPage,
    )

    LazyColumn(
        modifier = modifier.testTag(CHARACTER_LIST_TEST_TAG),
        state = listState,
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        uiState.refreshError?.let { error ->
            item(key = "refresh_error") {
                RefreshErrorState(error = error, onRetry = onRefresh)
            }
        }
        items(
            items = uiState.characters,
            key = Character::id,
        ) { character ->
            CharacterCard(character = character)
        }

        when {
            uiState.isLoadingMore -> item(key = "pagination_loading") {
                PaginationLoadingState()
            }

            uiState.paginationError != null -> item(key = "pagination_error") {
                PaginationErrorState(
                    error = uiState.paginationError,
                    onRetry = onRetryPagination,
                )
            }

            uiState.isLastPage -> item(key = "pagination_end") {
                PaginationEndState()
            }
        }
    }
}

@Composable
private fun PaginationEffect(
    listState: LazyListState,
    itemCount: Int,
    canLoadMore: Boolean,
    onLoadNextPage: () -> Unit,
) {
    LaunchedEffect(listState, itemCount, canLoadMore) {
        if (!canLoadMore || itemCount == 0) return@LaunchedEffect
        snapshotFlow {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val totalItems = listState.layoutInfo.totalItemsCount
            totalItems > 0 && lastVisibleItem >= totalItems - PAGINATION_PREFETCH_DISTANCE
        }
            .distinctUntilChanged()
            .filter { it }
            .collect { onLoadNextPage() }
    }
}

@Composable
private fun InitialLoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(R.string.character_initial_loading),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun InitialErrorState(
    error: CharacterListUiError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MessageState(
        title = stringResource(R.string.character_initial_error_title),
        message = errorMessage(error),
        actionLabel = stringResource(R.string.character_retry),
        onAction = onRetry,
        modifier = modifier,
    )
}

@Composable
private fun EmptyState(
    hasActiveFilters: Boolean,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MessageState(
        title = stringResource(
            if (hasActiveFilters) R.string.character_no_results_title else R.string.character_empty_title,
        ),
        message = stringResource(
            if (hasActiveFilters) R.string.character_no_results_message else R.string.character_empty_message,
        ),
        actionLabel = stringResource(R.string.character_clear_filters).takeIf { hasActiveFilters },
        onAction = onClearFilters.takeIf { hasActiveFilters },
        modifier = modifier,
    )
}

@Composable
private fun MessageState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        PortalMark(modifier = Modifier.size(48.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Button(
                onClick = onAction,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun RefreshErrorState(
    error: CharacterListUiError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ErrorBanner(
        title = stringResource(R.string.character_refresh_error_title),
        message = errorMessage(error),
        onRetry = onRetry,
        modifier = modifier,
    )
}

@Composable
private fun PaginationLoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator(modifier = Modifier.size(32.dp))
        Text(
            text = stringResource(R.string.character_pagination_loading),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PaginationErrorState(
    error: CharacterListUiError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ErrorBanner(
        title = stringResource(R.string.character_pagination_error_title),
        message = errorMessage(error),
        onRetry = onRetry,
        modifier = modifier,
    )
}

@Composable
private fun ErrorBanner(
    title: String,
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            OutlinedButton(onClick = onRetry) {
                Text(stringResource(R.string.character_retry))
            }
        }
    }
}

@Composable
private fun PaginationEndState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HorizontalDivider()
        Text(
            text = stringResource(R.string.character_pagination_end),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PortalMark(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Canvas(modifier = modifier.size(36.dp)) {
        val stroke = size.minDimension * 0.14f
        drawCircle(
            color = color.copy(alpha = 0.22f),
            radius = size.minDimension * 0.42f,
            center = center,
            style = Stroke(width = stroke),
        )
        drawCircle(
            color = color,
            radius = size.minDimension * 0.15f,
            center = Offset(center.x, center.y),
        )
    }
}

@Composable
private fun CharacterStatusFilter.filterLabel(): String = when (this) {
    CharacterStatusFilter.All -> stringResource(R.string.character_filter_all)
    CharacterStatusFilter.Alive -> stringResource(R.string.character_status_alive)
    CharacterStatusFilter.Dead -> stringResource(R.string.character_status_dead)
    CharacterStatusFilter.Unknown -> stringResource(R.string.character_status_unknown)
}

@Composable
private fun errorMessage(error: CharacterListUiError): String = when (error) {
    CharacterListUiError.Connection -> stringResource(R.string.character_error_connection)
    CharacterListUiError.Serialization -> stringResource(R.string.character_error_serialization)
    is CharacterListUiError.Http -> stringResource(R.string.character_error_http, error.statusCode)
}

@Preview(showBackground = true, widthDp = 390, heightDp = 900)
@Composable
private fun CharacterListPreview() {
    PreviewTheme {
        CharacterListContent(
            uiState = CharacterListUiState(
                characters = previewCharacters,
                totalCharacters = 3,
                isInitialLoading = false,
                isLastPage = true,
            ),
        )
    }
}

@Preview(
    showBackground = true,
    widthDp = 700,
    heightDp = 600,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun CharacterListTabletDarkPreview() {
    PreviewTheme(darkTheme = true) {
        CharacterListContent(
            uiState = CharacterListUiState(
                characters = previewCharacters,
                searchQuery = "Morty",
                selectedStatus = CharacterStatusFilter.Alive,
                totalCharacters = 2,
                isInitialLoading = false,
                isLastPage = true,
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 900)
@Composable
private fun CharacterNoResultsPreview() {
    PreviewTheme {
        CharacterListContent(
            uiState = CharacterListUiState(
                searchQuery = "Personaje inexistente",
                isInitialLoading = false,
                isLastPage = true,
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 900)
@Composable
private fun CharacterErrorPreview() {
    PreviewTheme {
        CharacterListContent(
            uiState = CharacterListUiState(
                isInitialLoading = false,
                initialError = CharacterListUiError.Connection,
            ),
        )
    }
}

@Composable
@OptIn(ExperimentalCoilApi::class)
private fun PreviewTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    RickAndMortyAppTheme(darkTheme = darkTheme) {
        val previewImageColor = MaterialTheme.colorScheme.surfaceVariant.toArgb()
        val previewHandler = AsyncImagePreviewHandler { ColorImage(previewImageColor) }
        CompositionLocalProvider(
            LocalAsyncImagePreviewHandler provides previewHandler,
            content = content,
        )
    }
}

private val previewCharacters = listOf(
    previewCharacter(id = 1, name = "Rick Sanchez", status = "Alive", species = "Human"),
    previewCharacter(id = 2, name = "Morty Smith", status = "Alive", species = "Human"),
    previewCharacter(id = 3, name = "Mechanical Morty", status = "Unknown", species = "Robot"),
)

private fun previewCharacter(
    id: Int,
    name: String,
    status: String,
    species: String,
) = Character(
    id = id,
    name = name,
    status = status,
    species = species,
    type = "",
    gender = "Unknown",
    originName = "Earth",
    originUrl = "",
    locationName = "Earth",
    locationUrl = "",
    imageUrl = "https://rickandmortyapi.com/api/character/avatar/$id.jpeg",
    episodeUrls = emptyList(),
    resourceUrl = "",
    createdAt = "",
)

private class FilterVisibilityNestedScrollConnection(
    private val thresholdPx: Float,
    private val onShowFilters: () -> Unit,
    private val onHideFilters: () -> Unit,
) : NestedScrollConnection {
    private var accumulatedDelta = 0f
    private var previousDirection = 0

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput || available.y == 0f) return Offset.Zero

        val currentDirection = if (available.y > 0f) 1 else -1
        if (currentDirection != previousDirection) {
            accumulatedDelta = 0f
            previousDirection = currentDirection
        }
        accumulatedDelta += available.y

        when {
            accumulatedDelta <= -thresholdPx -> {
                accumulatedDelta = 0f
                onHideFilters()
            }

            accumulatedDelta >= thresholdPx -> {
                accumulatedDelta = 0f
                onShowFilters()
            }
        }
        return Offset.Zero
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        accumulatedDelta = 0f
        return Velocity.Zero
    }
}

private val TABLET_WIDTH = 600.dp
private val MAX_CONTENT_WIDTH = 840.dp
private val FILTER_VISIBILITY_THRESHOLD = 36.dp
private const val FILTER_ANIMATION_MILLIS = 220
private const val PAGINATION_PREFETCH_DISTANCE = 3
private const val FILTER_PANEL_TEST_TAG = "character_filter_panel"
private const val FILTER_TOGGLE_TEST_TAG = "character_filter_toggle"
private const val SEARCH_FIELD_TEST_TAG = "character_search_field"
private const val CHARACTER_LIST_TEST_TAG = "character_list"
private const val LOGOUT_BUTTON_TEST_TAG = "character_logout_button"
