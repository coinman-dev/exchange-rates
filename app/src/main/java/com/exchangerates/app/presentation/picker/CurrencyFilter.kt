package com.exchangerates.app.presentation.picker

import com.exchangerates.app.domain.model.Currency
import com.exchangerates.app.domain.model.CurrencyKind

/** Раздел списка в диалоге выбора валюты. */
enum class PickerFilter { POPULAR, ALL, FIAT, CRYPTO, METALS }

/**
 * Поиск и фильтрация валют. Вынесено из UI, чтобы порядок выдачи
 * (точное совпадение кода → начало кода → название → алиас) проверялся тестами.
 */
object CurrencyFilter {

    const val POPULAR_LIMIT = 55

    fun apply(
        currencies: List<Currency>,
        query: String,
        filter: PickerFilter,
        russian: Boolean,
    ): List<Currency> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return byKind(currencies, filter).sortedBy { it.rank }
        }
        val needle = trimmed.lowercase()
        // при активном поиске тип актива не ограничиваем: пользователь ищет
        // конкретную валюту и не должен помнить, в каком она разделе
        return currencies
            .mapNotNull { currency ->
                score(currency, needle, russian)?.let { currency to it }
            }
            .sortedWith(compareBy({ it.second }, { it.first.rank }))
            .map { it.first }
    }

    private fun byKind(currencies: List<Currency>, filter: PickerFilter): List<Currency> =
        when (filter) {
            PickerFilter.ALL -> currencies
            PickerFilter.POPULAR -> currencies.filter { it.rank <= POPULAR_LIMIT }
            PickerFilter.FIAT -> currencies.filter { it.kind == CurrencyKind.FIAT }
            PickerFilter.CRYPTO -> currencies.filter { it.kind == CurrencyKind.CRYPTO }
            PickerFilter.METALS -> currencies.filter { it.kind == CurrencyKind.METAL }
        }

    fun score(currency: Currency, needle: String, russian: Boolean): Int? {
        val code = currency.code.lowercase()
        if (code == needle) return 0
        if (code.startsWith(needle)) return 1
        val primary = currency.displayName(russian).lowercase()
        if (primary.startsWith(needle)) return 2
        val secondary = currency.displayName(!russian).lowercase()
        if (secondary.startsWith(needle)) return 3
        if (currency.searchAliases.any { it.lowercase().startsWith(needle) }) return 4
        if (primary.contains(needle) || secondary.contains(needle)) return 5
        if (currency.symbol.isNotEmpty() && currency.symbol.lowercase() == needle) return 6
        if (code.contains(needle)) return 7
        return null
    }
}
