package com.example.tvlimit

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.flow.first

class QuotaEnforcerWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val settings = AppState.settings.first()
        val quotas = settings.quotasList.associate { it.packageName to it.minutes }
        if (quotas.isEmpty()) return Result.success()

        val packages = quotas.keys.toSet()
        val usage = Usage.todayUsageMs(context, packages)
        val exceeded = packages.filter { pkg ->
            val usedMs = usage[pkg] ?: 0L
            val quotaMs = (quotas[pkg] ?: 0) * 60_000L
            usedMs >= quotaMs && quotaMs > 0
        }.toSet()

        if (exceeded.isNotEmpty()) {
            AppState.update { cur ->
                cur.toBuilder()
                    .clearBlocked()
                    .addAllBlocked((cur.blockedList + exceeded).distinct())
                    .build()
            }
            if (DeviceOwner.isDeviceOwner(context)) {
//                DeviceOwner.setPackageEnabled(context, exceeded.toList(), false)
                DeviceOwner.setPackagesBlocked(context, exceeded, true)
            }
        }
        return Result.success()
    }

    companion object {
        fun enqueueNow(context: Context) {
            val once = OneTimeWorkRequestBuilder<QuotaEnforcerWorker>()
                .build()
            WorkManager.getInstance(context).enqueue(once)
        }
    }
}
