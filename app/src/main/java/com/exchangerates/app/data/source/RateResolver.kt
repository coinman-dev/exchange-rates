package com.exchangerates.app.data.source

import android.util.Log
import com.exchangerates.app.data.catalog.CurrencyCatalog
import com.exchangerates.app.domain.model.CurrencyKind
import com.exchangerates.app.domain.model.RateEntry
import com.exchangerates.app.domain.model.RateMode
import com.exchangerates.app.domain.model.SourceStatus
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull

/** Результат опроса всех источников. */
data class ResolveOutcome(
    val entries: List<RateEntry>,
    val statuses: List<SourceStatus>,
    val previousDay: Map<String, BigDecimal>,
    val anySuccess: Boolean,
)

/**
 * Сводит курсы из нескольких источников в одну сетку с опорой USD.
 *
 * Правила:
 * 1. Для каждого кода берётся значение источника с наивысшим приоритетом
 *    для его типа актива (фиат / крипта / металл).
 * 2. Значение сверяется с официальным дневным курсом Frankfurter: если
 *    расхождение превышает порог, значение отбрасывается и берётся следующий
 *    источник. Это защищает от ошибок масштаба (курс за 100 единиц и т. п.).
 * 3. В режиме «курс ЦБ» используется только выбранный центробанк — смешивать
 *    официальный курс с рыночным нельзя.
 */
@Singleton
class RateResolver @Inject constructor(
    private val catalog: CurrencyCatalog,
    private val sources: List<@JvmSuppressWildcards RateSource>,
    private val frankfurter: FrankfurterSource,
    private val manifestProvider: SourcesManifestProvider,
) {

    suspend fun resolve(mode: RateMode): ResolveOutcome {
        val currencies = catalog.all()
        val byKind = currencies.groupBy { it.kind }
        val request = FetchRequest(
            fiat = byKind[CurrencyKind.FIAT].orEmpty().mapTo(mutableSetOf()) { it.code },
            crypto = byKind[CurrencyKind.CRYPTO].orEmpty().mapTo(mutableSetOf()) { it.code },
            metals = byKind[CurrencyKind.METAL].orEmpty().mapTo(mutableSetOf()) { it.code },
        )
        val kindOf = currencies.associate { it.code to it.kind }

        return if (mode.isCentralBank) {
            resolveCentralBank(mode, request)
        } else {
            resolveMidMarket(request, kindOf)
        }
    }

    // ---------- рыночная середина ----------

    private suspend fun resolveMidMarket(
        request: FetchRequest,
        kindOf: Map<String, CurrencyKind>,
    ): ResolveOutcome = coroutineScope {
        val manifest = manifestProvider.manifest()
        val enabled = sources.filter { manifestProvider.configFor(manifest, it.id).enabled }

        val jobs = enabled.map { source ->
            async {
                val started = Instant.now()
                val result = withTimeoutOrNull(SOURCE_TIMEOUT.toMillis()) {
                    runCatching { source.fetch(request) }
                }
                when {
                    result == null -> source to Result.failure(TimeoutException(SOURCE_TIMEOUT))
                    else -> source to result.also {
                        it.exceptionOrNull()?.let { error ->
                            Log.w(TAG, "источник ${source.id} не ответил: ${error.message}")
                        }
                        Log.d(TAG, "источник ${source.id}: ${Duration.between(started, Instant.now()).toMillis()} мс")
                    }
                }
            }
        }
        val previousDayJob = async {
            withTimeoutOrNull(SOURCE_TIMEOUT.toMillis()) {
                runCatching {
                    frankfurter.fetchOnDate(
                        LocalDate.now().minusDays(PREVIOUS_DAY_OFFSET),
                        request.fiat + request.metals,
                    )
                }.getOrNull()
            }
        }

        val fetched = jobs.map { it.await() }
        val successes = fetched.mapNotNull { (source, result) ->
            result.getOrNull()?.let { source to it }
        }
        val chosen = RateMerger.merge(
            kinds = kindOf,
            candidates = successes.map { (source, result) ->
                SourceCandidate(
                    sourceId = source.id,
                    priorities = source.priorities,
                    ttl = source.ttl,
                    result = result,
                )
            },
            referenceSourceId = FrankfurterSource.ID,
        )

        val statuses = fetched.map { (source, result) ->
            val value = result.getOrNull()
            SourceStatus(
                sourceId = source.id,
                title = source.title,
                attribution = source.attribution,
                ratesCount = value?.rates?.size ?: 0,
                asOf = value?.asOf,
                error = result.exceptionOrNull()?.let { it.message ?: it::class.simpleName },
                usedFor = chosen.values.count { it.sourceId == source.id },
            )
        }

        ResolveOutcome(
            entries = chosen.values.toList(),
            statuses = statuses,
            previousDay = previousDayJob.await()?.rates.orEmpty(),
            anySuccess = successes.isNotEmpty(),
        )
    }

    // ---------- официальный курс центробанка ----------

    private suspend fun resolveCentralBank(
        mode: RateMode,
        request: FetchRequest,
    ): ResolveOutcome {
        val providerKey = mode.providerKey ?: return ResolveOutcome(emptyList(), emptyList(), emptyMap(), false)
        val codes = request.fiat + request.metals
        val result = runCatching { frankfurter.fetchProvider(providerKey, codes) }
        val value = result.getOrNull()
        val entries = value?.rates?.map { (code, rate) ->
            RateEntry(code, rate, value.sourceId, value.asOf)
        }.orEmpty()
        val status = SourceStatus(
            sourceId = "frankfurter:$providerKey",
            title = centralBankTitle(mode),
            attribution = "Frankfurter · $providerKey",
            ratesCount = entries.size,
            asOf = value?.asOf,
            error = result.exceptionOrNull()?.let { it.message ?: it::class.simpleName },
            usedFor = entries.size,
        )
        return ResolveOutcome(entries, listOf(status), emptyMap(), value != null)
    }

    private fun centralBankTitle(mode: RateMode): String = when (mode) {
        RateMode.CBR -> "Центральный банк РФ"
        RateMode.TCMB -> "Центральный банк Турции"
        RateMode.NBK -> "Национальный банк Казахстана"
        RateMode.MID_MARKET -> "Mid-market"
    }

    private class TimeoutException(timeout: Duration) :
        RuntimeException("превышено время ожидания ${timeout.seconds} с")

    private companion object {
        const val TAG = "RateResolver"
        const val PREVIOUS_DAY_OFFSET = 1L
        val SOURCE_TIMEOUT: Duration = Duration.ofSeconds(12)
    }
}
