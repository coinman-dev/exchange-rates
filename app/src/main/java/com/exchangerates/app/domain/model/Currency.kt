package com.exchangerates.app.domain.model

/** Тип актива. Влияет на иконку, точность и на выбор источника курса. */
enum class CurrencyKind { FIAT, CRYPTO, METAL }

/**
 * Запись справочника валют. Данные берутся из `assets/currencies.json`,
 * который генерируется скриптом `tools/gen_catalog.py` (названия — из ICU).
 */
data class Currency(
    val code: String,
    val kind: CurrencyKind,
    val country: String?,
    val nameEn: String,
    val nameRu: String,
    val symbol: String,
    val decimals: Int,
    val rank: Int,
) {
    fun displayName(russian: Boolean): String = if (russian) nameRu else nameEn

    /** Дополнительные строки для поиска: старые коды и обиходные названия. */
    val searchAliases: List<String>
        get() = ALIASES[code].orEmpty()

    private companion object {
        val ALIASES = mapOf(
            "RUB" to listOf("RUR", "рубль", "ruble", "rub"),
            "BTC" to listOf("XBT", "биткоин", "биткойн", "bitcoin"),
            "USD" to listOf("бакс", "dollar", "доллар"),
            "EUR" to listOf("евро", "euro"),
            "TRY" to listOf("TRL", "лира", "lira"),
            "KZT" to listOf("тенге", "tenge"),
            "UAH" to listOf("гривна", "hryvnia"),
            "BYN" to listOf("BYR", "белорусский"),
            "XAU" to listOf("gold", "золото"),
            "XAG" to listOf("silver", "серебро"),
            "XPT" to listOf("platinum", "платина"),
            "XPD" to listOf("palladium", "палладий"),
            "USDT" to listOf("tether", "тезер"),
            "ETH" to listOf("эфир", "ether"),
        )
    }
}
