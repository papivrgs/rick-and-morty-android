package com.example.rickandmorty.ui.characters.components

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.rickandmorty.R
import com.example.rickandmorty.domain.model.Character
import com.example.rickandmorty.ui.theme.RickAndMortyAppTheme

@Composable
fun CharacterCard(
    character: Character,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            AsyncImage(
                model = character.imageUrl,
                contentDescription = stringResource(
                    R.string.character_image_description,
                    character.name,
                ),
                modifier = Modifier
                    .size(108.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = character.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                CharacterStatus(status = character.status)
                Text(
                    text = stringResource(R.string.character_species, character.species),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CharacterStatus(
    status: String,
    modifier: Modifier = Modifier,
) {
    val normalizedStatus = when (status.lowercase()) {
        "alive" -> stringResource(R.string.character_status_alive)
        "dead" -> stringResource(R.string.character_status_dead)
        else -> stringResource(R.string.character_status_unknown)
    }
    val indicatorColor = when (status.lowercase()) {
        "alive" -> MaterialTheme.colorScheme.primary
        "dead" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val containerColor = when (status.lowercase()) {
        "alive" -> MaterialTheme.colorScheme.primaryContainer
        "dead" -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when (status.lowercase()) {
        "alive" -> MaterialTheme.colorScheme.onPrimaryContainer
        "dead" -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier.semantics(mergeDescendants = true) {},
        shape = RoundedCornerShape(50),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .background(indicatorColor, CircleShape),
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = stringResource(R.string.character_status, normalizedStatus),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 420)
@Composable
private fun CharacterCardPreview() {
    RickAndMortyAppTheme(dynamicColor = false) {
        CharacterCard(
            character = previewCharacter,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 420, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CharacterCardDarkPreview() {
    RickAndMortyAppTheme(dynamicColor = false) {
        CharacterCard(
            character = previewCharacter,
            modifier = Modifier.padding(16.dp),
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
    locationName = "Earth",
    locationUrl = "",
    imageUrl = "https://rickandmortyapi.com/api/character/avatar/1.jpeg",
    episodeUrls = emptyList(),
    resourceUrl = "",
    createdAt = "2017-11-04T18:48:46.250Z",
)
