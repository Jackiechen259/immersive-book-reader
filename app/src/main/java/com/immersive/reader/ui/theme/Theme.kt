package com.immersive.reader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.immersive.reader.core.model.ThemeMode

private val LightScheme = lightColorScheme(
    primary = ReaderColors.Moss,
    onPrimary = Color.White,
    primaryContainer = ReaderColors.Sage,
    onPrimaryContainer = ReaderColors.Ink,
    secondary = ReaderColors.Sage,
    onSecondary = ReaderColors.Ink,
    tertiary = ReaderColors.GoldSpine,
    onTertiary = ReaderColors.Ink,
    background = ReaderColors.Paper,
    onBackground = ReaderColors.Ink,
    surface = ReaderColors.PaperSurface,
    onSurface = ReaderColors.Ink,
    surfaceVariant = ReaderColors.SurfaceVariantLight,
    onSurfaceVariant = ReaderColors.OnSurfaceVariantLight,
    outline = ReaderColors.OutlineLight,
    error = ReaderColors.Error,
    onError = ReaderColors.OnError,
    errorContainer = ReaderColors.ErrorContainer,
    onErrorContainer = ReaderColors.OnErrorContainer,
)

private val SepiaScheme = lightColorScheme(
    primary = ReaderColors.SepiaMoss,
    onPrimary = Color.White,
    primaryContainer = ReaderColors.SepiaSage,
    onPrimaryContainer = ReaderColors.SepiaInk,
    secondary = ReaderColors.SepiaSage,
    onSecondary = ReaderColors.SepiaInk,
    tertiary = ReaderColors.GoldSpine,
    onTertiary = ReaderColors.SepiaInk,
    background = ReaderColors.SepiaPaper,
    onBackground = ReaderColors.SepiaInk,
    surface = ReaderColors.SepiaSurface,
    onSurface = ReaderColors.SepiaInk,
    surfaceVariant = ReaderColors.SepiaSurfaceVariant,
    onSurfaceVariant = ReaderColors.SepiaOnSurfaceVariant,
    outline = ReaderColors.SepiaOutline,
    error = ReaderColors.Error,
    onError = ReaderColors.OnError,
    errorContainer = ReaderColors.ErrorContainer,
    onErrorContainer = ReaderColors.OnErrorContainer,
)

private val DarkScheme = darkColorScheme(
    primary = ReaderColors.NightMoss,
    onPrimary = ReaderColors.NightOnPrimary,
    primaryContainer = ReaderColors.NightSage,
    onPrimaryContainer = ReaderColors.NightInk,
    secondary = ReaderColors.NightSage,
    onSecondary = ReaderColors.NightInk,
    tertiary = ReaderColors.NightGold,
    onTertiary = ReaderColors.Night,
    background = ReaderColors.Night,
    onBackground = ReaderColors.NightInk,
    surface = ReaderColors.NightSurface,
    onSurface = ReaderColors.NightInk,
    surfaceVariant = ReaderColors.NightSurfaceVariant,
    onSurfaceVariant = ReaderColors.NightOnSurfaceVariant,
    outline = ReaderColors.NightOutline,
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
)

private val ReaderShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun ImmersiveReaderTheme(
    themeMode: ThemeMode = ThemeMode.LIGHT,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val scheme = when (themeMode) {
        ThemeMode.DARK -> DarkScheme
        ThemeMode.SEPIA -> SepiaScheme
        ThemeMode.LIGHT -> if (systemDark) DarkScheme else LightScheme
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = readerTypography(),
        shapes = ReaderShapes,
        content = content,
    )
}
