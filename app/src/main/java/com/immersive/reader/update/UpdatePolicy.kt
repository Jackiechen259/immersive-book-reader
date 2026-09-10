package com.immersive.reader.update

object UpdatePolicy {
    const val AUTO_CHECK_INTERVAL_MS = 6L * 60 * 60 * 1000

    fun shouldAutoCheck(lastCheckEpochMillis: Long?, nowMillis: Long): Boolean {
        if (lastCheckEpochMillis == null) return true
        return nowMillis - lastCheckEpochMillis >= AUTO_CHECK_INTERVAL_MS
    }

    fun shouldAutoPrompt(
        state: UpdateState,
        skippedTag: String?,
        dismissedTag: String?,
        recoveryVisible: Boolean,
    ): Boolean {
        if (recoveryVisible) return false
        val available = state as? UpdateState.Available ?: return false
        val tag = available.release.tagName
        if (tag == skippedTag || tag == dismissedTag) return false
        return true
    }
}
