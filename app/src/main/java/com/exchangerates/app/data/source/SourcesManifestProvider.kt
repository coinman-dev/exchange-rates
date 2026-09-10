package com.exchangerates.app.data.source

import android.util.Log
import com.exchangerates.app.data.remote.Endpoints
import com.exchangerates.app.data.remote.RatesApi
import com.exchangerates.app.data.remote.SourceConfig
import com.exchangerates.app.data.remote.SourcesManifest
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Манифест источников: позволяет отключить сломавшийся источник или заменить
 * его адрес без выпуска новой версии приложения. Пока манифест не опубликован,
 * запрос завершается ошибкой и работают значения по умолчанию из кода.
 */
@Singleton
class SourcesManifestProvider @Inject constructor(
    private val api: RatesApi,
) {
    private val mutex = Mutex()
    private var cached: SourcesManifest = SourcesManifest()
    private var loadedAt: Instant? = null

    suspend fun manifest(): SourcesManifest {
        val last = loadedAt
        if (last != null && Duration.between(last, Instant.now()) < REFRESH_AFTER) return cached
        return mutex.withLock {
            val current = loadedAt
            if (current != null && Duration.between(current, Instant.now()) < REFRESH_AFTER) {
                return@withLock cached
            }
            val fresh = runCatching { api.manifest(Endpoints.MANIFEST) }
                .onFailure { Log.i(TAG, "манифест источников недоступен, используются значения по умолчанию") }
                .getOrNull()
            if (fresh != null) cached = fresh
            loadedAt = Instant.now()
            cached
        }
    }

    fun configFor(manifest: SourcesManifest, sourceId: String): SourceConfig =
        manifest.sources[sourceId] ?: SourceConfig()

    private companion object {
        const val TAG = "SourcesManifest"
        val REFRESH_AFTER: Duration = Duration.ofHours(12)
    }
}
