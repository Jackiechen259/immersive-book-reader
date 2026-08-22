package com.immersive.reader.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.immersive.reader.core.datastore.ReaderPreferences
import com.immersive.reader.core.model.ReadingMode
import com.immersive.reader.core.model.ThemeMode
import com.immersive.reader.core.model.ExitPolicy
import com.immersive.reader.session.ReadingSessionState
import java.util.concurrent.TimeUnit
import android.os.SystemClock
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
    onOpenSettings: () -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onLineHeightChange: (Float) -> Unit,
    onReadingModeChange: (ReadingMode) -> Unit,
) {
    var settingsVisible by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        if (controlsVisible || errorMessage != null) {
            Surface(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                color = Color.Black.copy(alpha = 0.68f),
                contentColor = Color.White,
            ) {
                Row(
                    modifier = Modifier.statusBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IconButton(onClick = onRequestExit) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Close reader") }
                    Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    TextButton(onClick = { settingsVisible = true; onOpenSettings() }) { Text("Aa", color = Color.White) }
                }
            }
        }

        if (controlsVisible && errorMessage == null) {
            Surface(
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                color = Color.Black.copy(alpha = 0.68f),
                contentColor = Color.White,
            ) {
                Row(
                    modifier = Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        sessionRemainingMillis?.let { "${formatDuration(it)} remaining" }
                            ?: sessionElapsedMillis?.let { formatDuration(it) }
                            ?: "Reading",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f),
                    )
                    progression?.let { Text("${(it * 100).toInt()}%", style = MaterialTheme.typography.labelLarge) }
                    TextButton(onClick = { settingsVisible = true; onOpenSettings() }) { Text("Preferences", color = Color.White) }
                }
            }
        }

        when {
            loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.White)
            errorMessage != null -> Surface(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                    Text(errorMessage)
                }
            }
        }
    }

    if (settingsVisible) {
        ModalBottomSheet(onDismissRequest = { settingsVisible = false }) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Reading preferences", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("Font size", style = MaterialTheme.typography.labelLarge)
                Slider(value = preferences.fontSize, onValueChange = onFontSizeChange, valueRange = 0.8f..1.5f, steps = 6)
                Text("Line height", style = MaterialTheme.typography.labelLarge)
                Slider(value = preferences.lineHeight, onValueChange = onLineHeightChange, valueRange = 1.2f..2f, steps = 7)
                Text("Theme", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(selected = preferences.theme == mode, onClick = { onThemeChange(mode) }, label = { Text(mode.name.lowercase().replaceFirstChar(Char::uppercase)) })
                    }
                }
                Text("Reading mode", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReadingMode.entries.forEach { mode ->
                        FilterChip(selected = preferences.readingMode == mode, onClick = { onReadingModeChange(mode) }, label = { Text(mode.name.lowercase().replaceFirstChar(Char::uppercase)) })
                    }
                }
            }
        }
    }

    exitDialog?.let { dialog ->
        ModalBottomSheet(onDismissRequest = onDismissExit) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (dialog.emergency) {
                    Text("Emergency exit", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("Use this only when you genuinely need to leave the reading session. It will be marked as interrupted.")
                    HoldToExitButton(
                        durationMillis = EMERGENCY_HOLD_MILLIS,
                        label = "Hold 5 seconds to exit",
                        onComplete = { onEndSession(true) },
                    )
                } else if (dialog.active.session.exitPolicy == ExitPolicy.TIME_LOCKED) {
                    Text("Focus session in progress", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("Remaining ${formatDuration(dialog.active.remainingMillis ?: 0L)}")
                    Text("This session will unlock automatically when the timer ends.")
                    Button(onClick = onDismissExit, modifier = Modifier.fillMaxWidth()) { Text("Return to reading") }
                    TextButton(onClick = onRequestEmergencyExit, modifier = Modifier.fillMaxWidth()) { Text("Emergency exit") }
                } else {
                    Text("End this reading?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("You have focused for ${formatDuration(dialog.active.elapsedMillis)}. The session time will be saved.")
                    if (dialog.active.session.exitPolicy == ExitPolicy.CONFIRM) {
                        Button(onClick = { onEndSession(false) }, modifier = Modifier.fillMaxWidth()) { Text("End session") }
                    } else {
                        HoldToExitButton(
                            durationMillis = preferences.exitHoldDurationSeconds * 1_000L,
                            label = "Hold ${preferences.exitHoldDurationSeconds} seconds to end",
                            onComplete = { onEndSession(false) },
                        )
                    }
                    TextButton(onClick = onDismissExit, modifier = Modifier.fillMaxWidth()) { Text("Continue reading") }
                }
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
        modifier = Modifier.fillMaxWidth().pointerInput(durationMillis) {
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

private fun formatDuration(milliseconds: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds).coerceAtLeast(0L)
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%02d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
