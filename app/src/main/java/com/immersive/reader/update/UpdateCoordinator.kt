package com.immersive.reader.update

import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class UpdateCoordinator(
    private val client: GitHubReleaseClient,
    private val downloader: ApkDownloader,
    private val preferences: UpdatePreferences,
    private val currentVersionName: String,
    private val ioDispatcher: CoroutineDispatcher,
    private val clock: () -> Long,
) {
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private val downloadCancelled = AtomicBoolean(false)

    suspend fun maybeAutoCheck() {
        if (_state.value is UpdateState.Downloading) return
        val now = clock()
        if (!UpdatePolicy.shouldAutoCheck(preferences.lastCheckEpochMillis(), now)) return
        performCheck(silentError = true)
    }

    suspend fun check() {
        if (_state.value is UpdateState.Downloading) return
        performCheck(silentError = false)
    }

    suspend fun download() {
        val release = when (val current = _state.value) {
            is UpdateState.Available -> current.release
            is UpdateState.Error -> current.release ?: return
            is UpdateState.ReadyToInstall -> current.release
            else -> return
        }
        downloadCancelled.set(false)
        _state.value = UpdateState.Downloading(release, 0f)
        withContext(ioDispatcher) {
            try {
                downloader.download(release.apkDownloadUrl) { progress ->
                    if (!downloadCancelled.get()) {
                        _state.value = UpdateState.Downloading(release, progress)
                    }
                }
                if (downloadCancelled.get()) {
                    downloader.deletePartial()
                    _state.value = UpdateState.Available(release)
                } else {
                    _state.value = UpdateState.ReadyToInstall(release)
                }
            } catch (error: CancellationException) {
                downloader.deletePartial()
                _state.value = UpdateState.Available(release)
                throw error
            } catch (_: Exception) {
                downloader.deletePartial()
                _state.value = UpdateState.Error(UpdateError.DownloadFailed, release)
            }
        }
    }

    fun cancelDownload() {
        downloadCancelled.set(true)
    }

    suspend fun skip() {
        val tag = when (val current = _state.value) {
            is UpdateState.Available -> current.release.tagName
            is UpdateState.Downloading -> current.release.tagName
            is UpdateState.ReadyToInstall -> current.release.tagName
            is UpdateState.Error -> current.release?.tagName
            else -> null
        } ?: return
        preferences.setSkippedTag(tag)
    }

    private suspend fun performCheck(silentError: Boolean) {
        _state.value = UpdateState.Checking
        withContext(ioDispatcher) {
            try {
                val release = client.fetchLatest()
                preferences.setLastCheckEpochMillis(clock())
                if (VersionComparator.isNewer(release.tagName, currentVersionName)) {
                    _state.value = UpdateState.Available(release)
                } else {
                    downloader.deletePartial()
                    _state.value = UpdateState.UpToDate
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                preferences.setLastCheckEpochMillis(clock())
                if (silentError) {
                    _state.value = UpdateState.Idle
                } else {
                    _state.value = UpdateState.Error(mapError(error))
                }
            }
        }
    }

    private fun mapError(error: Exception): UpdateError = when (error) {
        is ReleaseParseException -> UpdateError.NoApk
        is UpdateHttpException -> if (error.code == 403) UpdateError.RateLimited else UpdateError.Network
        else -> UpdateError.Network
    }
}
