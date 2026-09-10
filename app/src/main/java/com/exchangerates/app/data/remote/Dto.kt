package com.exchangerates.app.data.remote

import java.math.BigDecimal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- Coinbase: живой mid-market по фиату и крипте ----------

@Serializable
data class CoinbaseResponse(val data: CoinbaseData)

@Serializable
data class CoinbaseData(
    val currency: String,
    val rates: Map<String, @Serializable(BigDecimalSerializer::class) BigDecimal>,
)

// ---------- Frankfurter v2: официальные дневные курсы центробанков ----------

@Serializable
data class FrankfurterRate(
    val date: String,
    val base: String,
    val quote: String,
    @Serializable(BigDecimalSerializer::class) val rate: BigDecimal,
)

// ---------- floatrates ----------

@Serializable
data class FloatRate(
    val code: String,
    @Serializable(BigDecimalSerializer::class) val rate: BigDecimal,
    val date: String? = null,
)

// ---------- open.er-api.com ----------

@Serializable
data class ErApiResponse(
    val result: String,
    @SerialName("base_code") val baseCode: String = "USD",
    @SerialName("time_last_update_unix") val timeLastUpdateUnix: Long = 0,
    val rates: Map<String, @Serializable(BigDecimalSerializer::class) BigDecimal> = emptyMap(),
)

// ---------- Binance ----------

@Serializable
data class BinanceTicker(
    val symbol: String,
    @Serializable(BigDecimalSerializer::class) val price: BigDecimal,
)

// Свечи Binance приходят массивом со смешанными типами, поэтому разбираются
// как список JsonPrimitive в HistoryRepository — отдельный DTO не нужен.

// ---------- gold-api.com: живые металлы ----------

@Serializable
data class GoldApiPrice(
    val symbol: String,
    @Serializable(BigDecimalSerializer::class) val price: BigDecimal,
    val updatedAt: String? = null,
)

// ---------- CoinGecko ----------

@Serializable
data class CoinGeckoMarketChart(
    val prices: List<List<Double>> = emptyList(),
)

// ---------- манифест источников ----------

@Serializable
data class SourcesManifest(
    val version: Int = 1,
    val sources: Map<String, SourceConfig> = emptyMap(),
)

@Serializable
data class SourceConfig(
    val enabled: Boolean = true,
    val priority: Int? = null,
    val url: String? = null,
    val ttlMinutes: Int? = null,
)
