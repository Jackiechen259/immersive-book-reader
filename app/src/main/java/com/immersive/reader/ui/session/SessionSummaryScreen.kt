package com.immersive.reader.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.immersive.reader.core.model.SessionStatus
import com.immersive.reader.reader.SessionSummaryUiState
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionSummaryScreen(
    summary: SessionSummaryUiState?,
    onBackToLibrary: () -> Unit,
    onContinueReading: () -> Unit,
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Session summary") }) }) { padding ->
        if (summary == null) {
            CircularProgressIndicator(modifier = Modifier.fillMaxSize().padding(48.dp).wrapContentSize())
            return@Scaffold
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(summary.status.title(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(18.dp))
            Text(formatDuration(summary.durationMillis), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(14.dp))
            Text(summary.title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            ProgressText(summary.startProgression, summary.endProgression)
            Spacer(Modifier.height(36.dp))
            Button(onClick = onContinueReading, modifier = Modifier.fillMaxWidth()) { Text("Continue reading") }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onBackToLibrary, modifier = Modifier.fillMaxWidth()) { Text("Back to library") }
        }
    }
}

@Composable
private fun ProgressText(start: Double?, end: Double?) {
    val startText = start?.let { "${(it * 100).toInt()}%" } ?: "—"
    val endText = end?.let { "${(it * 100).toInt()}%" } ?: "—"
    Text("Progress: $startText → $endText", color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private fun SessionStatus.title(): String = when (this) {
    SessionStatus.COMPLETED -> "Focus completed"
    SessionStatus.USER_ENDED -> "Session ended early"
    SessionStatus.EMERGENCY_EXIT -> "Session interrupted"
    SessionStatus.INTERRUPTED -> "Session interrupted"
    SessionStatus.ACTIVE -> "Reading in progress"
}

private fun formatDuration(milliseconds: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds).coerceAtLeast(0L)
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}
