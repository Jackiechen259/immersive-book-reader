package com.immersive.reader.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import com.immersive.reader.MainActivity
import com.immersive.reader.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.immersive.reader.ui.components.localizedReadingDuration
import com.immersive.reader.ui.achievements.AchievementsScreen
import com.immersive.reader.ui.library.LibraryScreen
import com.immersive.reader.ui.settings.SettingsScreen
import com.immersive.reader.ui.session.StartSessionScreen
import com.immersive.reader.ui.theme.ImmersiveReaderTheme
import com.immersive.reader.ui.update.UpdateViewModel
import com.immersive.reader.ui.update.VersionComparatorDisplay
import com.immersive.reader.ui.update.truncatedReleaseNotes
import com.immersive.reader.update.UpdatePolicy
import com.immersive.reader.update.UpdateState
import com.immersive.reader.reader.ReaderActivity
import com.immersive.reader.reader.SessionSummaryActivity

@Composable
fun ImmersiveReaderApp(
    preferencesViewModel: ReaderPreferencesViewModel = hiltViewModel(),
    updateViewModel: UpdateViewModel = hiltViewModel(),
) {
    val preferences by preferencesViewModel.preferences.collectAsStateWithLifecycle()
    val focusCapabilityViewModel: FocusCapabilityViewModel = hiltViewModel()
    val focusCapabilities by focusCapabilityViewModel.capabilities.collectAsStateWithLifecycle()
    val sessionRecoveryViewModel: SessionRecoveryViewModel = hiltViewModel()
    val sessionStateViewModel: SessionStateViewModel = hiltViewModel()
    val recovery by sessionStateViewModel.recovery.collectAsStateWithLifecycle()
    val updateState by updateViewModel.state.collectAsStateWithLifecycle()
    val skippedTag by updateViewModel.skippedTag.collectAsStateWithLifecycle()
    val dismissedTag by updateViewModel.dismissedTag.collectAsStateWithLifecycle()
    val offerAutoPrompt by updateViewModel.offerAutoPrompt.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val context = LocalContext.current
    val activity = context as? MainActivity

    LaunchedEffect(recovery) {
        if (recovery == null) updateViewModel.maybeAutoCheck()
    }
    LaunchedEffect(updateState) {
        if (updateViewModel.consumeInstallAfterDownload()) {
            activity?.requestApkInstall()
        }
    }

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
                        onAchievementsVisibleChange = preferencesViewModel::setAchievementsVisible,
                        onOpenAchievements = { navController.navigate("achievements") },
                        focusCapabilities = focusCapabilities,
                        onInstallUpdate = { activity?.requestApkInstall() },
                        updateViewModel = updateViewModel,
                    )
                }
                composable("achievements") {
                    if (!preferences.achievementsVisible) {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                    } else {
                        AchievementsScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
        }
        val available = updateState as? UpdateState.Available
        if (
            recovery == null &&
            available != null &&
            offerAutoPrompt &&
            UpdatePolicy.shouldAutoPrompt(updateState, skippedTag, dismissedTag, recoveryVisible = false)
        ) {
            AlertDialog(
                onDismissRequest = updateViewModel::later,
                title = { Text(stringResource(R.string.update_dialog_title)) },
                text = {
                    val notes = truncatedReleaseNotes(available.release.body).ifBlank {
                        stringResource(R.string.update_notes_unavailable)
                    }
                    Text(
                        stringResource(
                            R.string.update_dialog_body,
                            VersionComparatorDisplay.displayName(available.release),
                            notes,
                        ),
                    )
                },
                confirmButton = {
                    Button(onClick = { updateViewModel.download(installWhenReady = true) }) {
                        Text(stringResource(R.string.action_update))
                    }
                },
                dismissButton = {
                    Column {
                        TextButton(onClick = updateViewModel::later) {
                            Text(stringResource(R.string.action_later))
                        }
                        TextButton(onClick = updateViewModel::skip) {
                            Text(stringResource(R.string.action_skip_version))
                        }
                    }
                },
            )
        }
        val downloading = updateState as? UpdateState.Downloading
        if (downloading != null) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text(stringResource(R.string.action_download_update)) },
                text = {
                    val percent = (downloading.progress * 100).toInt().coerceIn(0, 100)
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.update_downloading, percent))
                        LinearProgressIndicator(
                            progress = { downloading.progress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = updateViewModel::cancelDownload) {
                        Text(stringResource(R.string.action_cancel_download))
                    }
                },
            )
        }
        recovery?.let { active ->
            AlertDialog(
                onDismissRequest = {},
                title = { Text(stringResource(R.string.continue_book_title, active.bookTitle)) },
                text = {
                    Text(
                        if (active.timed && active.remainingMillis != null) {
                            stringResource(
                                R.string.recovery_timed,
                                localizedReadingDuration(active.elapsedMillis),
                                localizedReadingDuration(active.remainingMillis),
                            )
                        } else {
                            stringResource(
                                R.string.recovery_open,
                                localizedReadingDuration(active.elapsedMillis),
                            )
                        },
                    )
                },
                confirmButton = {
                    Button(onClick = { context.startActivity(ReaderActivity.intent(context, active.bookId, active.sessionId)) }) {
                        Text(stringResource(R.string.continue_reading))
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        sessionRecoveryViewModel.endRecoveredSession { sessionId ->
                            context.startActivity(SessionSummaryActivity.intent(context, sessionId))
                        }
                    }) { Text(stringResource(R.string.end_session)) }
                },
            )
        }
    }
}
