package com.immersive.reader.update

import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateCoordinatorTest {
    private val newer = GitHubRelease(
        tagName = "v1.0.0",
        name = "1.0.0",
        body = "first release",
        htmlUrl = "https://example.com/v1.0.0",
        apkDownloadUrl = "https://example.com/app.apk",
        apkName = "app.apk",
    )

    @Test
    fun checkSetsAvailableWhenRemoteIsNewer() = runTest {
        val coordinator = coordinator(client = FakeGitHubReleaseClient(newer), currentVersion = "0.1.0")

        coordinator.check()

        val available = coordinator.state.value as UpdateState.Available
        assertEquals("v1.0.0", available.release.tagName)
    }

    @Test
    fun checkSetsUpToDateWhenRemoteIsEqual() = runTest {
        val downloader = FakeApkDownloader()
        val coordinator = coordinator(
            client = FakeGitHubReleaseClient(newer.copy(tagName = "0.1.0")),
            downloader = downloader,
            currentVersion = "0.1.0",
        )

        coordinator.check()

        assertEquals(UpdateState.UpToDate, coordinator.state.value)
        assertTrue(downloader.deleted)
    }

    @Test
    fun manualCheckFailureGoesToError() = runTest {
        val coordinator = coordinator(client = FakeGitHubReleaseClient(error = IOException("offline")))

        coordinator.check()

        val error = coordinator.state.value as UpdateState.Error
        assertEquals(UpdateError.Network, error.reason)
    }

    @Test
    fun failedAutoCheckStaysIdleAndStampsCache() = runTest {
        val prefs = FakeUpdatePreferences()
        var now = 5_000L
        val coordinator = coordinator(
            client = FakeGitHubReleaseClient(error = IOException("offline")),
            preferences = prefs,
            clock = { now },
        )

        coordinator.maybeAutoCheck()

        assertEquals(UpdateState.Idle, coordinator.state.value)
        assertEquals(5_000L, prefs.lastCheck)
        now = 6_000L
        coordinator.maybeAutoCheck()
        assertEquals(5_000L, prefs.lastCheck)
    }

    @Test
    fun autoCheckSkippedWithinSixHours() = runTest {
        val prefs = FakeUpdatePreferences(lastCheck = 1_000L)
        val client = FakeGitHubReleaseClient(newer)
        val coordinator = coordinator(
            client = client,
            preferences = prefs,
            clock = { 1_000L + 3 * 60 * 60 * 1000L },
        )

        coordinator.maybeAutoCheck()

        assertEquals(0, client.fetches)
        assertEquals(UpdateState.Idle, coordinator.state.value)
    }

    @Test
    fun downloadProgressThenReadyToInstall() = runTest {
        val downloader = FakeApkDownloader(progress = listOf(0.4f, 1f))
        val coordinator = coordinator(client = FakeGitHubReleaseClient(newer), downloader = downloader)
        coordinator.check()

        coordinator.download()

        val ready = coordinator.state.value as UpdateState.ReadyToInstall
        assertEquals("v1.0.0", ready.release.tagName)
        assertEquals(listOf(0.4f, 1f), downloader.reported)
    }

    @Test
    fun downloadFailureDeletesPartialFile() = runTest {
        val downloader = FakeApkDownloader(fail = true)
        val coordinator = coordinator(client = FakeGitHubReleaseClient(newer), downloader = downloader)
        coordinator.check()

        coordinator.download()

        val error = coordinator.state.value as UpdateState.Error
        assertEquals(UpdateError.DownloadFailed, error.reason)
        assertTrue(downloader.deleted)
        assertEquals(newer, error.release)
    }

    @Test
    fun skipPersistsCurrentTag() = runTest {
        val prefs = FakeUpdatePreferences()
        val coordinator = coordinator(
            client = FakeGitHubReleaseClient(newer),
            preferences = prefs,
        )
        coordinator.check()

        coordinator.skip()

        assertEquals("v1.0.0", prefs.skipped)
    }

    @Test
    fun noApkAssetMapsToNoApkError() = runTest {
        val coordinator = coordinator(client = FakeGitHubReleaseClient(error = ReleaseParseException("Release has no APK")))

        coordinator.check()

        assertEquals(UpdateError.NoApk, (coordinator.state.value as UpdateState.Error).reason)
    }

    private fun kotlinx.coroutines.test.TestScope.coordinator(
        client: GitHubReleaseClient,
        downloader: ApkDownloader = FakeApkDownloader(),
        preferences: UpdatePreferences = FakeUpdatePreferences(),
        currentVersion: String = "0.1.0",
        clock: () -> Long = { 0L },
    ) = UpdateCoordinator(
        client = client,
        downloader = downloader,
        preferences = preferences,
        currentVersionName = currentVersion,
        ioDispatcher = UnconfinedTestDispatcher(testScheduler),
        clock = clock,
    )
}

private class FakeGitHubReleaseClient(
    private val release: GitHubRelease? = null,
    private val error: Throwable? = null,
) : GitHubReleaseClient {
    var fetches = 0

    override suspend fun fetchLatest(): GitHubRelease {
        fetches += 1
        error?.let { throw it }
        return requireNotNull(release)
    }
}

private class FakeApkDownloader(
    private val progress: List<Float> = listOf(1f),
    private val fail: Boolean = false,
) : ApkDownloader {
    var deleted = false
    val reported = mutableListOf<Float>()

    override suspend fun download(url: String, onProgress: (Float) -> Unit) {
        if (fail) throw IOException("download failed")
        progress.forEach {
            reported += it
            onProgress(it)
        }
    }

    override fun deletePartial() {
        deleted = true
    }
}

private class FakeUpdatePreferences(
    var lastCheck: Long? = null,
    var skipped: String? = null,
) : UpdatePreferences {
    override suspend fun lastCheckEpochMillis(): Long? = lastCheck
    override suspend fun setLastCheckEpochMillis(value: Long) {
        lastCheck = value
    }
    override suspend fun skippedTag(): String? = skipped
    override suspend fun setSkippedTag(value: String?) {
        skipped = value
    }
}
