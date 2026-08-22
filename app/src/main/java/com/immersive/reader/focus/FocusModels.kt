package com.immersive.reader.focus

import android.app.Activity

data class FocusCapabilities(
    val immersiveAvailable: Boolean,
    val screenPinningAvailable: Boolean,
    val notificationPolicyGranted: Boolean,
    val lockTaskPermitted: Boolean,
    val isDeviceOwner: Boolean,
)

enum class FocusTier {
    IMMERSIVE,
    PINNED,
    LOCK_TASK,
}

interface FocusController {
    fun getCapabilities(): FocusCapabilities

    fun attach(activity: Activity)

    fun detach(activity: Activity)

    suspend fun enter(session: com.immersive.reader.core.model.ReadingSession)

    suspend fun exit()

    suspend fun restore()
}
