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
    private val statePreferences = context.getSharedPreferences("focus_state", Context.MODE_PRIVATE)
    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    private var previousFilter: Int? = statePreferences.getInt(KEY_PREVIOUS_FILTER, -1).takeIf { it >= 0 }
    private var changedFilter: Boolean = statePreferences.getBoolean(KEY_CHANGED_FILTER, false)

    fun enter() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || notificationManager?.isNotificationPolicyAccessGranted != true) return
        if (previousFilter == null) {
            previousFilter = notificationManager.currentInterruptionFilter
            statePreferences.edit().putInt(KEY_PREVIOUS_FILTER, previousFilter ?: return).apply()
        }
        runCatching {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            changedFilter = true
            statePreferences.edit().putBoolean(KEY_CHANGED_FILTER, true).apply()
        }
    }

    fun exit() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || !changedFilter) return
        previousFilter?.let { filter -> runCatching { notificationManager.setInterruptionFilter(filter) } }
        previousFilter = null
        changedFilter = false
        statePreferences.edit().clear().apply()
    }

    private companion object {
        const val KEY_PREVIOUS_FILTER = "previous_dnd_filter"
        const val KEY_CHANGED_FILTER = "dnd_changed"
    }
}
