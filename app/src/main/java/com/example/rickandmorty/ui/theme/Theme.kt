package com.example.rickandmorty.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PortalGreenLight,
    onPrimary = Color(0xFF00391C),
    primaryContainer = PortalGreenContainerLight,
    onPrimaryContainer = PortalGreenContainerDark,
    secondary = Color(0xFFB6CCBA),
    tertiary = PortalTealLight,
    background = SpaceBackgroundDark,
    onBackground = SpaceOnSurfaceDark,
    surface = SpaceSurfaceDark,
    onSurface = SpaceOnSurfaceDark,
)

private val LightColorScheme = lightColorScheme(
    primary = PortalGreenDark,
    onPrimary = Color.White,
    primaryContainer = PortalGreenContainerDark,
    onPrimaryContainer = Color(0xFF00210E),
    secondary = Color(0xFF4F6353),
    tertiary = PortalTealDark,
    background = SpaceBackgroundLight,
    onBackground = SpaceOnSurfaceLight,
    surface = SpaceSurfaceLight,
    onSurface = SpaceOnSurfaceLight,
)

@Composable
fun RickAndMortyAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
