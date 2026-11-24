package com.example.rickandmorty.ui.characters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.ColorImage
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import com.example.rickandmorty.R
import com.example.rickandmorty.domain.model.Character
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
        onLoadNextPage = viewModel::loadNextPage,
        onRetryInitialLoad = viewModel::retryInitialLoad,
        onRetryPagination = viewModel::retryPagination,
        onLogout = onLogout,
        modifier = modifier,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CharacterListContent(
    uiState: CharacterListUiState,
    onLoadNextPage: () -> Unit,
    onRetryInitialLoad: () -> Unit,
    onRetryPagination: () -> Unit,
    modifier: Modifier = Modifier,
    onLogout: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.character_list_title),
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text(stringResource(R.string.auth_logout))
                    }
                },
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
            val contentModifier = Modifier
                .widthIn(max = MAX_CONTENT_WIDTH)
                .fillMaxSize()
                .padding(horizontal = horizontalPadding)

            when {
                uiState.isInitialLoading && uiState.characters.isEmpty() -> InitialLoadingState(
                    modifier = contentModifier,
                )

                uiState.initialError != null && uiState.characters.isEmpty() -> InitialErrorState(
                    error = uiState.initialError,
                    onRetry = onRetryInitialLoad,
                    modifier = contentModifier,
                )

                uiState.characters.isEmpty() -> EmptyState(modifier = contentModifier)

                else -> CharacterList(
                    uiState = uiState,
                    onLoadNextPage = onLoadNextPage,
                    onRetryPagination = onRetryPagination,
                    modifier = contentModifier,
                )
            }
        }
    }
}

@Composable
private fun CharacterList(
    uiState: CharacterListUiState,
    onLoadNextPage: () -> Unit,
    onRetryPagination: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    PaginationEffect(
        listState = listState,
        itemCount = uiState.characters.size,
        canLoadMore = !uiState.isLoadingMore &&
            !uiState.isLastPage &&
            uiState.paginationError == null,
        onLoadNextPage = onLoadNextPage,
    )

    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
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
    val loadingText = stringResource(R.string.character_initial_loading)
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        CircularProgressIndicator()
        Text(
            text = loadingText,
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
private fun EmptyState(modifier: Modifier = Modifier) {
    MessageState(
        title = stringResource(R.string.character_empty_title),
        message = stringResource(R.string.character_empty_message),
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
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
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
            Button(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun PaginationLoadingState(modifier: Modifier = Modifier) {
    RowLoadingState(
        text = stringResource(R.string.character_pagination_loading),
        modifier = modifier,
    )
}

@Composable
private fun RowLoadingState(
    text: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator()
        Text(
            text = text,
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
            Text(
                text = stringResource(R.string.character_pagination_error_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = errorMessage(error),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
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
private fun errorMessage(error: CharacterListUiError): String = when (error) {
    CharacterListUiError.Connection -> stringResource(R.string.character_error_connection)
    CharacterListUiError.Serialization -> stringResource(R.string.character_error_serialization)
    is CharacterListUiError.Http -> stringResource(R.string.character_error_http, error.statusCode)
}

@Preview(showBackground = true, widthDp = 390, heightDp = 780)
@Composable
private fun CharacterListPreview() {
    PreviewTheme {
        CharacterListContent(
            uiState = CharacterListUiState(
                characters = previewCharacters,
                isInitialLoading = false,
                isLastPage = true,
            ),
            onLoadNextPage = {},
            onRetryInitialLoad = {},
            onRetryPagination = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 700, heightDp = 500)
@Composable
private fun CharacterListTabletDarkPreview() {
    PreviewTheme(darkTheme = true) {
        CharacterListContent(
            uiState = CharacterListUiState(
                characters = previewCharacters,
                isInitialLoading = false,
                isLastPage = true,
            ),
            onLoadNextPage = {},
            onRetryInitialLoad = {},
            onRetryPagination = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 780)
@Composable
private fun CharacterPaginationErrorPreview() {
    PreviewTheme {
        CharacterListContent(
            uiState = CharacterListUiState(
                characters = previewCharacters,
                isInitialLoading = false,
                paginationError = CharacterListUiError.Connection,
            ),
            onLoadNextPage = {},
            onRetryInitialLoad = {},
            onRetryPagination = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 780)
@Composable
private fun CharacterLoadingPreview() {
    PreviewTheme {
        CharacterListContent(
            uiState = CharacterListUiState(),
            onLoadNextPage = {},
            onRetryInitialLoad = {},
            onRetryPagination = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 780)
@Composable
private fun CharacterErrorPreview() {
    PreviewTheme {
        CharacterListContent(
            uiState = CharacterListUiState(
                isInitialLoading = false,
                initialError = CharacterListUiError.Connection,
            ),
            onLoadNextPage = {},
            onRetryInitialLoad = {},
            onRetryPagination = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 780)
@Composable
private fun CharacterEmptyPreview() {
    PreviewTheme {
        CharacterListContent(
            uiState = CharacterListUiState(isInitialLoading = false),
            onLoadNextPage = {},
            onRetryInitialLoad = {},
            onRetryPagination = {},
        )
    }
}

@Composable
@OptIn(ExperimentalCoilApi::class)
private fun PreviewTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    RickAndMortyAppTheme(
        darkTheme = darkTheme,
        dynamicColor = false,
    ) {
        val previewImageColor = MaterialTheme.colorScheme.surfaceVariant.toArgb()
        val previewHandler = AsyncImagePreviewHandler {
            ColorImage(previewImageColor)
        }
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

private val TABLET_WIDTH = 600.dp
private val MAX_CONTENT_WIDTH = 760.dp
private const val PAGINATION_PREFETCH_DISTANCE = 3
