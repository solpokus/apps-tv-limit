package com.example.tvlimit

import android.content.Intent
import android.content.pm.PackageManager

data class AppInfo(val label: String, val packageName: String)

object AppList {
    fun loadInstalledLaunchables(context: android.content.Context): List<AppInfo> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        }
        val activities = pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0))
        return activities.map {
            val label = it.loadLabel(pm)?.toString() ?: it.activityInfo.packageName
            AppInfo(label, it.activityInfo.packageName)
        }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
    }
}
