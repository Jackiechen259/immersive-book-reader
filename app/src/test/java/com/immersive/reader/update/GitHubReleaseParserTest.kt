package com.immersive.reader.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubReleaseParserTest {
    @Test
    fun parsesReleaseWithSingleApk() {
        val release = GitHubReleaseParser.parse(
            """
            {
              "tag_name": "v0.2.0",
              "name": "0.2.0",
              "body": "Calm updates.",
              "html_url": "https://github.com/Jackiechen259/immersive-book-reader/releases/tag/v0.2.0",
              "assets": [
                {
                  "name": "immersive-reader-0.2.0.apk",
                  "browser_download_url": "https://example.com/immersive-reader-0.2.0.apk"
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals("v0.2.0", release.tagName)
        assertEquals("0.2.0", release.name)
        assertEquals("Calm updates.", release.body)
        assertEquals("https://example.com/immersive-reader-0.2.0.apk", release.apkDownloadUrl)
        assertEquals("immersive-reader-0.2.0.apk", release.apkName)
    }

    @Test
    fun prefersNonDebugApkWhenBothPresent() {
        val release = GitHubReleaseParser.parse(
            """
            {
              "tag_name": "1.0.0",
              "html_url": "https://example.com/1.0.0",
              "assets": [
                {"name": "app-debug.apk", "browser_download_url": "https://example.com/debug.apk"},
                {"name": "immersive-reader-1.0.0.apk", "browser_download_url": "https://example.com/release.apk"}
              ]
            }
            """.trimIndent(),
        )

        assertEquals("https://example.com/release.apk", release.apkDownloadUrl)
        assertEquals("immersive-reader-1.0.0.apk", release.apkName)
    }

    @Test
    fun throwsWhenNoApkAsset() {
        try {
            GitHubReleaseParser.parse(
                """
                {
                  "tag_name": "1.0.0",
                  "html_url": "https://example.com/1.0.0",
                  "assets": [{"name": "notes.txt", "browser_download_url": "https://example.com/notes.txt"}]
                }
                """.trimIndent(),
            )
            throw AssertionError("expected ReleaseParseException")
        } catch (error: ReleaseParseException) {
            assertTrue(error.message.orEmpty().contains("APK"))
        }
    }

    @Test
    fun throwsOnMalformedJson() {
        try {
            GitHubReleaseParser.parse("{not json")
            throw AssertionError("expected ReleaseParseException")
        } catch (error: ReleaseParseException) {
            assertTrue(error.message.orEmpty().isNotBlank())
        }
    }
}
