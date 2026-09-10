package com.exchangerates.app.data.remote

/**
 * Базовые адреса источников. Проверены 10.09.2026; могут быть переопределены
 * удалённым манифестом (см. [SourcesManifest]) без выпуска новой версии.
 */
object Endpoints {

    const val COINBASE_USD = "https://api.coinbase.com/v2/exchange-rates?currency=USD"

    const val FRANKFURTER_USD = "https://api.frankfurter.dev/v2/rates?base=USD"

    fun frankfurterProvider(providerKey: String): String =
        "https://api.frankfurter.dev/v2/rates?base=USD&providers=$providerKey"

    fun frankfurterSeries(base: String, quote: String, from: String, to: String, group: String?): String =
        buildString {
            append("https://api.frankfurter.dev/v2/rates?base=$base&quotes=$quote&from=$from&to=$to")
            if (group != null) append("&group=$group")
        }

    const val FLOATRATES_USD = "https://www.floatrates.com/daily/usd.json"

    const val ER_API_USD = "https://open.er-api.com/v6/latest/USD"

    const val FAWAZAHMED_USD =
        "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/usd.json"

    const val FAWAZAHMED_USD_FALLBACK =
        "https://latest.currency-api.pages.dev/v1/currencies/usd.json"

    fun binanceTickers(symbols: List<String>): String {
        val list = symbols.joinToString(",") { "%22$it%22" }
        return "https://api.binance.com/api/v3/ticker/price?symbols=%5B$list%5D"
    }

    fun binanceKlines(symbol: String, interval: String, limit: Int): String =
        "https://api.binance.com/api/v3/klines?symbol=$symbol&interval=$interval&limit=$limit"

    fun goldApi(symbol: String): String = "https://api.gold-api.com/price/$symbol"

    fun coinGeckoSimplePrice(ids: List<String>): String =
        "https://api.coingecko.com/api/v3/simple/price?ids=${ids.joinToString(",")}&vs_currencies=usd"

    fun coinGeckoMarketChart(id: String, days: Int): String =
        "https://api.coingecko.com/api/v3/coins/$id/market_chart?vs_currency=usd&days=$days"

    fun yahooChart(pair: String, range: String, interval: String): String =
        "https://query1.finance.yahoo.com/v8/finance/chart/$pair=X?range=$range&interval=$interval"

    /** Манифест источников. Пока не опубликован — при 404 используется вшитый по умолчанию. */
    const val MANIFEST =
        "https://raw.githubusercontent.com/exchange-rates-app/config/main/sources_manifest.json"
}
