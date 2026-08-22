package com.immersive.reader.focus

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DevicePolicyController @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val manager = context.getSystemService(DevicePolicyManager::class.java)
    private val adminComponent = ComponentName(context, FocusDeviceAdminReceiver::class.java)

    fun isDeviceOwner(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        runCatching { manager?.isDeviceOwnerApp(context.packageName) == true }.getOrDefault(false)
    } else {
        false
    }

    fun configureLockTask(): Boolean {
        if (!isDeviceOwner() || manager == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false
        return runCatching {
            manager.setLockTaskPackages(adminComponent, arrayOf(context.packageName))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                manager.setLockTaskFeatures(adminComponent, DevicePolicyManager.LOCK_TASK_FEATURE_NONE)
            }
            true
        }.getOrDefault(false)
    }
}
