package com.exchangerates.app.data.catalog

import androidx.test.core.app.ApplicationProvider
import com.exchangerates.app.domain.model.CurrencyKind
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Проверка сгенерированных ассетов: файлы `currencies.json` и
 * `initial_rates.json` создаются скриптом, поэтому важно ловить расхождения
 * между генератором и моделями приложения.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CurrencyCatalogTest {

    private val catalog = CurrencyCatalog(
        context = ApplicationProvider.getApplicationContext(),
        json = Json { ignoreUnknownKeys = true },
    )

    @Test
    fun `справочник содержит все мировые валюты, крипту и металлы`() = runTest {
        val all = catalog.all()
        assertTrue("валют в справочнике: ${all.size}", all.size >= 190)
        val byKind = all.groupingBy { it.kind }.eachCount()
        assertTrue("фиата: ${byKind[CurrencyKind.FIAT]}", (byKind[CurrencyKind.FIAT] ?: 0) >= 150)
        assertTrue("крипты: ${byKind[CurrencyKind.CRYPTO]}", (byKind[CurrencyKind.CRYPTO] ?: 0) >= 20)
        assertEquals(4, byKind[CurrencyKind.METAL])
    }

    @Test
    fun `ключевые валюты на месте и с русскими названиями`() = runTest {
        val byCode = catalog.byCode()
        listOf("USD", "RUB", "EUR", "TRY", "KZT", "UAH", "BYN", "UZS", "BTC", "ETH", "USDT", "XAU")
            .forEach { code -> assertNotNull("нет $code", byCode[code]) }

        val rub = byCode.getValue("RUB")
        assertEquals("российский рубль", rub.nameRu)
        assertEquals("₽", rub.symbol)
        assertEquals("RU", rub.country)
        assertEquals(2, rub.decimals)
    }

    @Test
    fun `доллар — первый по популярности, рубль в первой пятёрке`() = runTest {
        val ranked = catalog.all().sortedBy { it.rank }
        assertEquals("USD", ranked.first().code)
        assertTrue(ranked.take(5).map { it.code }.contains("RUB"))
    }

    @Test
    fun `у криптовалют больше знаков после запятой чем у фиата`() = runTest {
        val byCode = catalog.byCode()
        assertEquals(8, byCode.getValue("BTC").decimals)
        assertEquals(2, byCode.getValue("USD").decimals)
    }

    @Test
    fun `у валют без единой страны нет кода флага`() = runTest {
        val byCode = catalog.byCode()
        // франк КФА используют восемь стран — единого флага нет
        assertEquals(null, byCode.getValue("XOF").country)
        assertEquals("EU", byCode.getValue("EUR").country)
    }

    @Test
    fun `стартовый снимок курсов покрывает весь справочник`() = runTest {
        val initial = catalog.initialRates()
        val codes = catalog.codes()
        val missing = codes - initial.rates.keys
        assertTrue("нет курсов для: $missing", missing.isEmpty())
        assertEquals(java.math.BigDecimal.ONE, initial.rates.getValue("USD").stripTrailingZeros())
        assertTrue(initial.rates.getValue("RUB").toDouble() > 1.0)
        assertTrue(initial.rates.getValue("BTC").toDouble() < 0.001)
        assertTrue(initial.asOf.epochSecond > 0)
    }

    @Test
    fun `справочник кэшируется и не читается заново`() = runTest {
        val first = catalog.all()
        val second = catalog.all()
        assertTrue(first === second)
    }
}
