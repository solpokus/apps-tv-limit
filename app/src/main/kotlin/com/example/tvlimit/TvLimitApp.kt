package com.example.tvlimit

import android.app.Application
import androidx.work.*
import java.util.concurrent.TimeUnit

class TvLimitApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppState.init(this)
        scheduleQuotaWorker()
        scheduleMidnightReset()
    }

    private fun scheduleQuotaWorker() {
        val work = PeriodicWorkRequestBuilder<QuotaEnforcerWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(false).build())
            .build()
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork("quota-enforcer", ExistingPeriodicWorkPolicy.KEEP, work)
    }

    private fun scheduleMidnightReset() {
        val delay = Midnight.computeDelayMillis()
        val once = OneTimeWorkRequestBuilder<ResetAtMidnightWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(this)
            .enqueueUniqueWork("midnight-reset", ExistingWorkPolicy.REPLACE, once)
    }
}
