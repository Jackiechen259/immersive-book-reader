package com.immersive.reader.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.immersive.reader.core.datastore.ReaderPreferences
import com.immersive.reader.core.model.ReadingMode
import com.immersive.reader.core.model.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferences: ReaderPreferences,
    onBack: () -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onLineHeightChange: (Float) -> Unit,
    onReadingModeChange: (ReadingMode) -> Unit,
    onKeepScreenAwakeChange: (Boolean) -> Unit,
    onUseDndChange: (Boolean) -> Unit,
) {
    Scaffold(topBar = {
        TopAppBar(title = { Text("Settings") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Reading", style = MaterialTheme.typography.titleLarge)
            SettingRow(icon = { Icon(Icons.Default.DarkMode, null) }, title = "Theme", subtitle = "Choose the atmosphere for your page") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode -> FilterChip(selected = preferences.theme == mode, onClick = { onThemeChange(mode) }, label = { Text(mode.name.lowercase().replaceFirstChar(Char::uppercase)) }) }
                }
            }
            SettingRow(icon = { Icon(Icons.Default.FormatSize, null) }, title = "Font size", subtitle = "${(preferences.fontSize * 100).toInt()}%") {
                Slider(value = preferences.fontSize, onValueChange = onFontSizeChange, valueRange = 0.8f..1.5f, steps = 6)
            }
            SettingRow(icon = { Icon(Icons.Default.Straighten, null) }, title = "Line height", subtitle = "${"%.1f".format(preferences.lineHeight)}×") {
                Slider(value = preferences.lineHeight, onValueChange = onLineHeightChange, valueRange = 1.2f..2f, steps = 7)
            }
            SettingRow(title = "Reading mode", subtitle = "How pages flow in the reader") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReadingMode.entries.forEach { mode -> FilterChip(selected = preferences.readingMode == mode, onClick = { onReadingModeChange(mode) }, label = { Text(mode.name.lowercase().replaceFirstChar(Char::uppercase)) }) }
                }
            }
            HorizontalDivider()
            Text("Focus", style = MaterialTheme.typography.titleLarge)
            ToggleRow(title = "Keep screen awake", subtitle = "Respect the display timeout by default.", checked = preferences.keepScreenAwake, onCheckedChange = onKeepScreenAwakeChange)
            ToggleRow(title = "Use Do Not Disturb", subtitle = "Requires Android notification policy access.", checked = preferences.useDnd, onCheckedChange = onUseDndChange)
            Spacer(Modifier.height(24.dp))
            Text("Deep Focus becomes available only when this app is provisioned as a Device Owner. Standard Focus remains safe and reversible on regular devices.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingRow(icon: @Composable (() -> Unit)? = null, title: String, subtitle: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon?.invoke()
            Column(Modifier.padding(start = if (icon == null) 0.dp else 12.dp)) { Text(title); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        content()
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
