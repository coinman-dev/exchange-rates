package com.exchangerates.app.presentation.converter

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.exchangerates.app.R
import com.exchangerates.app.core.theme.AppDimens
import com.exchangerates.app.core.theme.ExchangeRatesTheme
import com.exchangerates.app.domain.model.Currency
import com.exchangerates.app.domain.model.CurrencyKind
import com.exchangerates.app.presentation.converter.components.CurrencyCardData
import com.exchangerates.app.presentation.converter.components.textFieldValue
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Положение списка конвертера: клавиатура не закрывает карточку, в которую
 * вводят сумму, а длинный список открывается с начала.
 *
 * Экран как у Galaxy S21 (360×800 dp): пятая карточка стоит в нижней половине,
 * как раз там, где появляется клавиатура.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w360dp-h800dp-xxhdpi")
class ConverterScreenTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var state by mutableStateOf(ConverterUiState())

    private fun fiat(code: String) = Currency(code, CurrencyKind.FIAT, null, code, code, code, 2, 1)

    private fun rows(vararg others: String) = buildList {
        add(
            CurrencyCardData(fiat("USD"), isBase = true, amountText = "100,00", unitRateText = null),
        )
        others.forEach { code ->
            add(
                CurrencyCardData(
                    currency = fiat(code),
                    isBase = false,
                    amountText = "100,00",
                    unitRateText = "1 USD → 1,0000 $code",
                ),
            )
        }
    }

    private fun showConverter() {
        compose.setContent {
            ExchangeRatesTheme {
                ConverterScreen(
                    state = state,
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
        }
        compose.waitForIdle()
    }

    private fun showConverterEditingLastCard() {
        state = ConverterUiState(
            rows = rows("RUB", "EUR", "GBP", "CHF"),
            activeCode = "CHF",
            input = textFieldValue("100"),
        )
        showConverter()
    }

    /** Клавиатуры в Robolectric нет, поэтому её отступ подаётся окну напрямую. */
    private fun showKeyboard(heightPx: Int) {
        compose.runOnUiThread {
            val insets = WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, heightPx))
                .setVisible(WindowInsetsCompat.Type.ime(), true)
                .build()
            val content = compose.activity.findViewById<View>(android.R.id.content)
            ViewCompat.dispatchApplyWindowInsets(content, insets)
        }
        compose.waitForIdle()
    }

    private fun assertActiveCardAboveOperators() {
        val rateLine = compose.onNodeWithText("1 USD → 1,0000 CHF")
        val plusLabel = compose.activity.getString(R.string.operator_plus)
        val plus = compose.onNodeWithContentDescription(plusLabel)
        rateLine.assertIsDisplayed()
        plus.assertIsDisplayed()
        val cardBottom = rateLine.getUnclippedBoundsInRoot().bottom + AppDimens.cardPaddingV
        val operatorsTop = plus.getUnclippedBoundsInRoot().top
        assertTrue(
            "низ карточки $cardBottom должен быть выше панели операторов $operatorsTop",
            cardBottom <= operatorsTop,
        )
    }

    @Test
    fun `карточка едет вверх вместе с выезжающей клавиатурой`() {
        showConverterEditingLastCard()
        for (heightPx in listOf(300, 600, 900)) {
            showKeyboard(heightPx)
        }
        assertActiveCardAboveOperators()
    }

    @Test
    fun `карточка видна, даже если клавиатура закрыла её разом`() {
        showConverterEditingLastCard()
        showKeyboard(1200)
        assertActiveCardAboveOperators()
    }

    @Test
    fun `длинный список после загрузки открывается с базовой карточки`() {
        showConverter()
        state = ConverterUiState(
            rows = rows("RUB", "EUR", "GBP", "CHF", "TRY", "KZT", "CNY", "JPY"),
        )
        compose.waitForIdle()
        compose.onNodeWithText(compose.activity.getString(R.string.you_convert)).assertIsDisplayed()
    }
}
