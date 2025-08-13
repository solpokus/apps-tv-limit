package com.example.tvlimit

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import android.provider.Settings
import java.util.Calendar

object Usage {
    fun hasUsagePermission(context: Context): Boolean {
        val aom = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = aom.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun todayUsageMs(context: Context, packages: Set<String>): Map<String, Long> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        val end = System.currentTimeMillis()

        val result = mutableMapOf<String, Long>().apply { packages.forEach { put(it, 0L) } }
        val events = usm.queryEvents(start, end)
        val lastResume = HashMap<String, Long>()
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            if (pkg !in packages) continue
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED, UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    lastResume[pkg] = event.timeStamp
                }
                UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val s = lastResume.remove(pkg)
                    if (s != null && event.timeStamp >= s) {
                        result[pkg] = (result[pkg] ?: 0L) + (event.timeStamp - s)
                    }
                }
            }
        }
        // If something is still in foreground at query end, add tail
        val now = end
        lastResume.forEach { (pkg, s) ->
            result[pkg] = (result[pkg] ?: 0L) + (now - s)
        }
        return result
    }
}
