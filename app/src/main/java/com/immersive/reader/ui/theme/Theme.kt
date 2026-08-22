package com.immersive.reader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.immersive.reader.core.model.ThemeMode

private val Ink = Color(0xFF25312E)
private val Moss = Color(0xFF355B4F)
private val Paper = Color(0xFFF8F7F2)
private val Sepia = Color(0xFFF2E8D5)

@Composable
fun ImmersiveReaderTheme(
    themeMode: ThemeMode = ThemeMode.LIGHT,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = themeMode == ThemeMode.DARK || (themeMode == ThemeMode.LIGHT && systemDark)
    val scheme = if (dark) {
        darkColorScheme(
            primary = Color(0xFF9AC9B7),
            onPrimary = Color(0xFF07382B),
            background = Color(0xFF18201D),
            surface = Color(0xFF202925),
            onBackground = Color(0xFFE3E9E4),
            onSurface = Color(0xFFE3E9E4),
        )
    } else {
        lightColorScheme(
            primary = Moss,
            onPrimary = Color.White,
            background = if (themeMode == ThemeMode.SEPIA) Sepia else Paper,
            surface = if (themeMode == ThemeMode.SEPIA) Color(0xFFF9F1E3) else Color.White,
            onBackground = Ink,
            onSurface = Ink,
        )
    }

    MaterialTheme(colorScheme = scheme, content = content)
}
