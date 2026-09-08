package com.immersive.reader.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.immersive.reader.core.datastore.ReaderPreferences
import com.immersive.reader.core.model.ReadingMode
import com.immersive.reader.core.model.ThemeMode
import com.immersive.reader.focus.FocusCapabilities
import com.immersive.reader.ui.ReadingStatistics
import com.immersive.reader.ui.StatisticsViewModel
import com.immersive.reader.ui.components.formatReadingDuration
import com.immersive.reader.ui.theme.ReaderColors

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
    onOpenNotificationPolicySettings: () -> Unit,
    focusCapabilities: FocusCapabilities,
    statisticsViewModel: StatisticsViewModel = hiltViewModel(),
) {
    val statistics by statisticsViewModel.statistics.collectAsStateWithLifecycle()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SettingsGroup(title = "Reading") {
                SettingRow(icon = { Icon(Icons.Default.DarkMode, null) }, title = "Theme", subtitle = "Choose the atmosphere for your page") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = preferences.theme == mode,
                                onClick = { onThemeChange(mode) },
                                leadingIcon = { ThemeDot(mode) },
                                label = { Text(mode.label()) },
                            )
                        }
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
                        ReadingMode.entries.forEach { mode ->
                            FilterChip(
                                selected = preferences.readingMode == mode,
                                onClick = { onReadingModeChange(mode) },
                                label = { Text(mode.label()) },
                            )
                        }
                    }
                }
            }
            SettingsGroup(title = "Focus") {
                ToggleRow(title = "Keep screen awake", subtitle = "Respect the display timeout by default.", checked = preferences.keepScreenAwake, onCheckedChange = onKeepScreenAwakeChange)
                ToggleRow(
                    title = "Use Do Not Disturb",
                    subtitle = if (focusCapabilities.notificationPolicyGranted) {
                        "Notification policy access granted."
                    } else {
                        "Android access is required before DND can be used."
                    },
                    checked = preferences.useDnd,
                    onCheckedChange = { enabled ->
                        if (enabled && !focusCapabilities.notificationPolicyGranted) onOpenNotificationPolicySettings() else onUseDndChange(enabled)
                    },
                )
                if (!focusCapabilities.notificationPolicyGranted) {
                    TextButton(onClick = onOpenNotificationPolicySettings) { Text("Open DND access settings") }
                }
            }
            SettingsGroup(title = "Deep Focus") {
                Text(
                    if (focusCapabilities.isDeviceOwner) {
                        "Available on this managed device."
                    } else {
                        "Not available on this phone. Standard focus still hides system chrome and asks before you leave."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SettingsGroup(title = "Your reading") {
                StatisticsSummary(statistics)
            }
            Text(
                "Immersive Focus remains safe and reversible on regular devices. Screen pinning is reported separately from true Device Owner Lock Task.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = CardDefaults.outlinedCardBorder(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
        }
    }
}

@Composable
private fun StatisticsSummary(statistics: ReadingStatistics) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            formatReadingDuration(statistics.totalMillis),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text("Total reading time", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            StatisticItem("Sessions", statistics.sessionCount.toString(), Modifier.weight(1f))
            StatisticItem("Completed", statistics.completedCount.toString(), Modifier.weight(1f))
            StatisticItem("Interrupted", statistics.interruptedCount.toString(), Modifier.weight(1f))
        }
        if (statistics.perBook.isNotEmpty()) {
            Text("By book", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            val maxMillis = statistics.perBook.maxOf { it.totalMillis }.coerceAtLeast(1L)
            statistics.perBook.take(5).forEach { stat ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stat.title, maxLines = 1, modifier = Modifier.weight(1f).padding(end = 12.dp))
                        Text(formatReadingDuration(stat.totalMillis), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    LinearProgressIndicator(
                        progress = { (stat.totalMillis.toFloat() / maxMillis).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatisticItem(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingRow(icon: @Composable (() -> Unit)? = null, title: String, subtitle: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon?.invoke()
            Column(Modifier.padding(start = if (icon == null) 0.dp else 12.dp)) {
                Text(title)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        content()
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ThemeDot(mode: ThemeMode) {
    val fill = when (mode) {
        ThemeMode.LIGHT -> ReaderColors.Paper
        ThemeMode.SEPIA -> ReaderColors.SepiaPaper
        ThemeMode.DARK -> ReaderColors.Night
    }
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(fill)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
    )
}

private fun ThemeMode.label(): String = when (this) {
    ThemeMode.LIGHT -> "Light"
    ThemeMode.SEPIA -> "Sepia"
    ThemeMode.DARK -> "Dark"
}

private fun ReadingMode.label(): String = when (this) {
    ReadingMode.PAGINATED -> "Pages"
    ReadingMode.SCROLLING -> "Scroll"
}
