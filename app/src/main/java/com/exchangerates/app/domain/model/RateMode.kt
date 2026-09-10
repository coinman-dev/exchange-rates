package com.exchangerates.app.domain.model

/**
 * Режим курса. Mid-market — рыночная середина (как в Xe); остальные —
 * официальные курсы центробанков (данные Frankfurter с фильтром по провайдеру).
 */
enum class RateMode(val id: String, val providerKey: String?) {
    MID_MARKET("mid", null),
    CBR("cbr", "CBR"),
    TCMB("tcmb", "TCMB"),
    NBK("nbk", "NBK"),
    ;

    val isCentralBank: Boolean get() = providerKey != null

    companion object {
        fun fromId(id: String?): RateMode = entries.firstOrNull { it.id == id } ?: MID_MARKET
    }
}
