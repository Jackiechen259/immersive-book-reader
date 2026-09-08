package com.immersive.reader.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.immersive.reader.core.model.SessionStatus
import com.immersive.reader.reader.SessionSummaryUiState
import com.immersive.reader.ui.components.BookCover
import com.immersive.reader.ui.components.formatProgressPercent
import com.immersive.reader.ui.components.formatReadingDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionSummaryScreen(
    summary: SessionSummaryUiState?,
    onBackToLibrary: () -> Unit,
    onContinueReading: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                title = { Text("Session summary") },
            )
        },
    ) { padding ->
        if (summary == null) {
            CircularProgressIndicator(modifier = Modifier.fillMaxSize().padding(48.dp).wrapContentSize())
            return@Scaffold
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BookCover(
                title = summary.title,
                coverPath = summary.coverPath,
                showFallbackTitle = false,
                modifier = Modifier.width(96.dp).height(136.dp),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                summary.status.title(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                formatReadingDuration(summary.durationMillis),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                summary.title,
                style = MaterialTheme.typography.titleLarge,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            summary.author?.takeIf { it.isNotBlank() }?.let { author ->
                Spacer(Modifier.height(4.dp))
                Text(author, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(16.dp))
            ProgressChange(summary.startProgression, summary.endProgression)
            Spacer(Modifier.height(36.dp))
            Button(onClick = onContinueReading, modifier = Modifier.fillMaxWidth()) { Text("Continue reading") }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onBackToLibrary, modifier = Modifier.fillMaxWidth()) { Text("Back to library") }
        }
    }
}

@Composable
private fun ProgressChange(start: Double?, end: Double?) {
    val endValue = end ?: start
    if (endValue == null) {
        Text("Progress saved", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "${start?.let(::formatProgressPercent) ?: "—"} → ${formatProgressPercent(endValue)}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LinearProgressIndicator(
            progress = { endValue.toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun SessionStatus.title(): String = when (this) {
    SessionStatus.COMPLETED -> "Session complete"
    SessionStatus.USER_ENDED -> "Session saved"
    SessionStatus.EMERGENCY_EXIT -> "Session interrupted"
    SessionStatus.INTERRUPTED -> "Session interrupted"
    SessionStatus.ACTIVE -> "Reading in progress"
}
