package com.example.rickandmorty.ui.characters.detail

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.ColorImage
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import com.example.rickandmorty.R
import com.example.rickandmorty.domain.model.Character
import com.example.rickandmorty.domain.model.Episode
import com.example.rickandmorty.ui.characters.CharacterListUiError
import com.example.rickandmorty.ui.characters.components.CharacterStatusBadge
import com.example.rickandmorty.ui.theme.RickAndMortyAppTheme

@Composable
fun CharacterDetailScreen(
    viewModel: CharacterDetailViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CharacterDetailContent(
        uiState = uiState,
        onBack = onBack,
        onRetryCharacter = viewModel::retryCharacter,
        onRetryEpisodes = viewModel::retryEpisodes,
        modifier = modifier,
    )
}

@Composable
fun CharacterDetailContent(
    uiState: CharacterDetailUiState,
    onBack: () -> Unit,
    onRetryCharacter: () -> Unit = {},
    onRetryEpisodes: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { CharacterDetailHeader(onBack = onBack) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            when {
                uiState.isCharacterLoading -> DetailLoadingState()
                uiState.characterError != null -> DetailErrorState(
                    error = uiState.characterError,
                    onRetry = onRetryCharacter,
                )

                uiState.character != null -> DetailBody(
                    character = uiState.character,
                    episodes = uiState.episodes,
                    isEpisodesLoading = uiState.isEpisodesLoading,
                    episodeError = uiState.episodeError,
                    onRetryEpisodes = onRetryEpisodes,
                )
            }
        }
    }
}

@Composable
private fun CharacterDetailHeader(onBack: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.character_detail_back),
                )
            }
            PortalGlyph(modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.character_detail_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun DetailBody(
    character: Character,
    episodes: List<Episode>,
    isEpisodesLoading: Boolean,
    episodeError: CharacterListUiError?,
    onRetryEpisodes: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 840.dp)
            .testTag("character_detail_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "hero") { CharacterHero(character) }
        item(key = "general") { GeneralInformation(character) }
        item(key = "location") { LocationInformation(character.locationName) }
        item(key = "episodes_header") {
            SectionTitle(
                title = stringResource(R.string.character_detail_episodes),
                subtitle = stringResource(R.string.character_detail_episodes_subtitle),
            )
        }
        when {
            isEpisodesLoading -> item(key = "episodes_loading") {
                EpisodeLoadingState()
            }

            episodeError != null -> item(key = "episodes_error") {
                EpisodeErrorState(
                    error = episodeError,
                    onRetry = onRetryEpisodes,
                )
            }

            episodes.isEmpty() -> item(key = "episodes_empty") {
                EpisodeEmptyState()
            }

            else -> items(episodes, key = Episode::id) { episode ->
                EpisodeCard(episode)
            }
        }
    }
}

@Composable
private fun CharacterHero(character: Character) {
    Card(
        modifier = Modifier.testTag("character_detail_marker_${character.id}"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column {
            AsyncImage(
                model = character.imageUrl,
                contentDescription = stringResource(R.string.character_image_description, character.name),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.35f)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
                contentScale = ContentScale.Crop,
            )
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = character.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                )
                CharacterStatusBadge(status = character.status)
            }
        }
    }
}

@Composable
private fun GeneralInformation(character: Character) {
    InformationCard(
        title = stringResource(R.string.character_detail_general_information),
    ) {
        InformationRow(stringResource(R.string.character_detail_species), character.species)
        InformationRow(stringResource(R.string.character_detail_gender), character.gender)
        if (character.type.isNotBlank()) {
            InformationRow(stringResource(R.string.character_detail_type), character.type)
        }
        InformationRow(stringResource(R.string.character_detail_origin), character.originName)
    }
}

@Composable
private fun LocationInformation(location: String) {
    InformationCard(title = stringResource(R.string.character_detail_last_location)) {
        Text(
            text = location,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun InformationCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionTitle(title = title)
            content()
        }
    }
}

@Composable
private fun InformationRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.42f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.58f),
        )
    }
}

@Composable
private fun SectionTitle(
    title: String,
    subtitle: String? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EpisodeCard(episode: Episode) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Text(
                    text = episode.code,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = episode.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun DetailLoadingState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator()
        Text(stringResource(R.string.character_detail_loading))
    }
}

@Composable
private fun DetailErrorState(
    error: CharacterListUiError,
    onRetry: () -> Unit,
) {
    DetailMessage(
        title = stringResource(R.string.character_detail_error_title),
        message = detailErrorMessage(error),
        actionLabel = stringResource(R.string.character_retry),
        onAction = onRetry,
    )
}

@Composable
private fun EpisodeLoadingState() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
        Text(stringResource(R.string.character_detail_episodes_loading))
    }
}

@Composable
private fun EpisodeErrorState(
    error: CharacterListUiError,
    onRetry: () -> Unit,
) {
    DetailMessage(
        title = stringResource(R.string.character_detail_episodes_error_title),
        message = detailErrorMessage(error),
        actionLabel = stringResource(R.string.character_retry),
        onAction = onRetry,
        compact = true,
    )
}

@Composable
private fun EpisodeEmptyState() {
    DetailMessage(
        title = stringResource(R.string.character_detail_episodes_empty_title),
        message = stringResource(R.string.character_detail_episodes_empty_message),
        compact = true,
    )
}

@Composable
private fun DetailMessage(
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    compact: Boolean = false,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(if (compact) 20.dp else 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PortalGlyph(modifier = Modifier.size(if (compact) 36.dp else 52.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
private fun PortalGlyph(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        drawCircle(
            color = color.copy(alpha = 0.2f),
            radius = size.minDimension * 0.42f,
            center = center,
            style = Stroke(width = size.minDimension * 0.12f),
        )
        drawCircle(
            color = color,
            radius = size.minDimension * 0.14f,
            center = Offset(center.x, center.y),
        )
    }
}

@Composable
private fun detailErrorMessage(error: CharacterListUiError): String = when (error) {
    CharacterListUiError.Connection -> stringResource(R.string.character_error_connection)
    CharacterListUiError.Serialization -> stringResource(R.string.character_error_serialization)
    is CharacterListUiError.Http -> stringResource(R.string.character_error_http, error.statusCode)
}

@Preview(showBackground = true, widthDp = 390, heightDp = 900)
@Composable
private fun CharacterDetailPreview() {
    DetailPreviewTheme {
        CharacterDetailContent(
            uiState = CharacterDetailUiState(
                character = previewCharacter,
                episodes = previewEpisodes,
                isCharacterLoading = false,
            ),
            onBack = {},
        )
    }
}

@Preview(
    showBackground = true,
    widthDp = 700,
    heightDp = 700,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun CharacterDetailDarkPreview() {
    DetailPreviewTheme(darkTheme = true) {
        CharacterDetailContent(
            uiState = CharacterDetailUiState(
                character = previewCharacter,
                episodeError = CharacterListUiError.Connection,
                isCharacterLoading = false,
            ),
            onBack = {},
        )
    }
}

@Composable
@OptIn(ExperimentalCoilApi::class)
private fun DetailPreviewTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    RickAndMortyAppTheme(darkTheme = darkTheme) {
        val imageColor = MaterialTheme.colorScheme.surfaceVariant.toArgb()
        CompositionLocalProvider(
            LocalAsyncImagePreviewHandler provides AsyncImagePreviewHandler { ColorImage(imageColor) },
            content = content,
        )
    }
}

private val previewCharacter = Character(
    id = 1,
    name = "Rick Sanchez",
    status = "Alive",
    species = "Human",
    type = "",
    gender = "Male",
    originName = "Earth (C-137)",
    originUrl = "",
    locationName = "Citadel of Ricks",
    locationUrl = "",
    imageUrl = "https://rickandmortyapi.com/api/character/avatar/1.jpeg",
    episodeUrls = emptyList(),
    resourceUrl = "",
    createdAt = "",
)

private val previewEpisodes = listOf(
    Episode(1, "Pilot", "S01E01"),
    Episode(2, "Lawnmower Dog", "S01E02"),
    Episode(3, "Anatomy Park", "S01E03"),
)
