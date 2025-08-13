package com.example.tvlimit

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class ResetAtMidnightWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val today = Midnight.currentEpochDay()
        val settings = AppState.settings.first()
        if (settings.lastResetEpochDay != today) {
            AppState.update { cur ->
                cur.toBuilder()
                    .setLastResetEpochDay(today)
                    .clearBlocked()
                    .build()
            }
        }
        val delay = Midnight.computeDelayMillis()
        val once = OneTimeWorkRequestBuilder<ResetAtMidnightWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "midnight-reset",
            ExistingWorkPolicy.REPLACE,
            once
        )
        return Result.success()
    }
}
