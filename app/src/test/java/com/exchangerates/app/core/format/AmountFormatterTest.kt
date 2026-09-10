package com.exchangerates.app.core.format

import java.math.BigDecimal
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AmountFormatterTest {

    private val en = AmountFormatter(Locale.forLanguageTag("en-US"))

    @Test
    fun `суммы фиата с разделителем разрядов`() {
        assertEquals("1,000.00", en.formatAmount(BigDecimal("1000"), 2))
        assertEquals("9,241.12", en.formatAmount(BigDecimal("9241.1234"), 2))
        assertEquals("93.20", en.formatAmount(BigDecimal("93.2"), 2))
    }

    @Test
    fun `без разделителя разрядов`() {
        assertEquals("1000.00", en.formatAmount(BigDecimal("1000"), 2, grouped = false))
    }

    @Test
    fun `мелкие значения не превращаются в ноль — исправление недостатка Xe`() {
        // 1000 TRY в биткойнах: Xe показывает 0.00, здесь видны значащие цифры
        val amount = en.formatAmount(BigDecimal("0.000206"), 2)
        assertTrue("получено «$amount»", amount.startsWith("0.000206"))
        assertTrue(amount != "0.00")
    }

    @Test
    fun `настоящий ноль показывается как ноль`() {
        assertEquals("0.00", en.formatAmount(BigDecimal.ZERO, 2))
    }

    @Test
    fun `удельный курс — четыре знака как в Xe`() {
        assertEquals("0.0932", en.formatUnitRate(BigDecimal("0.09321")))
        assertEquals("0.0206", en.formatUnitRate(BigDecimal("0.020632")))
    }

    @Test
    fun `микрокурс показывается значащими цифрами вместо нулей`() {
        val text = en.formatUnitRate(BigDecimal("0.0000206"))
        assertTrue("получено «$text»", text.startsWith("0.0000206"))
    }

    @Test
    fun `большой курс округляется до двух знаков`() {
        assertEquals("9,241.12", en.formatUnitRate(BigDecimal("9241.1234")))
    }

    @Test
    fun `крипта не тянет восемь знаков для крупных значений`() {
        val text = en.formatAmount(BigDecimal("12.3456789"), 8)
        assertEquals("12.3457", text)
    }

    @Test
    fun `проценты со знаком`() {
        assertEquals("+1.24 %", en.formatPercent(BigDecimal("1.2354")))
        assertEquals("-0.50 %", en.formatPercent(BigDecimal("-0.5")))
    }

    @Test
    fun `текст для поля ввода без лишних нулей`() {
        assertEquals("20.62", en.formatForEditing(BigDecimal("20.6200"), 2))
        assertEquals("", en.formatForEditing(BigDecimal.ZERO, 2))
    }

    @Test
    fun `русская локаль использует запятую`() {
        val ru = AmountFormatter(Locale.forLanguageTag("ru-RU"))
        val text = ru.formatAmount(BigDecimal("1234.5"), 2)
        assertTrue("получено «$text»", text.contains(","))
    }
}
