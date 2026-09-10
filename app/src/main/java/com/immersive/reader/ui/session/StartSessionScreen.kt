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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.immersive.reader.R
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
                title = { Text(stringResource(R.string.prepare_to_read)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
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
                        title = book?.title ?: stringResource(R.string.book_fallback),
                        coverPath = book?.coverPath,
                        showFallbackTitle = false,
                        modifier = Modifier.width(80.dp).height(112.dp),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                        Text(
                            book?.title ?: stringResource(R.string.opening_book),
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
                Text(
                    stringResource(R.string.how_long_to_read),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = uiState.timerMode == TimerMode.OPEN_ENDED,
                        onClick = { viewModel.setTimerMode(TimerMode.OPEN_ENDED) },
                        label = { Text(stringResource(R.string.timer_unlimited)) },
                    )
                    FilterChip(
                        selected = uiState.timerMode == TimerMode.COUNTDOWN,
                        onClick = { viewModel.setTimerMode(TimerMode.COUNTDOWN) },
                        label = { Text(stringResource(R.string.timer_timed)) },
                    )
                }
                if (uiState.timerMode == TimerMode.OPEN_ENDED) {
                    Text(
                        stringResource(R.string.unlimited_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = uiState.hours.toString().padStart(2, '0'),
                            onValueChange = { viewModel.setHours(it.filter(Char::isDigit).take(2).toIntOrNull() ?: 0) },
                            label = { Text(stringResource(R.string.hours)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        Text(":", style = MaterialTheme.typography.headlineSmall)
                        OutlinedTextField(
                            value = uiState.minutes.toString().padStart(2, '0'),
                            onValueChange = { viewModel.setMinutes(it.filter(Char::isDigit).take(2).toIntOrNull() ?: 0) },
                            label = { Text(stringResource(R.string.minutes)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                    }
                    val durationPresets = listOf(
                        15 to stringResource(R.string.duration_preset_minutes, 15),
                        30 to stringResource(R.string.duration_preset_minutes, 30),
                        45 to stringResource(R.string.duration_preset_minutes, 45),
                        60 to stringResource(R.string.duration_preset_hours, 1),
                        120 to stringResource(R.string.duration_preset_hours, 2),
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        durationPresets.forEach { (value, label) ->
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
                        Text(stringResource(R.string.lock_reading_environment), fontWeight = FontWeight.SemiBold)
                        Text(
                            stringResource(R.string.lock_reading_environment_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = uiState.lockEnvironment, onCheckedChange = viewModel::setLockEnvironment)
                }
                if (uiState.timerMode == TimerMode.COUNTDOWN && viewModel.focusCapabilities.isDeviceOwner) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.deep_focus), fontWeight = FontWeight.SemiBold)
                            Text(
                                stringResource(R.string.deep_focus_hint),
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
                    Text(stringResource(R.string.starting))
                } else {
                    Text(stringResource(R.string.start_reading))
                }
            }
        }
    }
}
