package com.example.rickandmorty.ui.characters.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.rickandmorty.R

@Composable
fun CharacterStatusBadge(
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
