package com.example.tvlimit

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

// Device admin receiver declared in AndroidManifest.xml
class DeviceAdmin : android.app.admin.DeviceAdminReceiver()

object DeviceOwner {
    private fun dpm(context: Context) =
        context.getSystemService(DevicePolicyManager::class.java)

    private fun admin(context: Context) =
        ComponentName(context, DeviceAdmin::class.java)

    /** True only if our app is the active Device Owner for this user/profile. */
    fun isDeviceOwner(context: Context): Boolean =
        dpm(context).isDeviceOwnerApp(context.packageName)

    /**
     * Best-effort hard block/unblock:
     * - If device owner: suspend or unsuspend package (system-level, can’t open)
     * - If not device owner: try a softer fallback (hide/disable if possible)
     *
     * @return true if a hard block (suspend) was applied; false if we fell back.
     */
    fun setPackageBlocked(context: Context, pkg: String, blocked: Boolean): Boolean {
        val dp = dpm(context)
        return if (dp.isDeviceOwnerApp(context.packageName)) {
            try {
                dp.setPackagesSuspended(admin(context), arrayOf(pkg), blocked)
                true
            } catch (_: SecurityException) {
                // Fallback to "disable"/"enable" as a last resort.
                try {
                    val state = if (blocked)
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    else
                        PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                    context.packageManager.setApplicationEnabledSetting(pkg, state, 0)
                } catch (_: Exception) { /* ignore */ }
                false
            } catch (_: Exception) {
                false
            }
        } else {
            // Not device owner → no true hard block available.
            // We still try to disable (may be ignored on TV), and rely on Accessibility fallback.
            try {
                val state = if (blocked)
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                else
                    PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                context.packageManager.setApplicationEnabledSetting(pkg, state, 0)
            } catch (_: Exception) { /* ignore */ }
            false
        }
    }

    /** Batch helper */
    fun setPackagesBlocked(context: Context, packages: Collection<String>, blocked: Boolean): Boolean {
        var anyHard = false
        packages.forEach { if (setPackageBlocked(context, it, blocked)) anyHard = true }
        return anyHard
    }
}
