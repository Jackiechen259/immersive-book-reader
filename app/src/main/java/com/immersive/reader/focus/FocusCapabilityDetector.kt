package com.immersive.reader.focus

import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FocusCapabilityDetector @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun getCapabilities(): FocusCapabilities {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val devicePolicyManager = context.getSystemService(DevicePolicyManager::class.java)
        val lockTaskPermitted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching { devicePolicyManager?.isLockTaskPermitted(context.packageName) == true }.getOrDefault(false)
        } else {
            false
        }
        val isDeviceOwner = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            runCatching { devicePolicyManager?.isDeviceOwnerApp(context.packageName) == true }.getOrDefault(false)
        } else {
            false
        }
        return FocusCapabilities(
            immersiveAvailable = true,
            screenPinningAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M,
            notificationPolicyGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                notificationManager?.isNotificationPolicyAccessGranted == true
            } else {
                false
            },
            lockTaskPermitted = lockTaskPermitted,
            isDeviceOwner = isDeviceOwner,
        )
    }
}
