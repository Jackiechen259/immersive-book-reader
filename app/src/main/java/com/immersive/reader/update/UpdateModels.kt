package com.immersive.reader.update

data class GitHubRelease(
    val tagName: String,
    val name: String?,
    val body: String?,
    val htmlUrl: String,
    val apkDownloadUrl: String,
    val apkName: String,
)

enum class UpdateError {
    Network,
    NoApk,
    DownloadFailed,
    RateLimited,
}

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val release: GitHubRelease) : UpdateState
    data class Downloading(val release: GitHubRelease, val progress: Float) : UpdateState
    data class ReadyToInstall(val release: GitHubRelease) : UpdateState
    data class Error(val reason: UpdateError, val release: GitHubRelease? = null) : UpdateState
}

class ReleaseParseException(message: String, cause: Throwable? = null) : Exception(message, cause)

class UpdateHttpException(val code: Int) : Exception("HTTP $code")
