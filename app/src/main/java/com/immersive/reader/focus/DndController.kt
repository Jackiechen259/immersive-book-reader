package com.immersive.reader.focus

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DndController @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    private var previousFilter: Int? = null
    private var changedFilter = false

    fun enter() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || notificationManager?.isNotificationPolicyAccessGranted != true) return
        if (previousFilter == null) previousFilter = notificationManager.currentInterruptionFilter
        runCatching {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            changedFilter = true
        }
    }

    fun exit() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || !changedFilter) return
        previousFilter?.let { filter -> runCatching { notificationManager.setInterruptionFilter(filter) } }
        previousFilter = null
        changedFilter = false
    }
}
