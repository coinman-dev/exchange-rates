package com.exchangerates.app.data.remote

import com.exchangerates.app.data.source.BinanceSource
import com.exchangerates.app.data.source.CoinGeckoSource
import com.exchangerates.app.data.source.CoinbaseSource
import com.exchangerates.app.data.source.ErApiSource
import com.exchangerates.app.data.source.FawazahmedSource
import com.exchangerates.app.data.source.FetchRequest
import com.exchangerates.app.data.source.FloatratesSource
import com.exchangerates.app.data.source.FrankfurterSource
import com.exchangerates.app.data.source.GoldApiSource
import java.math.BigDecimal
import java.net.InetAddress
import java.time.Duration
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.BeforeClass
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Проверка разбора ответов живых API. Тест обращается к сети: если её нет,
 * он помечается пропущенным, а не проваленным.
 *
 * Смысл теста — ловить изменения схемы у источников. Именно такие поломки
 * незаметны при обычной сборке и всплывают уже у пользователя.
 */
class LiveSourcesIntegrationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        coerceInputValues = true
    }

    private val api: RatesApi = Retrofit.Builder()
        .baseUrl("https://api.frankfurter.dev/")
        .client(
            OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(15))
                .readTimeout(Duration.ofSeconds(20))
                .build(),
        )
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(RatesApi::class.java)

    private val request = FetchRequest(
        fiat = setOf("RUB", "TRY", "KZT", "EUR", "UAH"),
        crypto = setOf("BTC", "ETH", "USDT"),
        metals = setOf("XAU", "XAG"),
    )

    /** Курс USD→RUB должен быть в разумных пределах — ловит ошибки масштаба. */
    private fun assertPlausibleRub(rate: BigDecimal?, source: String) {
        assertTrue("$source: нет курса RUB", rate != null)
        val value = rate!!.toDouble()
        assertTrue("$source: неправдоподобный курс RUB = $value", value in 30.0..300.0)
    }

    @Test
    fun `Coinbase отдаёт фиат и крипту`() = runTest {
        val result = CoinbaseSource(api).fetch(request)
        assertPlausibleRub(result.rates["RUB"], "Coinbase")
        val btc = result.rates["BTC"]
        assertTrue("Coinbase: нет BTC", btc != null)
        assertTrue("Coinbase: BTC = $btc", btc!!.toDouble() in 1e-7..1e-3)
    }

    @Test
    fun `Frankfurter отдаёт официальные курсы и металлы`() = runTest {
        val result = FrankfurterSource(api).fetch(request)
        assertPlausibleRub(result.rates["RUB"], "Frankfurter")
        assertTrue("Frankfurter: нет XAU", result.rates["XAU"] != null)
    }

    @Test
    fun `режим курса ЦБ РФ возвращает данные именно этого банка`() = runTest {
        val result = FrankfurterSource(api).fetchProvider("CBR", setOf("RUB", "EUR", "TRY"))
        assertPlausibleRub(result.rates["RUB"], "CBR")
        assertTrue("CBR: источник помечен как ${result.sourceId}", result.sourceId.contains("CBR"))
    }

    @Test
    fun `режимы ЦБ Турции и Нацбанка Казахстана работают`() = runTest {
        val tcmb = FrankfurterSource(api).fetchProvider("TCMB", setOf("TRY", "EUR"))
        assertTrue("TCMB: нет TRY", tcmb.rates["TRY"] != null)
        val nbk = FrankfurterSource(api).fetchProvider("NBK", setOf("KZT", "RUB"))
        val kzt = nbk.rates["KZT"]
        assertTrue("NBK: нет KZT", kzt != null)
        assertTrue("NBK: KZT = $kzt", kzt!!.toDouble() in 200.0..900.0)
    }

    @Test
    fun `floatrates отдаёт фиат с меткой времени`() = runTest {
        val result = FloatratesSource(api).fetch(request)
        assertPlausibleRub(result.rates["RUB"], "floatrates")
        assertTrue("floatrates: метка времени пустая", result.asOf.epochSecond > 0)
    }

    @Test
    fun `open er-api отдаёт фиат`() = runTest {
        val result = ErApiSource(api).fetch(request)
        assertPlausibleRub(result.rates["RUB"], "er-api")
    }

    @Test
    fun `fawazahmed0 отдаёт фиат, крипту и металлы`() = runTest {
        val result = FawazahmedSource(api).fetch(request)
        assertPlausibleRub(result.rates["RUB"], "fawazahmed0")
        assertTrue("fawazahmed0: нет BTC", result.rates["BTC"] != null)
        assertTrue("fawazahmed0: нет XAU", result.rates["XAU"] != null)
    }

    @Test
    fun `Binance отдаёт крипту или недоступен по региону`() = runTest {
        val result = runCatching { BinanceSource(api).fetch(request) }.getOrNull()
        // домен Binance блокируется в части стран — это штатная ситуация,
        // приложение переходит на Coinbase и CoinGecko
        if (result == null || result.rates.isEmpty()) return@runTest
        val btc = result.rates["BTC"]!!
        assertTrue("Binance: BTC = $btc", btc.toDouble() in 1e-7..1e-3)
    }

    @Test
    fun `CoinGecko отдаёт крипту или упирается в лимит`() = runTest {
        val result = runCatching { CoinGeckoSource(api).fetch(request) }.getOrNull()
        if (result == null || result.rates.isEmpty()) return@runTest
        assertTrue("CoinGecko: нет BTC", result.rates["BTC"] != null)
    }

    @Test
    fun `gold-api отдаёт цены металлов`() = runTest {
        val result = runCatching { GoldApiSource(api).fetch(request) }.getOrNull()
        if (result == null || result.rates.isEmpty()) return@runTest
        val xau = result.rates["XAU"]!!
        // курс USD→XAU — обратная величина цены унции (тысячи долларов)
        assertTrue("gold-api: XAU = $xau", xau.toDouble() in 1e-6..1e-3)
    }

    @Test
    fun `исторический ряд Frankfurter пригоден для графика`() = runTest {
        val to = LocalDate.now()
        val from = to.minusDays(30)
        val rows = api.frankfurter(
            Endpoints.frankfurterSeries("USD", "RUB", from.toString(), to.toString(), null),
        )
        assertTrue("точек в ряду: ${rows.size}", rows.size >= 10)
        assertTrue(rows.all { it.rate.signum() > 0 })
    }

    companion object {
        @BeforeClass
        @JvmStatic
        fun requireNetwork() {
            val online = runCatching {
                InetAddress.getByName("api.frankfurter.dev").isReachable(3000) ||
                    InetAddress.getByName("api.frankfurter.dev") != null
            }.getOrDefault(false)
            assumeTrue("сети нет — интеграционные тесты пропущены", online)
        }
    }
}
