package com.immersive.reader.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.immersive.reader.R
import com.immersive.reader.core.datastore.ReaderPreferences
import com.immersive.reader.core.model.ReadingMode
import com.immersive.reader.core.model.ThemeMode
import com.immersive.reader.core.model.ExitPolicy
import com.immersive.reader.session.ReadingSessionState
import com.immersive.reader.ui.components.formatClock
import com.immersive.reader.ui.components.formatProgressPercent
import com.immersive.reader.ui.components.label
import com.immersive.reader.ui.components.localizedReadingDuration
import android.os.SystemClock
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.withFrameNanos

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderOverlay(
    title: String,
    loading: Boolean,
    errorMessage: String?,
    controlsVisible: Boolean,
    progression: Double?,
    preferences: ReaderPreferences,
    sessionElapsedMillis: Long?,
    sessionRemainingMillis: Long?,
    exitDialog: ReaderExitDialogState?,
    onRequestExit: () -> Unit,
    onDismissExit: () -> Unit,
    onRequestEmergencyExit: () -> Unit,
    onEndSession: (Boolean) -> Unit,
    onLeaveReader: () -> Unit,
    onOpenSettings: () -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onLineHeightChange: (Float) -> Unit,
    onReadingModeChange: (ReadingMode) -> Unit,
) {
    var settingsVisible by remember { mutableStateOf(false) }
    val chrome = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    val chromeContent = MaterialTheme.colorScheme.onSurface

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = controlsVisible || errorMessage != null,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(180)),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = chrome,
                contentColor = chromeContent,
            ) {
                Row(
                    modifier = Modifier.statusBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IconButton(onClick = onRequestExit) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.close_reader))
                    }
                    Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    TextButton(onClick = { settingsVisible = true; onOpenSettings() }) { Text("Aa") }
                }
            }
        }

        AnimatedVisibility(
            visible = controlsVisible && errorMessage == null,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(180)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = chrome,
                contentColor = chromeContent,
            ) {
                Column(modifier = Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            sessionRemainingMillis?.let { stringResource(R.string.reader_remaining, formatClock(it)) }
                                ?: sessionElapsedMillis?.let { formatClock(it) }
                                ?: stringResource(R.string.reading),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.weight(1f),
                        )
                        progression?.let {
                            Text(formatProgressPercent(it), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    progression?.let {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { it.toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                }
            }
        }

        when {
            loading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary,
            )
            errorMessage != null -> Surface(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                        Text(errorMessage)
                    }
                    Button(onClick = onLeaveReader, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.back_to_library))
                    }
                }
            }
        }
    }

    if (settingsVisible) {
        ModalBottomSheet(onDismissRequest = { settingsVisible = false }) {
            Column(
                modifier = Modifier.navigationBarsPadding().padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(R.string.reading_preferences),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(stringResource(R.string.font_size), style = MaterialTheme.typography.labelLarge)
                Slider(value = preferences.fontSize, onValueChange = onFontSizeChange, valueRange = 0.8f..1.5f, steps = 6)
                Text(stringResource(R.string.line_height), style = MaterialTheme.typography.labelLarge)
                Slider(value = preferences.lineHeight, onValueChange = onLineHeightChange, valueRange = 1.2f..2f, steps = 7)
                Text(stringResource(R.string.theme), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = preferences.theme == mode,
                            onClick = { onThemeChange(mode) },
                            label = { Text(mode.label()) },
                        )
                    }
                }
                Text(stringResource(R.string.reading_mode), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReadingMode.entries.forEach { mode ->
                        FilterChip(
                            selected = preferences.readingMode == mode,
                            onClick = { onReadingModeChange(mode) },
                            label = { Text(mode.label()) },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    exitDialog?.let { dialog ->
        ModalBottomSheet(onDismissRequest = onDismissExit) {
            Column(
                modifier = Modifier.navigationBarsPadding().padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (dialog.emergency) {
                    Text(
                        stringResource(R.string.emergency_exit),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(stringResource(R.string.emergency_exit_body))
                    HoldToExitButton(
                        durationMillis = EMERGENCY_HOLD_MILLIS,
                        label = stringResource(R.string.hold_five_seconds_to_exit),
                        onComplete = { onEndSession(true) },
                    )
                } else if (dialog.active.session.exitPolicy == ExitPolicy.TIME_LOCKED) {
                    Text(
                        stringResource(R.string.focus_session_in_progress),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(stringResource(R.string.remaining_clock, formatClock(dialog.active.remainingMillis ?: 0L)))
                    Text(stringResource(R.string.session_unlocks_when_timer_ends))
                    Button(onClick = onDismissExit, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.return_to_reading))
                    }
                    TextButton(onClick = onRequestEmergencyExit, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.emergency_exit))
                    }
                } else {
                    Text(
                        stringResource(R.string.end_this_reading),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        stringResource(
                            R.string.focused_time_will_be_saved,
                            localizedReadingDuration(dialog.active.elapsedMillis),
                        ),
                    )
                    if (dialog.active.session.exitPolicy == ExitPolicy.CONFIRM) {
                        Button(onClick = { onEndSession(false) }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.end_session))
                        }
                    } else {
                        HoldToExitButton(
                            durationMillis = preferences.exitHoldDurationSeconds * 1_000L,
                            label = pluralStringResource(
                                R.plurals.hold_seconds_to_end,
                                preferences.exitHoldDurationSeconds,
                                preferences.exitHoldDurationSeconds,
                            ),
                            onComplete = { onEndSession(false) },
                        )
                    }
                    TextButton(onClick = onDismissExit, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.continue_reading))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

data class ReaderExitDialogState(
    val active: ReadingSessionState.Active,
    val emergency: Boolean = false,
)

@Composable
private fun HoldToExitButton(
    durationMillis: Long,
    label: String,
    onComplete: () -> Unit,
) {
    var holding by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(holding, durationMillis) {
        if (!holding) {
            progress = 0f
            return@LaunchedEffect
        }
        val start = SystemClock.elapsedRealtime()
        while (holding) {
            progress = ((SystemClock.elapsedRealtime() - start).toFloat() / durationMillis).coerceIn(0f, 1f)
            if (progress >= 1f) {
                holding = false
                onComplete()
                break
            }
            withFrameNanos { }
        }
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .pointerInput(durationMillis) {
                detectTapGestures(
                    onPress = {
                        holding = true
                        tryAwaitRelease()
                        holding = false
                    },
                )
            },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, fontWeight = FontWeight.SemiBold)
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        }
    }
}

private const val EMERGENCY_HOLD_MILLIS = 5_000L
