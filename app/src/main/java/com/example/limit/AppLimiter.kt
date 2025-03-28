package com.example.limit

import android.content.Context
import android.content.Intent

class AppLimiter(private val context: Context) {
    private val usageMonitor = UsageMonitor(context)
    private val appLimits = mutableMapOf<String, Long>() // Store limits in ms

    fun setLimit(packageName: String, timeLimit: Long) {
        appLimits[packageName] = timeLimit
    }

    fun checkLimit(): Boolean {
        val packageName = usageMonitor.getMostUsedApp() ?: return false
        val usedTime = usageMonitor.getUsageStats()
            .firstOrNull { it.packageName == packageName }?.totalTimeInForeground ?: 0

        return appLimits[packageName]?.let { usedTime > it } ?: false
    }

    fun enforceLimit() {
        if (checkLimit()) {
            val intent = Intent(context, OverlayService::class.java)
            context.startService(intent)
        }
    }
}
