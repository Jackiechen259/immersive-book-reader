package com.immersive.reader.update

interface GitHubReleaseClient {
    suspend fun fetchLatest(): GitHubRelease
}

interface ApkDownloader {
    suspend fun download(url: String, onProgress: (Float) -> Unit)
    fun deletePartial()
}

interface UpdatePreferences {
    suspend fun lastCheckEpochMillis(): Long?
    suspend fun setLastCheckEpochMillis(value: Long)
    suspend fun skippedTag(): String?
    suspend fun setSkippedTag(value: String?)
}
