package com.exchangerates.app.presentation.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.input.TextFieldValue
import com.exchangerates.app.core.theme.ExchangeRatesTheme
import com.exchangerates.app.domain.model.Currency
import com.exchangerates.app.domain.model.CurrencyKind
import com.exchangerates.app.presentation.converter.components.CurrencyCard
import com.exchangerates.app.presentation.converter.components.CurrencyCardData
import com.exchangerates.app.presentation.converter.components.MathOperator
import com.exchangerates.app.presentation.converter.components.OperatorBar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Проверка карточки валюты и панели операторов без устройства:
 * отрисовка суммы, удельного курса, кнопки очистки и коллбэков.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CurrencyCardTest {

    @get:Rule
    val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val usd = Currency("USD", CurrencyKind.FIAT, "US", "US Dollar", "доллар США", "$", 2, 1)
    private val rub = Currency("RUB", CurrencyKind.FIAT, "RU", "Russian Ruble", "российский рубль", "₽", 2, 3)

    private fun data(
        currency: Currency = rub,
        isBase: Boolean = false,
        amount: String = "8,433.00",
        unitRate: String? = "1 USD → 84.3300 RUB",
        change: String? = null,
    ) = CurrencyCardData(
        currency = currency,
        isBase = isBase,
        amountText = amount,
        unitRateText = unitRate,
        changePercentText = change,
    )

    private fun card(
        cardData: CurrencyCardData = data(),
        isActive: Boolean = false,
        input: TextFieldValue = TextFieldValue(""),
        onSelect: () -> Unit = {},
        onClear: () -> Unit = {},
        onPickCurrency: () -> Unit = {},
    ) {
        compose.setContent {
            ExchangeRatesTheme {
                CurrencyCard(
                    data = cardData,
                    isActive = isActive,
                    input = input,
                    inputPristine = true,
                    inputError = false,
                    baseLabel = "Вы конвертируете",
                    noRateLabel = "нет курса",
                    onSelect = onSelect,
                    onLongPress = {},
                    onPickCurrency = onPickCurrency,
                    onInputChange = {},
                    onCommit = {},
                    onClear = onClear,
                )
            }
        }
    }

    @Test
    fun `пассивная карточка показывает сумму и удельный курс`() {
        card()
        compose.onNodeWithText("8,433.00").assertIsDisplayed()
        compose.onNodeWithText("1 USD → 84.3300 RUB").assertIsDisplayed()
        compose.onNodeWithText("RUB").assertIsDisplayed()
    }

    @Test
    fun `базовая карточка подписана и без удельного курса`() {
        card(data(currency = usd, isBase = true, amount = "100.00", unitRate = null))
        compose.onNodeWithText("Вы конвертируете").assertIsDisplayed()
        compose.onNodeWithText("100.00").assertIsDisplayed()
    }

    @Test
    fun `нажатие на сумму выбирает карточку`() {
        var selected = false
        card(onSelect = { selected = true })
        compose.onNodeWithText("8,433.00").performClick()
        assertTrue(selected)
    }

    @Test
    fun `нажатие на код валюты открывает выбор валюты`() {
        var picked = false
        card(onPickCurrency = { picked = true })
        compose.onNodeWithText("RUB").performClick()
        assertTrue(picked)
    }

    @Test
    fun `активная карточка показывает введённое выражение`() {
        card(isActive = true, input = TextFieldValue("20.62+50"))
        compose.onNodeWithText("20.62+50").assertIsDisplayed()
    }

    @Test
    fun `в карточке без курса выводится пояснение`() {
        card(data(amount = "", unitRate = null).copy(hasRate = false))
        compose.onNodeWithText("нет курса").assertIsDisplayed()
    }

    @Test
    fun `изменение за сутки выводится в той же строке, что и курс`() {
        // курс и изменение собраны в один Text: иначе на узком экране
        // изменение переносилось по одному символу в столбик
        card(data(change = "+1.24 %"))
        compose.onNodeWithText("+1.24 %", substring = true).assertIsDisplayed()
        compose.onNodeWithText("1 USD → 84.3300 RUB", substring = true).assertIsDisplayed()
    }

    @Test
    fun `панель операторов сообщает нажатую кнопку`() {
        val pressed = mutableListOf<MathOperator>()
        compose.setContent {
            ExchangeRatesTheme {
                OperatorBar(onOperator = { pressed += it })
            }
        }
        val plus = compose.activity.getString(com.exchangerates.app.R.string.operator_plus)
        val equals = compose.activity.getString(com.exchangerates.app.R.string.operator_equals)
        compose.onNodeWithContentDescription(plus).performClick()
        compose.onNodeWithContentDescription(equals).performClick()
        assertEquals(listOf(MathOperator.PLUS, MathOperator.EQUALS), pressed)
    }
}
