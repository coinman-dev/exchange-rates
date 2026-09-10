package com.exchangerates.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.exchangerates.app.data.local.SettingsStore
import com.exchangerates.app.data.repository.RatesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/** Тихое фоновое обновление курсов. */
@HiltWorker
class RatesSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: RatesRepository,
    private val settings: SettingsStore,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val mode = settings.settings.first().rateMode
        repository.seedIfEmpty()
        val updated = repository.refresh(mode, force = true)
        return if (updated) Result.success() else Result.retry()
    }

    companion object {
        const val NAME = "rates-sync"
    }
}
