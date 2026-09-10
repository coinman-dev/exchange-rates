package com.exchangerates.app.data.remote

import kotlinx.serialization.json.JsonObject
import retrofit2.http.GET
import retrofit2.http.Url

/**
 * Все адреса передаются через @Url, потому что источников много и они
 * переопределяются манифестом без пересборки приложения.
 */
interface RatesApi {

    @GET
    suspend fun coinbase(@Url url: String): CoinbaseResponse

    @GET
    suspend fun frankfurter(@Url url: String): List<FrankfurterRate>

    @GET
    suspend fun floatrates(@Url url: String): Map<String, FloatRate>

    @GET
    suspend fun erApi(@Url url: String): ErApiResponse

    /** fawazahmed0: ключ верхнего уровня — код базовой валюты, поэтому разбираем вручную. */
    @GET
    suspend fun fawazahmed(@Url url: String): JsonObject

    /** Ответы без стабильной схемы (например, график Yahoo Finance). */
    @GET
    suspend fun rawJson(@Url url: String): JsonObject

    @GET
    suspend fun binanceTickers(@Url url: String): List<BinanceTicker>

    @GET
    suspend fun binanceKlines(@Url url: String): List<List<kotlinx.serialization.json.JsonPrimitive>>

    @GET
    suspend fun goldApi(@Url url: String): GoldApiPrice

    @GET
    suspend fun coinGeckoSimplePrice(@Url url: String): Map<String, Map<String, Double>>

    @GET
    suspend fun coinGeckoMarketChart(@Url url: String): CoinGeckoMarketChart

    @GET
    suspend fun manifest(@Url url: String): SourcesManifest
}
