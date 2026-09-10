package com.immersive.reader.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Language
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.immersive.reader.R
import com.immersive.reader.core.datastore.ReaderPreferences
import com.immersive.reader.core.locale.applyAppLanguage
import com.immersive.reader.core.locale.currentAppLanguage
import com.immersive.reader.core.model.AppLanguage
import com.immersive.reader.core.model.ReadingMode
import com.immersive.reader.core.model.ThemeMode
import com.immersive.reader.focus.FocusCapabilities
import com.immersive.reader.ui.ReadingStatistics
import com.immersive.reader.ui.StatisticsViewModel
import com.immersive.reader.ui.components.label
import com.immersive.reader.ui.components.localizedReadingDuration
import com.immersive.reader.ui.theme.ReaderColors
import com.immersive.reader.ui.update.UpdateSettingsContent
import com.immersive.reader.ui.update.UpdateViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    onAchievementsVisibleChange: (Boolean) -> Unit,
    onOpenAchievements: () -> Unit,
    focusCapabilities: FocusCapabilities,
    onInstallUpdate: () -> Unit,
    updateViewModel: UpdateViewModel,
    statisticsViewModel: StatisticsViewModel = hiltViewModel(),
) {
    val statistics by statisticsViewModel.statistics.collectAsStateWithLifecycle()
    val updateState by updateViewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SettingsGroup(title = stringResource(R.string.settings_group_language)) {
                SettingRow(
                    icon = { Icon(Icons.Default.Language, null) },
                    title = stringResource(R.string.settings_group_language),
                    subtitle = stringResource(R.string.language_subtitle),
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val selectedLanguage = currentAppLanguage()
                        AppLanguage.entries.forEach { language ->
                            FilterChip(
                                selected = selectedLanguage == language,
                                onClick = { applyAppLanguage(language) },
                                label = { Text(language.label()) },
                            )
                        }
                    }
                }
            }
            SettingsGroup(title = stringResource(R.string.settings_group_reading)) {
                SettingRow(
                    icon = { Icon(Icons.Default.DarkMode, null) },
                    title = stringResource(R.string.theme),
                    subtitle = stringResource(R.string.theme_subtitle),
                ) {
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
                SettingRow(
                    icon = { Icon(Icons.Default.FormatSize, null) },
                    title = stringResource(R.string.font_size),
                    subtitle = stringResource(R.string.font_size_percent, (preferences.fontSize * 100).toInt()),
                ) {
                    Slider(value = preferences.fontSize, onValueChange = onFontSizeChange, valueRange = 0.8f..1.5f, steps = 6)
                }
                SettingRow(
                    icon = { Icon(Icons.Default.Straighten, null) },
                    title = stringResource(R.string.line_height),
                    subtitle = stringResource(R.string.line_height_value, "%.1f".format(preferences.lineHeight)),
                ) {
                    Slider(value = preferences.lineHeight, onValueChange = onLineHeightChange, valueRange = 1.2f..2f, steps = 7)
                }
                SettingRow(
                    title = stringResource(R.string.reading_mode),
                    subtitle = stringResource(R.string.reading_mode_subtitle),
                ) {
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
            SettingsGroup(title = stringResource(R.string.settings_group_focus)) {
                ToggleRow(
                    title = stringResource(R.string.keep_screen_awake),
                    subtitle = stringResource(R.string.keep_screen_awake_subtitle),
                    checked = preferences.keepScreenAwake,
                    onCheckedChange = onKeepScreenAwakeChange,
                )
                ToggleRow(
                    title = stringResource(R.string.use_dnd),
                    subtitle = if (focusCapabilities.notificationPolicyGranted) {
                        stringResource(R.string.dnd_granted)
                    } else {
                        stringResource(R.string.dnd_required)
                    },
                    checked = preferences.useDnd,
                    onCheckedChange = { enabled ->
                        if (enabled && !focusCapabilities.notificationPolicyGranted) onOpenNotificationPolicySettings() else onUseDndChange(enabled)
                    },
                )
                if (!focusCapabilities.notificationPolicyGranted) {
                    TextButton(onClick = onOpenNotificationPolicySettings) {
                        Text(stringResource(R.string.open_dnd_settings))
                    }
                }
            }
            SettingsGroup(title = stringResource(R.string.settings_group_deep_focus)) {
                Text(
                    if (focusCapabilities.isDeviceOwner) {
                        stringResource(R.string.deep_focus_available)
                    } else {
                        stringResource(R.string.deep_focus_unavailable)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SettingsGroup(title = stringResource(R.string.settings_group_achievements)) {
                ToggleRow(
                    title = stringResource(R.string.show_achievements),
                    subtitle = stringResource(R.string.show_achievements_subtitle),
                    checked = preferences.achievementsVisible,
                    onCheckedChange = onAchievementsVisibleChange,
                )
                if (preferences.achievementsVisible) {
                    TextButton(onClick = onOpenAchievements) {
                        Text(stringResource(R.string.view_achievements))
                    }
                }
            }
            SettingsGroup(title = stringResource(R.string.settings_group_your_reading)) {
                StatisticsSummary(statistics)
            }
            SettingsGroup(title = stringResource(R.string.settings_group_about)) {
                UpdateSettingsContent(
                    state = updateState,
                    onCheck = updateViewModel::check,
                    onDownload = { updateViewModel.download(installWhenReady = true) },
                    onInstall = onInstallUpdate,
                )
            }
            Text(
                stringResource(R.string.focus_safety_note),
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
            localizedReadingDuration(statistics.totalMillis),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            stringResource(R.string.total_reading_time),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            StatisticItem(stringResource(R.string.stat_sessions), statistics.sessionCount.toString(), Modifier.weight(1f))
            StatisticItem(stringResource(R.string.stat_completed), statistics.completedCount.toString(), Modifier.weight(1f))
            StatisticItem(stringResource(R.string.stat_interrupted), statistics.interruptedCount.toString(), Modifier.weight(1f))
        }
        if (statistics.perBook.isNotEmpty()) {
            Text(stringResource(R.string.stat_by_book), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            val maxMillis = statistics.perBook.maxOf { it.totalMillis }.coerceAtLeast(1L)
            statistics.perBook.take(5).forEach { stat ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stat.title, maxLines = 1, modifier = Modifier.weight(1f).padding(end = 12.dp))
                        Text(localizedReadingDuration(stat.totalMillis), color = MaterialTheme.colorScheme.onSurfaceVariant)
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

