package com.example.limit

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context

class UsageMonitor(private val context: Context) {
    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    fun getUsageStats(): List<UsageStats> {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 1000 * 60 * 60 // Last 1 hour
        return usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, endTime
        )
    }

    fun getMostUsedApp(): String? {
        val stats = getUsageStats()
        return stats.maxByOrNull { it.totalTimeInForeground }?.packageName
    }
}
