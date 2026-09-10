package com.immersive.reader.ui.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.immersive.reader.BuildConfig
import com.immersive.reader.R
import com.immersive.reader.update.GitHubRelease
import com.immersive.reader.update.UpdateError
import com.immersive.reader.update.UpdateState

@Composable
fun UpdateSettingsContent(
    state: UpdateState,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.current_version, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodyMedium,
        )
        when (state) {
            UpdateState.Idle -> TextButton(onClick = onCheck) {
                Text(stringResource(R.string.action_check_for_updates))
            }
            UpdateState.Checking -> Text(
                stringResource(R.string.update_checking),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            UpdateState.UpToDate -> Text(
                stringResource(R.string.update_up_to_date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            is UpdateState.Available -> AvailableContent(state.release, onDownload)
            is UpdateState.Downloading -> DownloadingContent(state.progress)
            is UpdateState.ReadyToInstall -> TextButton(onClick = onInstall) {
                Text(stringResource(R.string.action_install_update))
            }
            is UpdateState.Error -> {
                Text(
                    stringResource(state.reason.messageRes()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                if (state.release != null) {
                    TextButton(onClick = onDownload) {
                        Text(stringResource(R.string.action_download_update))
                    }
                } else {
                    TextButton(onClick = onCheck) {
                        Text(stringResource(R.string.action_retry))
                    }
                }
            }
        }
    }
}

@Composable
private fun AvailableContent(release: GitHubRelease, onDownload: () -> Unit) {
    Text(
        stringResource(R.string.update_available, VersionComparatorDisplay.displayName(release)),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val notes = truncatedReleaseNotes(release.body)
    if (notes.isNotBlank()) {
        Text(notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    TextButton(onClick = onDownload) {
        Text(stringResource(R.string.action_download_update))
    }
}

@Composable
private fun DownloadingContent(progress: Float) {
    Text(
        stringResource(R.string.update_downloading, (progress * 100).toInt().coerceIn(0, 100)),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
}

fun UpdateError.messageRes(): Int = when (this) {
    UpdateError.Network -> R.string.update_error_network
    UpdateError.NoApk -> R.string.update_error_no_apk
    UpdateError.DownloadFailed -> R.string.update_error_download
    UpdateError.RateLimited -> R.string.update_error_rate_limited
}

fun truncatedReleaseNotes(body: String?, limit: Int = 500): String {
    val trimmed = body?.trim().orEmpty()
    if (trimmed.isEmpty()) return ""
    return if (trimmed.length <= limit) trimmed else trimmed.take(limit).trimEnd() + "…"
}

object VersionComparatorDisplay {
    fun displayName(release: GitHubRelease): String {
        val fromName = release.name?.trim().orEmpty().removePrefix("v").removePrefix("V")
        if (fromName.isNotEmpty()) return fromName
        return release.tagName.trim().removePrefix("v").removePrefix("V")
    }
}
