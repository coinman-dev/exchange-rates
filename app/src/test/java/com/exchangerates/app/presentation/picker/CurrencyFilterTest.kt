package com.exchangerates.app.presentation.picker

import com.exchangerates.app.domain.model.Currency
import com.exchangerates.app.domain.model.CurrencyKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyFilterTest {

    private fun currency(
        code: String,
        kind: CurrencyKind = CurrencyKind.FIAT,
        nameEn: String = code,
        nameRu: String = code,
        symbol: String = "",
        rank: Int = 100,
    ) = Currency(code, kind, null, nameEn, nameRu, symbol, 2, rank)

    private val catalog = listOf(
        currency("USD", nameEn = "US Dollar", nameRu = "доллар США", symbol = "$", rank = 1),
        currency("EUR", nameEn = "Euro", nameRu = "евро", symbol = "€", rank = 2),
        currency("RUB", nameEn = "Russian Ruble", nameRu = "российский рубль", symbol = "₽", rank = 3),
        currency("TRY", nameEn = "Turkish Lira", nameRu = "турецкая лира", symbol = "₺", rank = 4),
        currency("BTC", CurrencyKind.CRYPTO, "Bitcoin", "Биткойн", "₿", rank = 51),
        currency("XAU", CurrencyKind.METAL, "Gold (troy ounce)", "Золото (тройская унция)", rank = 54),
        currency("RSD", nameEn = "Serbian Dinar", nameRu = "сербский динар", rank = 900),
    )

    @Test
    fun `без запроса популярный раздел ограничен рангом`() {
        val result = CurrencyFilter.apply(catalog, "", PickerFilter.POPULAR, russian = true)
        assertTrue(result.all { it.rank <= CurrencyFilter.POPULAR_LIMIT })
        assertEquals("USD", result.first().code)
    }

    @Test
    fun `раздел криптовалют содержит только крипту`() {
        val result = CurrencyFilter.apply(catalog, "", PickerFilter.CRYPTO, russian = true)
        assertEquals(listOf("BTC"), result.map { it.code })
    }

    @Test
    fun `раздел металлов содержит только металлы`() {
        val result = CurrencyFilter.apply(catalog, "", PickerFilter.METALS, russian = true)
        assertEquals(listOf("XAU"), result.map { it.code })
    }

    @Test
    fun `точное совпадение кода поднимается наверх`() {
        val result = CurrencyFilter.apply(catalog, "rub", PickerFilter.ALL, russian = true)
        assertEquals("RUB", result.first().code)
    }

    @Test
    fun `поиск по русскому названию`() {
        val result = CurrencyFilter.apply(catalog, "турецк", PickerFilter.ALL, russian = true)
        assertEquals("TRY", result.first().code)
    }

    @Test
    fun `поиск по английскому названию при русском интерфейсе`() {
        val result = CurrencyFilter.apply(catalog, "dollar", PickerFilter.ALL, russian = true)
        assertEquals("USD", result.first().code)
    }

    @Test
    fun `устаревший код RUR находит рубль`() {
        val result = CurrencyFilter.apply(catalog, "RUR", PickerFilter.ALL, russian = true)
        assertEquals("RUB", result.first().code)
    }

    @Test
    fun `код XBT находит биткойн — как обозначает его Xe`() {
        val result = CurrencyFilter.apply(catalog, "xbt", PickerFilter.ALL, russian = true)
        assertEquals("BTC", result.first().code)
    }

    @Test
    fun `поиск по символу валюты`() {
        val result = CurrencyFilter.apply(catalog, "₽", PickerFilter.ALL, russian = true)
        assertEquals("RUB", result.first().code)
    }

    @Test
    fun `поиск игнорирует выбранный раздел`() {
        // ищем крипту, находясь в разделе «Валюты»
        val result = CurrencyFilter.apply(catalog, "bitcoin", PickerFilter.FIAT, russian = true)
        assertEquals("BTC", result.first().code)
    }

    @Test
    fun `несовпадающий запрос даёт пустой список`() {
        assertTrue(CurrencyFilter.apply(catalog, "zzzz", PickerFilter.ALL, russian = true).isEmpty())
    }

    @Test
    fun `при равном приоритете сортировка по популярности`() {
        val result = CurrencyFilter.apply(catalog, "r", PickerFilter.ALL, russian = true)
        // RUB (ранг 3) должен идти раньше RSD (ранг 900)
        val codes = result.map { it.code }
        assertTrue("получено $codes", codes.indexOf("RUB") < codes.indexOf("RSD"))
    }
}
