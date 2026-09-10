package com.exchangerates.app.data.repository

import android.util.Log
import com.exchangerates.app.data.catalog.CurrencyCatalog
import com.exchangerates.app.data.local.HistoryDao
import com.exchangerates.app.data.local.HistoryEntity
import com.exchangerates.app.data.remote.Endpoints
import com.exchangerates.app.data.remote.RatesApi
import com.exchangerates.app.data.source.CoinGeckoSource
import com.exchangerates.app.domain.model.ChartRange
import com.exchangerates.app.domain.model.CurrencyKind
import com.exchangerates.app.domain.model.HistoryPoint
import com.exchangerates.app.domain.model.RateTable
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

/**
 * История курсов для экрана графиков.
 *
 * Фиат и металлы — Frankfurter v2 (диапазон дат, прореживание `group`),
 * крипта — свечи Binance с резервом CoinGecko, внутридневной ряд по фиату —
 * Yahoo Finance (неофициальный источник, поэтому его отказ не считается ошибкой).
 */
@Singleton
class HistoryRepository @Inject constructor(
    private val api: RatesApi,
    private val dao: HistoryDao,
    private val catalog: CurrencyCatalog,
) {

    suspend fun series(base: String, quote: String, range: ChartRange): List<HistoryPoint> {
        if (base == quote) return emptyList()
        return runCatching {
            if (range == ChartRange.DAY) intraday(base, quote) else daily(base, quote, range)
        }.onFailure {
            Log.w(TAG, "история $base/$quote ${range.id} недоступна: ${it.message}")
        }.getOrDefault(emptyList())
    }

    // ---------- внутридневной ряд ----------

    private suspend fun intraday(base: String, quote: String): List<HistoryPoint> {
        val kinds = catalog.byCode()
        val baseKind = kinds[base]?.kind ?: CurrencyKind.FIAT
        val quoteKind = kinds[quote]?.kind ?: CurrencyKind.FIAT

        if (baseKind == CurrencyKind.CRYPTO || quoteKind == CurrencyKind.CRYPTO) {
            return combineUsdLegs(base, quote, ChartRange.DAY)
        }
        // фиатные пары: пятиминутные свечи Yahoo
        val points = yahooSeries("$base$quote", range = "1d", interval = "5m")
        if (points.isNotEmpty()) return points
        return yahooSeries("$quote$base", range = "1d", interval = "5m")
            .map { HistoryPoint(it.epochMillis, invert(it.rate)) }
    }

    private suspend fun yahooSeries(pair: String, range: String, interval: String): List<HistoryPoint> {
        val body = api.rawJson(Endpoints.yahooChart(pair, range, interval))
        val chart = body["chart"] ?: return emptyList()
        val result = chart.jsonObjectOrNull()?.get("result")?.jsonArrayOrNull()?.firstOrNull()
            ?.jsonObjectOrNull() ?: return emptyList()
        val timestamps = result["timestamp"]?.jsonArrayOrNull()
            ?.mapNotNull { it.primitiveOrNull()?.longOrNull } ?: return emptyList()
        val closes = result["indicators"]?.jsonObjectOrNull()
            ?.get("quote")?.jsonArrayOrNull()?.firstOrNull()?.jsonObjectOrNull()
            ?.get("close")?.jsonArrayOrNull()
            ?.map { it.primitiveOrNull()?.doubleOrNull } ?: return emptyList()
        return timestamps.zip(closes).mapNotNull { (time, close) ->
            if (close == null || close <= 0.0) null
            else HistoryPoint(time * 1000, BigDecimal.valueOf(close))
        }
    }

    // ---------- дневной ряд ----------

    private suspend fun daily(base: String, quote: String, range: ChartRange): List<HistoryPoint> {
        val kinds = catalog.byCode()
        val baseKind = kinds[base]?.kind ?: CurrencyKind.FIAT
        val quoteKind = kinds[quote]?.kind ?: CurrencyKind.FIAT
        val hasCrypto = baseKind == CurrencyKind.CRYPTO || quoteKind == CurrencyKind.CRYPTO

        val pair = "$base/$quote/${range.id}"
        val cached = readCache(pair, range)
        if (cached != null) return cached

        val points = if (hasCrypto) {
            combineUsdLegs(base, quote, range)
        } else {
            frankfurterSeries(base, quote, range)
        }
        if (points.isNotEmpty()) writeCache(pair, points)
        return points
    }

    private suspend fun frankfurterSeries(
        base: String,
        quote: String,
        range: ChartRange,
    ): List<HistoryPoint> {
        val to = LocalDate.now()
        val from = to.minusDays(range.days.toLong())
        val rows = api.frankfurter(
            Endpoints.frankfurterSeries(base, quote, from.toString(), to.toString(), range.group),
        )
        return rows.mapNotNull { row ->
            val date = runCatching { LocalDate.parse(row.date) }.getOrNull() ?: return@mapNotNull null
            if (row.rate.signum() <= 0) return@mapNotNull null
            HistoryPoint(date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(), row.rate)
        }.sortedBy { it.epochMillis }
    }

    /**
     * Ряд для пар с криптой: берём оба плеча в долларах и делим поточечно.
     * Точки выравниваются по дате с переносом последнего известного значения.
     */
    private suspend fun combineUsdLegs(
        base: String,
        quote: String,
        range: ChartRange,
    ): List<HistoryPoint> = coroutineScope {
        val baseLeg = async { usdLeg(base, range) }
        val quoteLeg = async { usdLeg(quote, range) }
        val baseSeries = baseLeg.await()
        val quoteSeries = quoteLeg.await()
        if (baseSeries.isEmpty() || quoteSeries.isEmpty()) return@coroutineScope emptyList()

        val baseByTime = baseSeries.associate { it.epochMillis to it.rate }.toSortedMap()
        val quoteByTime = quoteSeries.associate { it.epochMillis to it.rate }.toSortedMap()
        // ведущее плечо — то, где точек больше (обычно криптовалюта)
        val leading = if (quoteByTime.size >= baseByTime.size) quoteByTime else baseByTime
        val result = mutableListOf<HistoryPoint>()
        for (time in leading.keys) {
            val baseRate = nearestAtOrBefore(baseByTime, time) ?: continue
            val quoteRate = nearestAtOrBefore(quoteByTime, time) ?: continue
            if (baseRate.signum() == 0) continue
            result += HistoryPoint(time, quoteRate.divide(baseRate, MC))
        }
        result
    }

    /** Ряд «сколько единиц валюты за 1 USD». */
    private suspend fun usdLeg(code: String, range: ChartRange): List<HistoryPoint> {
        if (code == RateTable.PIVOT) {
            return listOf(HistoryPoint(Instant.now().toEpochMilli(), BigDecimal.ONE))
        }
        val kind = catalog.byCode()[code]?.kind ?: CurrencyKind.FIAT
        return if (kind == CurrencyKind.CRYPTO) {
            binanceLeg(code, range).ifEmpty { coinGeckoLeg(code, range) }
        } else {
            frankfurterSeries(RateTable.PIVOT, code, range)
        }
    }

    private suspend fun binanceLeg(code: String, range: ChartRange): List<HistoryPoint> {
        if (code == "USDT") return listOf(HistoryPoint(Instant.now().toEpochMilli(), BigDecimal.ONE))
        val (interval, limit) = when (range) {
            ChartRange.DAY -> "1h" to 24
            ChartRange.WEEK -> "4h" to 42
            ChartRange.MONTH -> "1d" to 30
            ChartRange.YEAR -> "1w" to 53
            ChartRange.FIVE_YEARS -> "1M" to 60
        }
        val rows = runCatching {
            api.binanceKlines(Endpoints.binanceKlines("${code}USDT", interval, limit))
        }.getOrElse { return emptyList() }
        return rows.mapNotNull { row ->
            val openTime = row.getOrNull(0)?.longOrNull ?: return@mapNotNull null
            val close = row.getOrNull(4)?.content?.toBigDecimalOrNull() ?: return@mapNotNull null
            if (close.signum() <= 0) return@mapNotNull null
            HistoryPoint(openTime, invert(close))
        }
    }

    private suspend fun coinGeckoLeg(code: String, range: ChartRange): List<HistoryPoint> {
        val id = CoinGeckoSource.COIN_IDS[code] ?: return emptyList()
        val chart = runCatching {
            api.coinGeckoMarketChart(Endpoints.coinGeckoMarketChart(id, range.days))
        }.getOrElse { return emptyList() }
        return chart.prices.mapNotNull { pair ->
            val time = pair.getOrNull(0)?.toLong() ?: return@mapNotNull null
            val price = pair.getOrNull(1) ?: return@mapNotNull null
            if (price <= 0.0) return@mapNotNull null
            HistoryPoint(time, invert(BigDecimal.valueOf(price)))
        }
    }

    // ---------- кэш ----------

    private suspend fun readCache(pair: String, range: ChartRange): List<HistoryPoint>? {
        val lastFetch = dao.lastFetchedAt(pair) ?: return null
        val age = Duration.between(Instant.ofEpochMilli(lastFetch), Instant.now())
        if (age > CACHE_TTL) return null
        val fromDay = LocalDate.now().minusDays(range.days.toLong()).toEpochDay()
        val rows = dao.range(pair, fromDay)
        if (rows.isEmpty()) return null
        return rows.map {
            HistoryPoint(it.epochDay * MILLIS_PER_DAY, BigDecimal(it.rate))
        }
    }

    private suspend fun writeCache(pair: String, points: List<HistoryPoint>) {
        val now = Instant.now().toEpochMilli()
        dao.insert(
            points.map {
                HistoryEntity(
                    pair = pair,
                    epochDay = it.epochMillis / MILLIS_PER_DAY,
                    rate = it.rate.toPlainString(),
                    fetchedAt = now,
                )
            },
        )
        dao.prune(now - CACHE_PRUNE.toMillis())
    }

    private fun invert(value: BigDecimal): BigDecimal =
        if (value.signum() == 0) BigDecimal.ZERO else BigDecimal.ONE.divide(value, MC)

    private fun nearestAtOrBefore(
        series: java.util.SortedMap<Long, BigDecimal>,
        time: Long,
    ): BigDecimal? {
        series[time]?.let { return it }
        val head = series.headMap(time + 1)
        if (head.isNotEmpty()) return head[head.lastKey()]
        return series.entries.firstOrNull()?.value
    }

    private fun String.toBigDecimalOrNull(): BigDecimal? = runCatching { BigDecimal(this) }.getOrNull()

    private fun kotlinx.serialization.json.JsonElement.jsonObjectOrNull() =
        this as? kotlinx.serialization.json.JsonObject

    private fun kotlinx.serialization.json.JsonElement.jsonArrayOrNull() =
        this as? kotlinx.serialization.json.JsonArray

    private fun kotlinx.serialization.json.JsonElement.primitiveOrNull() =
        this as? kotlinx.serialization.json.JsonPrimitive

    private companion object {
        const val TAG = "HistoryRepository"
        const val MILLIS_PER_DAY = 86_400_000L
        val MC: MathContext = MathContext(20, RoundingMode.HALF_EVEN)
        val CACHE_TTL: Duration = Duration.ofHours(6)
        val CACHE_PRUNE: Duration = Duration.ofDays(30)
    }
}
