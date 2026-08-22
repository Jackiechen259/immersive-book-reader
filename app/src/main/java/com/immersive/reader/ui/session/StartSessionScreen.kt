package com.immersive.reader.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Button
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.immersive.reader.core.model.TimerMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartSessionScreen(
    bookId: String,
    onBack: () -> Unit,
    onStartReading: (String) -> Unit,
    viewModel: StartSessionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Prepare to read") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
        )
    }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MenuBook, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 12.dp))
                Column {
                    Text("Reading session", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("Book $bookId", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text("How long would you like to read?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(selected = uiState.timerMode == TimerMode.OPEN_ENDED, onClick = { viewModel.setTimerMode(TimerMode.OPEN_ENDED) }, label = { Text("∞ Unlimited") })
                FilterChip(selected = uiState.timerMode == TimerMode.COUNTDOWN, onClick = { viewModel.setTimerMode(TimerMode.COUNTDOWN) }, label = { Text("Timed") })
            }
            if (uiState.timerMode == TimerMode.OPEN_ENDED) {
                Text("No time limit. Your reading time will be recorded until you decide to end the session.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = uiState.hours.toString().padStart(2, '0'), onValueChange = { viewModel.setHours(it.filter(Char::isDigit).take(2).toIntOrNull() ?: 0) }, label = { Text("Hours") }, modifier = Modifier.weight(1f), singleLine = true)
                    Text(":", style = MaterialTheme.typography.headlineSmall)
                    OutlinedTextField(value = uiState.minutes.toString().padStart(2, '0'), onValueChange = { viewModel.setMinutes(it.filter(Char::isDigit).take(2).toIntOrNull() ?: 0) }, label = { Text("Minutes") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15 to "15m", 30 to "30m", 45 to "45m", 60 to "1h", 120 to "2h").forEach { (value, label) ->
                        FilterChip(selected = uiState.hours * 60 + uiState.minutes == value, onClick = { viewModel.setHours(value / 60); viewModel.setMinutes(value % 60) }, label = { Text(label) })
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Lock reading environment", fontWeight = FontWeight.SemiBold)
                    Text("Hide system chrome and add a clear exit boundary.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = uiState.lockEnvironment, onCheckedChange = viewModel::setLockEnvironment)
            }
            Spacer(Modifier.weight(1f))
            uiState.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = { viewModel.start(bookId, onStarted = onStartReading) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.starting && (uiState.timerMode == TimerMode.OPEN_ENDED || uiState.hours * 60 + uiState.minutes > 0),
            ) {
                Text("Start reading")
            }
        }
    }
}
