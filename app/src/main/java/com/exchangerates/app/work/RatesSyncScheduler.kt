package com.exchangerates.app.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Планировщик фонового обновления: интервал и ограничение по сети из настроек. */
@Singleton
class RatesSyncScheduler @Inject constructor(
    private val context: Context,
) {
    fun schedule(intervalHours: Int, wifiOnly: Boolean) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<RatesSyncWorker>(
            intervalHours.coerceIn(1, 48).toLong(),
            TimeUnit.HOURS,
        )
            .setConstraints(constraints)
            .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            RatesSyncWorker.NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}
