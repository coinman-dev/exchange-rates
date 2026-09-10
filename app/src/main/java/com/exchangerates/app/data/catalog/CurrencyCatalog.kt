package com.exchangerates.app.data.catalog

import android.content.Context
import com.exchangerates.app.domain.model.Currency
import com.exchangerates.app.domain.model.CurrencyKind
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class CurrencyDto(
    val code: String,
    val kind: String,
    val country: String? = null,
    val nameEn: String,
    val nameRu: String,
    val symbol: String = "",
    val decimals: Int = 2,
    val rank: Int = 9999,
)

@Serializable
private data class InitialRatesDto(
    val base: String,
    val date: String,
    val source: String,
    @SerialName("rates") val rates: Map<String, Double>,
)

/** Стартовый снимок курсов, вшитый в APK: приложение считает без сети с первого запуска. */
data class InitialRates(
    val source: String,
    val asOf: Instant,
    val rates: Map<String, BigDecimal>,
)

/**
 * Справочник валют из `assets/currencies.json` (199 записей: фиат, крипта, металлы).
 * Читается один раз и держится в памяти — файл около 37 КБ.
 */
@Singleton
class CurrencyCatalog @Inject constructor(
    private val context: Context,
    private val json: Json,
) {
    @Volatile
    private var cached: List<Currency>? = null

    @Volatile
    private var cachedInitialRates: InitialRates? = null

    suspend fun all(): List<Currency> = cached ?: withContext(Dispatchers.IO) {
        val text = context.assets.open(CURRENCIES_ASSET).bufferedReader().use { it.readText() }
        val parsed = json.decodeFromString<List<CurrencyDto>>(text).map { dto ->
            Currency(
                code = dto.code,
                kind = runCatching { CurrencyKind.valueOf(dto.kind) }.getOrDefault(CurrencyKind.FIAT),
                country = dto.country,
                nameEn = dto.nameEn,
                nameRu = dto.nameRu,
                symbol = dto.symbol,
                decimals = dto.decimals,
                rank = dto.rank,
            )
        }
        cached = parsed
        parsed
    }

    suspend fun byCode(): Map<String, Currency> = all().associateBy { it.code }

    suspend fun codes(): Set<String> = all().mapTo(mutableSetOf()) { it.code }

    suspend fun initialRates(): InitialRates = cachedInitialRates ?: withContext(Dispatchers.IO) {
        val text = context.assets.open(INITIAL_RATES_ASSET).bufferedReader().use { it.readText() }
        val dto = json.decodeFromString<InitialRatesDto>(text)
        val asOf = runCatching {
            LocalDate.parse(dto.date).atStartOfDay(ZoneOffset.UTC).toInstant()
        }.getOrDefault(Instant.EPOCH)
        val result = InitialRates(
            source = dto.source,
            asOf = asOf,
            rates = dto.rates.mapValues { (_, v) -> BigDecimal.valueOf(v) },
        )
        cachedInitialRates = result
        result
    }

    private companion object {
        const val CURRENCIES_ASSET = "currencies.json"
        const val INITIAL_RATES_ASSET = "initial_rates.json"
    }
}
