package com.example.tvlimit

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

class DeviceAdmin : android.app.admin.DeviceAdminReceiver()

object DeviceOwner {
    private fun dpm(context: Context) = context.getSystemService(DevicePolicyManager::class.java)
    private fun admin(context: Context) = ComponentName(context, DeviceAdmin::class.java)

    fun isDeviceOwner(context: Context): Boolean =
        dpm(context).isDeviceOwnerApp(context.packageName)

    fun setPackageEnabled(context: Context, packages: List<String>, enable: Boolean) {
        val dpm = dpm(context)
        val admin = admin(context)
        if (!dpm.isDeviceOwnerApp(context.packageName)) return
        packages.forEach { pkg ->
            try {
                dpm.setPackagesSuspended(admin, arrayOf(pkg), !enable)
            } catch (_: Exception) {
                try {
                    val state = if (enable) PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                    else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    context.packageManager.setApplicationEnabledSetting(pkg, state, 0)
                } catch (_: Exception) {}
            }
        }
    }
}
