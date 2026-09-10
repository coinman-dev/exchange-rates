package com.exchangerates.app.data.source

import com.exchangerates.app.domain.model.CurrencyKind
import com.exchangerates.app.domain.model.RateEntry
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant

/** Один ответ источника вместе с его настройками приоритета и свежести. */
data class SourceCandidate(
    val sourceId: String,
    val priorities: Map<CurrencyKind, Int>,
    val ttl: Duration,
    val result: SourceResult,
)

/**
 * Чистая логика выбора курса из нескольких источников — вынесена из
 * [RateResolver], чтобы правила приоритета, свежести и сверки можно было
 * проверить тестами без Android и без сети.
 */
object RateMerger {

    val FIAT_TOLERANCE: BigDecimal = BigDecimal("0.05")
    val CRYPTO_TOLERANCE: BigDecimal = BigDecimal("0.25")
    val METAL_TOLERANCE: BigDecimal = BigDecimal("0.15")
    val STALE_GRACE: Duration = Duration.ofHours(12)

    private val MC = MathContext(16, RoundingMode.HALF_EVEN)

    /**
     * @param kinds код валюты -> тип актива
     * @param candidates ответы источников
     * @param referenceSourceId источник, считающийся эталоном для сверки
     * @param now точка отсчёта свежести
     */
    fun merge(
        kinds: Map<String, CurrencyKind>,
        candidates: List<SourceCandidate>,
        referenceSourceId: String?,
        now: Instant = Instant.now(),
    ): Map<String, RateEntry> {
        val reference = candidates.firstOrNull { it.sourceId == referenceSourceId }?.result
        val chosen = LinkedHashMap<String, RateEntry>(kinds.size)

        for ((code, kind) in kinds) {
            val ranked = candidates
                .mapNotNull { candidate ->
                    val priority = candidate.priorities[kind] ?: return@mapNotNull null
                    val rate = candidate.result.rates[code] ?: return@mapNotNull null
                    if (rate.signum() <= 0) return@mapNotNull null
                    if (isStale(candidate, now)) return@mapNotNull null
                    RankedRate(priority, candidate.sourceId, rate, candidate.result.asOf)
                }
                .sortedWith(compareBy({ it.priority }, { it.sourceId }))
            if (ranked.isEmpty()) continue

            val referenceRate = reference?.rates?.get(code)
            val pick = ranked.firstOrNull { candidate ->
                referenceRate == null ||
                    candidate.sourceId == referenceSourceId ||
                    withinTolerance(candidate.rate, referenceRate, kind)
            } ?: ranked.first()

            chosen[code] = RateEntry(code, pick.rate, pick.sourceId, pick.asOf)
        }
        return chosen
    }

    fun isStale(candidate: SourceCandidate, now: Instant): Boolean {
        val age = Duration.between(candidate.result.asOf, now)
        if (age.isNegative) return false
        return age > candidate.ttl.plus(STALE_GRACE)
    }

    fun withinTolerance(value: BigDecimal, reference: BigDecimal, kind: CurrencyKind): Boolean {
        if (reference.signum() == 0) return true
        val deviation = value.subtract(reference).abs().divide(reference.abs(), MC)
        return deviation <= toleranceFor(kind)
    }

    fun toleranceFor(kind: CurrencyKind): BigDecimal = when (kind) {
        CurrencyKind.FIAT -> FIAT_TOLERANCE
        CurrencyKind.CRYPTO -> CRYPTO_TOLERANCE
        CurrencyKind.METAL -> METAL_TOLERANCE
    }

    private data class RankedRate(
        val priority: Int,
        val sourceId: String,
        val rate: BigDecimal,
        val asOf: Instant,
    )
}
