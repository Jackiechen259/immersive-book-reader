package com.immersive.reader.update

import java.net.HttpURLConnection
import java.net.URL

class HttpGitHubReleaseClient(
    private val versionName: String,
    private val latestReleaseUrl: String = LATEST_RELEASE_URL,
) : GitHubReleaseClient {
    override suspend fun fetchLatest(): GitHubRelease {
        val connection = (URL(latestReleaseUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "ImmersiveReader/$versionName")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            connectTimeout = 15_000
            readTimeout = 20_000
            instanceFollowRedirects = true
        }
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw UpdateHttpException(code)
            return GitHubReleaseParser.parse(body)
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val LATEST_RELEASE_URL =
            "https://api.github.com/repos/Jackiechen259/immersive-book-reader/releases/latest"
    }
}
