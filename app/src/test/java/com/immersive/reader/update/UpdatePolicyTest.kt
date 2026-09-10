package com.immersive.reader.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatePolicyTest {
    private val release = GitHubRelease(
        tagName = "v0.2.0",
        name = "0.2.0",
        body = "notes",
        htmlUrl = "https://example.com",
        apkDownloadUrl = "https://example.com/app.apk",
        apkName = "app.apk",
    )

    @Test
    fun autoCheckRunsWhenNeverChecked() {
        assertTrue(UpdatePolicy.shouldAutoCheck(lastCheckEpochMillis = null, nowMillis = 10_000L))
    }

    @Test
    fun autoCheckWaitsSixHours() {
        val last = 1_000L
        assertFalse(UpdatePolicy.shouldAutoCheck(last, last + 3 * 60 * 60 * 1000L))
        assertTrue(UpdatePolicy.shouldAutoCheck(last, last + UpdatePolicy.AUTO_CHECK_INTERVAL_MS))
    }

    @Test
    fun autoPromptRequiresAvailableState() {
        assertFalse(
            UpdatePolicy.shouldAutoPrompt(
                state = UpdateState.UpToDate,
                skippedTag = null,
                dismissedTag = null,
                recoveryVisible = false,
            ),
        )
        assertTrue(
            UpdatePolicy.shouldAutoPrompt(
                state = UpdateState.Available(release),
                skippedTag = null,
                dismissedTag = null,
                recoveryVisible = false,
            ),
        )
    }

    @Test
    fun skippedTagDoesNotAutoPrompt() {
        assertFalse(
            UpdatePolicy.shouldAutoPrompt(
                state = UpdateState.Available(release),
                skippedTag = "v0.2.0",
                dismissedTag = null,
                recoveryVisible = false,
            ),
        )
    }

    @Test
    fun dismissedTagDoesNotAutoPrompt() {
        assertFalse(
            UpdatePolicy.shouldAutoPrompt(
                state = UpdateState.Available(release),
                skippedTag = null,
                dismissedTag = "v0.2.0",
                recoveryVisible = false,
            ),
        )
    }

    @Test
    fun recoveryDialogSuppressesAutoPrompt() {
        assertFalse(
            UpdatePolicy.shouldAutoPrompt(
                state = UpdateState.Available(release),
                skippedTag = null,
                dismissedTag = null,
                recoveryVisible = true,
            ),
        )
    }
}
