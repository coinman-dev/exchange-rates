package com.exchangerates.app.data.source

import com.exchangerates.app.data.remote.Endpoints
import com.exchangerates.app.data.remote.RatesApi
import com.exchangerates.app.domain.model.CurrencyKind
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

private val MC = MathContext(24, RoundingMode.HALF_EVEN)

/**
 * Binance: самые быстрые курсы крипты и свечи для графиков.
 * Цены даны в USDT; отличие USDT от USD (~0.03%) меньше типичной волатильности,
 * поэтому принимается 1:1 — это отражено в диалоге «Об источниках».
 *
 * Домен api.binance.com блокируется в ряде стран, поэтому источник не единственный.
 */
class BinanceSource @Inject constructor(
    private val api: RatesApi,
) : RateSource {
    override val id = ID
    override val title = "Binance"
    override val priorities = mapOf(CurrencyKind.CRYPTO to 10)
    override val ttl: Duration = Duration.ofMinutes(5)

    override suspend fun fetch(request: FetchRequest): SourceResult {
        val codes = request.crypto.filter { it != USDT }
        if (codes.isEmpty()) return SourceResult(id, emptyMap(), Instant.now())
        val symbols = codes.map { "${it}$USDT" }
        val tickers = api.binanceTickers(Endpoints.binanceTickers(symbols))
        val rates = mutableMapOf<String, BigDecimal>()
        for (ticker in tickers) {
            val code = ticker.symbol.removeSuffix(USDT)
            if (code.isEmpty() || ticker.price.signum() <= 0) continue
            rates[code] = BigDecimal.ONE.divide(ticker.price, MC)
        }
        if (USDT in request.crypto) rates[USDT] = BigDecimal.ONE
        return SourceResult(id, rates, Instant.now())
    }

    companion object {
        const val ID = "binance"
        private const val USDT = "USDT"
    }
}

/** CoinGecko: резерв по крипте и источник её истории для графиков. */
class CoinGeckoSource @Inject constructor(
    private val api: RatesApi,
) : RateSource {
    override val id = ID
    override val title = "CoinGecko"
    override val priorities = mapOf(CurrencyKind.CRYPTO to 30)
    override val ttl: Duration = Duration.ofMinutes(15)

    override suspend fun fetch(request: FetchRequest): SourceResult {
        val ids = request.crypto.mapNotNull { code -> COIN_IDS[code]?.let { code to it } }
        if (ids.isEmpty()) return SourceResult(id, emptyMap(), Instant.now())
        val response = api.coinGeckoSimplePrice(Endpoints.coinGeckoSimplePrice(ids.map { it.second }))
        val rates = mutableMapOf<String, BigDecimal>()
        for ((code, coinId) in ids) {
            val usd = response[coinId]?.get("usd") ?: continue
            if (usd <= 0.0) continue
            rates[code] = BigDecimal.ONE.divide(BigDecimal.valueOf(usd), MC)
        }
        return SourceResult(id, rates, Instant.now())
    }

    fun coinId(code: String): String? = COIN_IDS[code]

    companion object {
        const val ID = "coingecko"

        /** Код валюты -> идентификатор монеты в CoinGecko. */
        val COIN_IDS = mapOf(
            "BTC" to "bitcoin", "ETH" to "ethereum", "USDT" to "tether",
            "USDC" to "usd-coin", "BNB" to "binancecoin", "SOL" to "solana",
            "XRP" to "ripple", "TON" to "the-open-network", "TRX" to "tron",
            "ADA" to "cardano", "DOGE" to "dogecoin", "LTC" to "litecoin",
            "DOT" to "polkadot", "AVAX" to "avalanche-2", "LINK" to "chainlink",
            "XMR" to "monero", "BCH" to "bitcoin-cash", "XLM" to "stellar",
            "ATOM" to "cosmos", "NEAR" to "near", "SUI" to "sui",
            "APT" to "aptos", "UNI" to "uniswap", "ETC" to "ethereum-classic",
            "SHIB" to "shiba-inu", "ARB" to "arbitrum", "OP" to "optimism",
            "FIL" to "filecoin", "ICP" to "internet-computer", "HBAR" to "hedera-hashgraph",
            "ALGO" to "algorand", "XTZ" to "tezos", "AAVE" to "aave", "PAXG" to "pax-gold",
        )
    }
}

/**
 * gold-api.com: живые цены металлов без ключа. Отдаёт цену за тройскую унцию
 * в USD, поэтому курс USD→XAU — обратная величина.
 */
class GoldApiSource @Inject constructor(
    private val api: RatesApi,
) : RateSource {
    override val id = ID
    override val title = "Gold-API"
    override val priorities = mapOf(CurrencyKind.METAL to 10)
    override val ttl: Duration = Duration.ofMinutes(30)

    override suspend fun fetch(request: FetchRequest): SourceResult = coroutineScope {
        val supported = request.metals.filter { it in SYMBOLS }
        val results = supported.map { symbol ->
            async {
                runCatching {
                    val price = api.goldApi(Endpoints.goldApi(symbol))
                    if (price.price.signum() <= 0) null
                    else symbol to BigDecimal.ONE.divide(price.price, MC)
                }.getOrNull()
            }
        }.mapNotNull { it.await() }
        SourceResult(id, results.toMap(), Instant.now())
    }

    companion object {
        const val ID = "goldapi"
        val SYMBOLS = setOf("XAU", "XAG", "XPT", "XPD")
    }
}
