package com.immersive.reader.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.immersive.reader.ui.library.LibraryScreen
import com.immersive.reader.ui.settings.SettingsScreen
import com.immersive.reader.ui.session.StartSessionScreen
import com.immersive.reader.ui.theme.ImmersiveReaderTheme
import com.immersive.reader.reader.ReaderActivity
import com.immersive.reader.reader.SessionSummaryActivity
import com.immersive.reader.session.ReadingSessionState

@Composable
fun ImmersiveReaderApp(
    preferencesViewModel: ReaderPreferencesViewModel = hiltViewModel(),
) {
    val preferences by preferencesViewModel.preferences.collectAsStateWithLifecycle()
    val focusCapabilityViewModel: FocusCapabilityViewModel = hiltViewModel()
    val focusCapabilities by focusCapabilityViewModel.capabilities.collectAsStateWithLifecycle()
    val sessionRecoveryViewModel: SessionRecoveryViewModel = hiltViewModel()
    val sessionStateViewModel: SessionStateViewModel = hiltViewModel()
    val sessionState by sessionStateViewModel.state.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val context = LocalContext.current

    ImmersiveReaderTheme(themeMode = preferences.theme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            NavHost(navController = navController, startDestination = "library") {
                composable("library") {
                    LibraryScreen(
                        onStartReading = { bookId -> navController.navigate("start/$bookId") },
                        onOpenSettings = { navController.navigate("settings") },
                    )
                }
                composable(
                    route = "start/{bookId}",
                    arguments = listOf(navArgument("bookId") { type = NavType.StringType }),
                ) { entry ->
                    StartSessionScreen(
                        bookId = entry.arguments?.getString("bookId").orEmpty(),
                        onBack = { navController.popBackStack() },
                        onStartReading = { sessionId ->
                            entry.arguments?.getString("bookId")?.let { bookId ->
                                context.startActivity(ReaderActivity.intent(context, bookId, sessionId))
                            }
                        },
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        preferences = preferences,
                        onBack = { navController.popBackStack() },
                        onThemeChange = preferencesViewModel::setTheme,
                        onFontSizeChange = preferencesViewModel::setFontSize,
                        onLineHeightChange = preferencesViewModel::setLineHeight,
                        onReadingModeChange = preferencesViewModel::setReadingMode,
                        onKeepScreenAwakeChange = preferencesViewModel::setKeepScreenAwake,
                        onUseDndChange = preferencesViewModel::setUseDnd,
                        onOpenNotificationPolicySettings = {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                        },
                        focusCapabilities = focusCapabilities,
                    )
                }
            }
        }
        val activeSession = (sessionState as? ReadingSessionState.Active)?.session
        if (activeSession != null) {
            AlertDialog(
                onDismissRequest = {},
                title = { androidx.compose.material3.Text("Reading session recovered") },
                text = { androidx.compose.material3.Text("An active ${activeSession.timerMode.name.lowercase().replace('_', ' ')} session is ready to continue.") },
                confirmButton = {
                    Button(onClick = { context.startActivity(ReaderActivity.intent(context, activeSession.bookId, activeSession.id)) }) {
                        androidx.compose.material3.Text("Continue reading")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        sessionRecoveryViewModel.endRecoveredSession { sessionId ->
                            context.startActivity(SessionSummaryActivity.intent(context, sessionId))
                        }
                    }) { androidx.compose.material3.Text("End session") }
                },
            )
        }
    }
}
