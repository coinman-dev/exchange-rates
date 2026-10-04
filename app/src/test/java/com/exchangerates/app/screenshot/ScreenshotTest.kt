package com.exchangerates.app.screenshot

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.exchangerates.app.core.theme.ExchangeRatesTheme
import com.exchangerates.app.core.theme.ThemeMode
import com.exchangerates.app.domain.model.Currency
import com.exchangerates.app.domain.model.CurrencyKind
import com.exchangerates.app.domain.model.RateMode
import com.exchangerates.app.presentation.converter.ConverterScreen
import com.exchangerates.app.presentation.converter.ConverterUiState
import com.exchangerates.app.data.local.AppSettings
import com.exchangerates.app.presentation.converter.components.CurrencyCardData
import com.exchangerates.app.presentation.more.MoreScreen
import com.exchangerates.app.presentation.more.MoreUiState
import com.exchangerates.app.presentation.picker.CurrencyPickerContent
import java.io.File
import java.time.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Рендер экранов в PNG на JVM — так вёрстку можно проверить без телефона.
 *
 * Параметры экрана заданы как у Galaxy S21 (1080×2400, xxhdpi → 360×800 dp),
 * язык русский: именно в этой комбинации подписи не помещались в строку.
 * Файлы складываются в `app/build/screenshots`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "ru-rRU-w360dp-h800dp-xxhdpi")
class ScreenshotTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val outputDir = File("build/screenshots").apply { mkdirs() }

    private val usd = Currency("USD", CurrencyKind.FIAT, "US", "US Dollar", "доллар США", "$", 2, 1)
    private val rub = Currency("RUB", CurrencyKind.FIAT, "RU", "Russian Ruble", "российский рубль", "₽", 2, 3)
    private val eur = Currency("EUR", CurrencyKind.FIAT, "EU", "Euro", "евро", "€", 2, 2)
    private val btc = Currency("BTC", CurrencyKind.CRYPTO, null, "Bitcoin", "Биткойн", "₿", 8, 51)
    private val xau = Currency("XAU", CurrencyKind.METAL, null, "Gold", "Золото (тройская унция)", "oz t", 4, 54)

    private fun state(mode: RateMode = RateMode.MID_MARKET) = ConverterUiState(
        rows = listOf(
            CurrencyCardData(usd, isBase = true, amountText = "100,00", unitRateText = null),
            CurrencyCardData(
                currency = rub,
                isBase = false,
                amountText = "8 405,37",
                unitRateText = "1 USD → 84,0537 RUB",
                changePercentText = "-2,24 %",
                changePositive = false,
            ),
            CurrencyCardData(
                currency = eur,
                isBase = false,
                amountText = "85,93",
                unitRateText = "1 USD → 0,8593 EUR",
                changePercentText = "+0,12 %",
            ),
            CurrencyCardData(
                currency = btc,
                isBase = false,
                amountText = "0,0012834",
                unitRateText = "1 USD → 0,00001283 BTC",
            ),
            CurrencyCardData(
                currency = xau,
                isBase = false,
                amountText = "0,0228",
                unitRateText = "1 USD → 0,000228 XAU",
            ),
        ),
        rateMode = mode,
        fetchedAt = Instant.parse("2026-09-10T12:53:00Z"),
        dataAsOf = Instant.parse("2026-09-10T12:53:00Z"),
        catalog = listOf(usd, eur, rub, btc, xau),
        russian = true,
    )

    private fun capture(name: String, content: @Composable () -> Unit) {
        compose.setContent {
            ExchangeRatesTheme(themeMode = ThemeMode.DARK) {
                Box(modifier = Modifier.fillMaxSize()) { content() }
            }
        }
        compose.waitForIdle()
        val bitmap: Bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(outputDir, "$name.png").outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        println("скриншот: ${File(outputDir, "$name.png").absolutePath} (${bitmap.width}x${bitmap.height})")
    }

    @Composable
    private fun Converter(uiState: ConverterUiState = state()) {
        ConverterScreen(
            state = uiState,
            onCardSelected = {},
            onInputChanged = {},
            onOperator = {},
            onCommit = {},
            onClearInput = {},
            onDismissEditing = {},
            onAddCurrency = {},
            onReplaceCurrency = { _, _ -> },
            onRemoveCurrency = {},
            onSetBase = {},
            onMove = { _, _ -> },
            onRefresh = {},
            onOpenChart = {},
            onRateModeChange = {},
        )
    }

    @Test
    fun `конвертер, тёмная тема, русский язык`() {
        capture("converter-dark-ru") { Converter() }
    }

    @Test
    fun `конвертер с шестью знаками после запятой`() {
        // самые длинные суммы на узком экране: шрифт должен уменьшиться, а не обрезать число
        val long = listOf("1 234,567891", "103 146,654321", "12 345 678,987654")
        val rows = state().rows.mapIndexed { index, row ->
            long.getOrNull(index)?.let { row.copy(amountText = it) } ?: row
        }
        capture("converter-six-decimals-ru") { Converter(state().copy(rows = rows)) }
    }

    @Test
    fun `конвертер в режиме курса ЦБ`() {
        capture("converter-cbr-ru") { Converter(state(RateMode.CBR)) }
    }

    @Test
    fun `настройки, тёмная тема, русский язык`() {
        capture("settings-dark-ru") {
            MoreScreen(
                state = MoreUiState(settings = AppSettings()),
                onTheme = {},
                onLanguage = {},
                onDecimals = {},
                onGrouping = {},
                onShowChange = {},
                onRateMode = {},
                onWifiOnly = {},
                onInterval = {},
                onRefreshNow = {},
            )
        }
    }

    @Test
    fun `выбор валюты`() {
        capture("picker-dark-ru") {
            CurrencyPickerContent(
                currencies = listOf(usd, eur, rub, btc, xau),
                russian = true,
                alreadyAdded = setOf("USD", "RUB"),
                title = "Добавить валюту",
                onSelect = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    @Test
    fun `конвертер, светлая тема`() {
        compose.setContent {
            ExchangeRatesTheme(themeMode = ThemeMode.LIGHT) {
                Box(modifier = Modifier.fillMaxSize()) { Converter() }
            }
        }
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(outputDir, "converter-light-ru.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        println("скриншот: ${File(outputDir, "converter-light-ru.png").absolutePath}")
    }
}
