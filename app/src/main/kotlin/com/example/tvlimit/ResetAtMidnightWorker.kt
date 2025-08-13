package com.example.tvlimit

import android.content.Context
import androidx.work.*
import com.example.tvlimit.proto.Settings
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class ResetAtMidnightWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val today = Midnight.currentEpochDay()
        val settings = AppState.settings.first()
        if (settings.lastResetEpochDay != today) {
            // Clear blocked-by-quota daily state; keep manual blocks
            AppState.update { cur ->
                cur.toBuilder()
                    .setLastResetEpochDay(today)
                    .clearBlocked() // daily blocks reset; you can split manual vs quota if needed
                    .build()
            }
        }
        // Schedule the next midnight reset
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
