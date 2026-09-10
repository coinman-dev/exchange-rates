package com.exchangerates.app.data.source

import com.exchangerates.app.domain.model.CurrencyKind
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

/** Что именно нужно запросить у источника. */
data class FetchRequest(
    val fiat: Set<String>,
    val crypto: Set<String>,
    val metals: Set<String>,
) {
    fun codesFor(kind: CurrencyKind): Set<String> = when (kind) {
        CurrencyKind.FIAT -> fiat
        CurrencyKind.CRYPTO -> crypto
        CurrencyKind.METAL -> metals
    }
}

/** Ответ источника: курсы «сколько единиц валюты за 1 USD». */
data class SourceResult(
    val sourceId: String,
    val rates: Map<String, BigDecimal>,
    val asOf: Instant,
)

/**
 * Источник курсов. Приоритет задаётся отдельно по типу актива: например
 * Coinbase — лучший для живого фиата, но для крипты приоритетнее Binance.
 */
interface RateSource {
    val id: String
    val title: String
    val attribution: String? get() = null
    val priorities: Map<CurrencyKind, Int>
    val ttl: Duration

    suspend fun fetch(request: FetchRequest): SourceResult

    fun priorityFor(kind: CurrencyKind): Int? = priorities[kind]

    companion object {
        val LIVE_TTL: Duration = Duration.ofMinutes(20)
        val DAILY_TTL: Duration = Duration.ofHours(36)
    }
}
