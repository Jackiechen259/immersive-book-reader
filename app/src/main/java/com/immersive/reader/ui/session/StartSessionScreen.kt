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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.immersive.reader.core.model.TimerMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartSessionScreen(bookId: String, onBack: () -> Unit, onStartReading: () -> Unit) {
    var timerMode by remember { mutableStateOf(TimerMode.OPEN_ENDED) }
    var hours by remember { mutableIntStateOf(0) }
    var minutes by remember { mutableIntStateOf(30) }
    var lockEnvironment by remember { mutableStateOf(true) }

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
                FilterChip(selected = timerMode == TimerMode.OPEN_ENDED, onClick = { timerMode = TimerMode.OPEN_ENDED }, label = { Text("∞ Unlimited") })
                FilterChip(selected = timerMode == TimerMode.COUNTDOWN, onClick = { timerMode = TimerMode.COUNTDOWN }, label = { Text("Timed") })
            }
            if (timerMode == TimerMode.OPEN_ENDED) {
                Text("No time limit. Your reading time will be recorded until you decide to end the session.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = hours.toString().padStart(2, '0'), onValueChange = { hours = it.filter(Char::isDigit).take(2).toIntOrNull()?.coerceAtMost(99) ?: 0 }, label = { Text("Hours") }, modifier = Modifier.weight(1f), singleLine = true)
                    Text(":", style = MaterialTheme.typography.headlineSmall)
                    OutlinedTextField(value = minutes.toString().padStart(2, '0'), onValueChange = { minutes = it.filter(Char::isDigit).take(2).toIntOrNull()?.coerceIn(0, 59) ?: 0 }, label = { Text("Minutes") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15 to "15m", 30 to "30m", 45 to "45m", 60 to "1h", 120 to "2h").forEach { (value, label) ->
                        FilterChip(selected = false, onClick = { hours = value / 60; minutes = value % 60 }, label = { Text(label) })
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Lock reading environment", fontWeight = FontWeight.SemiBold)
                    Text("Hide system chrome and add a clear exit boundary.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = lockEnvironment, onCheckedChange = { lockEnvironment = it })
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = onStartReading, modifier = Modifier.fillMaxWidth(), enabled = timerMode == TimerMode.OPEN_ENDED || hours * 60 + minutes > 0) {
                Text("Start reading")
            }
        }
    }
}
