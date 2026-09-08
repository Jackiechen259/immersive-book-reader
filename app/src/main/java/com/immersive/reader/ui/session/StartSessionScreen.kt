package com.immersive.reader.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.immersive.reader.core.model.TimerMode
import com.immersive.reader.ui.components.BookCover
import com.immersive.reader.ui.components.ReadingProgress
import com.immersive.reader.ui.components.displayAuthor

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StartSessionScreen(
    bookId: String,
    onBack: () -> Unit,
    onStartReading: (String) -> Unit,
    viewModel: StartSessionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val book by viewModel.book.collectAsStateWithLifecycle()
    LaunchedEffect(bookId) { viewModel.loadBook(bookId) }

    val canStart = !uiState.starting &&
        book != null &&
        (uiState.timerMode == TimerMode.OPEN_ENDED || uiState.hours * 60 + uiState.minutes > 0)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                title = { Text("Prepare to read") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    BookCover(
                        title = book?.title ?: "Book",
                        coverPath = book?.coverPath,
                        showFallbackTitle = false,
                        modifier = Modifier.width(80.dp).height(112.dp),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                        Text(
                            book?.title ?: "Opening book…",
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            book?.displayAuthor() ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        ReadingProgress(book?.progression)
                    }
                }
                Text("How long would you like to read?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = uiState.timerMode == TimerMode.OPEN_ENDED,
                        onClick = { viewModel.setTimerMode(TimerMode.OPEN_ENDED) },
                        label = { Text("Unlimited") },
                    )
                    FilterChip(
                        selected = uiState.timerMode == TimerMode.COUNTDOWN,
                        onClick = { viewModel.setTimerMode(TimerMode.COUNTDOWN) },
                        label = { Text("Timed") },
                    )
                }
                if (uiState.timerMode == TimerMode.OPEN_ENDED) {
                    Text(
                        "No time limit. Your reading time will be recorded until you decide to end the session.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = uiState.hours.toString().padStart(2, '0'),
                            onValueChange = { viewModel.setHours(it.filter(Char::isDigit).take(2).toIntOrNull() ?: 0) },
                            label = { Text("Hours") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        Text(":", style = MaterialTheme.typography.headlineSmall)
                        OutlinedTextField(
                            value = uiState.minutes.toString().padStart(2, '0'),
                            onValueChange = { viewModel.setMinutes(it.filter(Char::isDigit).take(2).toIntOrNull() ?: 0) },
                            label = { Text("Minutes") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(15 to "15m", 30 to "30m", 45 to "45m", 60 to "1h", 120 to "2h").forEach { (value, label) ->
                            FilterChip(
                                selected = uiState.hours * 60 + uiState.minutes == value,
                                onClick = { viewModel.setHours(value / 60); viewModel.setMinutes(value % 60) },
                                label = { Text(label) },
                            )
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Lock reading environment", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Hide system chrome and add a clear exit boundary.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = uiState.lockEnvironment, onCheckedChange = viewModel::setLockEnvironment)
                }
                if (uiState.timerMode == TimerMode.COUNTDOWN && viewModel.focusCapabilities.isDeviceOwner) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Deep Focus", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Stay in the book until the timer ends.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = uiState.deepFocus,
                            onCheckedChange = viewModel::setDeepFocus,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            uiState.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
            }
            Button(
                onClick = { viewModel.start(bookId, onStarted = onStartReading) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                enabled = canStart,
            ) {
                if (uiState.starting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Starting…")
                } else {
                    Text("Start reading")
                }
            }
        }
    }
}
