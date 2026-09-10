package com.exchangerates.app.data.source

import com.exchangerates.app.data.remote.Endpoints
import com.exchangerates.app.data.remote.RatesApi
import com.exchangerates.app.domain.model.CurrencyKind
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Coinbase: единственный найденный бесплатный источник без ключа, который отдаёт
 * живые курсы и по фиату, и по крипте (~640 кодов). Метки времени в ответе нет,
 * поэтому свежесть считаем по моменту запроса.
 */
class CoinbaseSource @Inject constructor(
    private val api: RatesApi,
) : RateSource {
    override val id = ID
    override val title = "Coinbase"
    override val priorities = mapOf(CurrencyKind.FIAT to 10, CurrencyKind.CRYPTO to 20)
    override val ttl: Duration = RateSource.LIVE_TTL

    override suspend fun fetch(request: FetchRequest): SourceResult {
        val response = api.coinbase(Endpoints.COINBASE_USD)
        val wanted = request.fiat + request.crypto
        val rates = response.data.rates
            .filterKeys { it in wanted }
            .filterValues { it.signum() > 0 }
        return SourceResult(id, rates, Instant.now())
    }

    companion object { const val ID = "coinbase" }
}

/** floatrates: 168 валют с метками времени, обновляется несколько раз в сутки. */
class FloatratesSource @Inject constructor(
    private val api: RatesApi,
) : RateSource {
    override val id = ID
    override val title = "FloatRates"
    override val priorities = mapOf(CurrencyKind.FIAT to 20)
    override val ttl: Duration = Duration.ofHours(12)

    override suspend fun fetch(request: FetchRequest): SourceResult {
        val response = api.floatrates(Endpoints.FLOATRATES_USD)
        val rates = mutableMapOf<String, BigDecimal>()
        var asOf: Instant? = null
        for ((_, value) in response) {
            val code = value.code.uppercase()
            if (code !in request.fiat) continue
            if (value.rate.signum() <= 0) continue
            rates[code] = value.rate
            if (asOf == null) asOf = parseRfc1123(value.date)
        }
        return SourceResult(id, rates, asOf ?: Instant.now())
    }

    private fun parseRfc1123(text: String?): Instant? = text?.let {
        runCatching { Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(it)) }.getOrNull()
    }

    companion object { const val ID = "floatrates" }
}

/**
 * Frankfurter v2: 165 валют (включая металлы XAU/XAG/XPT/XPD), курсы смешаны
 * по 96 центробанкам. Дневная точность, зато официальные данные и история.
 */
class FrankfurterSource @Inject constructor(
    private val api: RatesApi,
) : RateSource {
    override val id = ID
    override val title = "Frankfurter (центробанки)"
    override val priorities = mapOf(
        CurrencyKind.FIAT to 30,
        CurrencyKind.METAL to 30,
    )
    override val ttl: Duration = RateSource.DAILY_TTL

    override suspend fun fetch(request: FetchRequest): SourceResult =
        fetchUrl(Endpoints.FRANKFURTER_USD, request.fiat + request.metals)

    /** Курс конкретного центробанка (режим «курс ЦБ»). */
    suspend fun fetchProvider(providerKey: String, codes: Set<String>): SourceResult =
        fetchUrl(Endpoints.frankfurterProvider(providerKey), codes, sourceId = "frankfurter:$providerKey")

    /** Сетка на конкретную дату — нужна для расчёта изменения за сутки. */
    suspend fun fetchOnDate(date: LocalDate, codes: Set<String>): SourceResult =
        fetchUrl("${Endpoints.FRANKFURTER_USD}&date=$date", codes, sourceId = "frankfurter:$date")

    private suspend fun fetchUrl(
        url: String,
        wanted: Set<String>,
        sourceId: String = id,
    ): SourceResult {
        val rows = api.frankfurter(url)
        val rates = rows
            .filter { it.quote in wanted && it.rate.signum() > 0 }
            .associate { it.quote to it.rate }
        val asOf = rows.mapNotNull { row ->
            runCatching { LocalDate.parse(row.date).atStartOfDay(ZoneOffset.UTC).toInstant() }.getOrNull()
        }.maxOrNull() ?: Instant.now()
        return SourceResult(sourceId, rates, asOf)
    }

    companion object { const val ID = "frankfurter" }
}

/** open.er-api.com: резерв по фиату. Требует указания атрибуции в интерфейсе. */
class ErApiSource @Inject constructor(
    private val api: RatesApi,
) : RateSource {
    override val id = ID
    override val title = "ExchangeRate-API"
    override val attribution = "Rates by exchangerate-api.com"
    override val priorities = mapOf(CurrencyKind.FIAT to 40)
    override val ttl: Duration = RateSource.DAILY_TTL

    override suspend fun fetch(request: FetchRequest): SourceResult {
        val response = api.erApi(Endpoints.ER_API_USD)
        if (!response.result.equals("success", ignoreCase = true)) {
            error("er-api result=${response.result}")
        }
        val rates = response.rates
            .filterKeys { it in request.fiat }
            .filterValues { it.signum() > 0 }
        val asOf = if (response.timeLastUpdateUnix > 0) {
            Instant.ofEpochSecond(response.timeLastUpdateUnix)
        } else {
            Instant.now()
        }
        return SourceResult(id, rates, asOf)
    }

    companion object { const val ID = "erapi" }
}

/**
 * fawazahmed0/currency-api: 372 кода в одном файле — фиат, крипта и металлы.
 * Раздаётся с CDN без лимитов, обновляется раз в сутки. Последний рубеж перед
 * вшитым в APK снимком.
 */
class FawazahmedSource @Inject constructor(
    private val api: RatesApi,
) : RateSource {
    override val id = ID
    override val title = "fawazahmed0/currency-api"
    override val priorities = mapOf(
        CurrencyKind.FIAT to 50,
        CurrencyKind.CRYPTO to 40,
        CurrencyKind.METAL to 20,
    )
    override val ttl: Duration = RateSource.DAILY_TTL

    override suspend fun fetch(request: FetchRequest): SourceResult {
        val body = runCatching { api.fawazahmed(Endpoints.FAWAZAHMED_USD) }
            .getOrElse { api.fawazahmed(Endpoints.FAWAZAHMED_USD_FALLBACK) }
        return parse(body, request.fiat + request.crypto + request.metals)
    }

    private fun parse(body: JsonObject, wanted: Set<String>): SourceResult {
        val date = body["date"]?.jsonPrimitive?.content
        val table = body["usd"]?.jsonObject ?: error("fawazahmed0: нет объекта usd")
        val rates = mutableMapOf<String, BigDecimal>()
        for ((key, value) in table) {
            val code = key.uppercase()
            if (code !in wanted) continue
            val rate = runCatching { BigDecimal(value.jsonPrimitive.content) }.getOrNull() ?: continue
            if (rate.signum() > 0) rates[code] = rate
        }
        val asOf = date?.let {
            runCatching { LocalDate.parse(it).atStartOfDay(ZoneOffset.UTC).toInstant() }.getOrNull()
        } ?: Instant.now()
        return SourceResult(id, rates, asOf)
    }

    companion object { const val ID = "fawazahmed0" }
}
