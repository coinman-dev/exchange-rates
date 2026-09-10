package com.exchangerates.app.core.math

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpressionEvaluatorTest {

    private fun value(input: String): BigDecimal {
        val result = ExpressionEvaluator.evaluate(input)
        assertTrue("ожидался успех для «$input», получено $result", result is ExpressionEvaluator.Result.Success)
        return (result as ExpressionEvaluator.Result.Success).value
    }

    private fun assertValue(expected: String, input: String) {
        assertEquals("для «$input»", BigDecimal(expected).stripTrailingZeros(), value(input).stripTrailingZeros())
    }

    @Test
    fun `простые числа и разделители`() {
        assertValue("1000", "1000")
        assertValue("20.62", "20.62")
        assertValue("20.62", "20,62")
        assertValue("1000.5", "1 000,5")
        assertValue("0.5", ".5")
    }

    @Test
    fun `выражение со скриншота Xe`() {
        assertValue("70.62", "20.62+50")
    }

    @Test
    fun `приоритет операций и скобки`() {
        assertValue("14", "2+3*4")
        assertValue("20", "(2+3)*4")
        assertValue("2", "8/4")
        assertValue("-6", "-2*3")
        assertValue("7", "1+2*3")
    }

    @Test
    fun `нормализация символов умножения и деления`() {
        assertValue("6", "2×3")
        assertValue("3", "6÷2")
        assertValue("1", "3−2")
    }

    @Test
    fun `проценты считаются как в калькуляторе`() {
        assertValue("110", "100+10%")
        assertValue("90", "100-10%")
        assertValue("10", "100*10%")
        assertValue("1000", "100/10%")
        assertValue("0.1", "10%")
    }

    @Test
    fun `деление на ноль возвращает ошибку`() {
        val result = ExpressionEvaluator.evaluate("5/0")
        assertEquals(
            ExpressionEvaluator.Result.Error(ExpressionEvaluator.ErrorKind.DIVISION_BY_ZERO),
            result,
        )
    }

    @Test
    fun `мусор возвращает синтаксическую ошибку`() {
        listOf("2++", "abc", "()", "(1+2", "2*/3").forEach { input ->
            val result = ExpressionEvaluator.evaluate(input)
            assertTrue(
                "ожидалась ошибка для «$input», получено $result",
                result is ExpressionEvaluator.Result.Error,
            )
        }
    }

    @Test
    fun `пустой ввод отличается от нуля`() {
        assertEquals(ExpressionEvaluator.Result.Empty, ExpressionEvaluator.evaluate(""))
        assertEquals(ExpressionEvaluator.Result.Empty, ExpressionEvaluator.evaluate("   "))
    }

    @Test
    fun `частичный ввод отбрасывает висящий оператор`() {
        val result = ExpressionEvaluator.evaluatePartial("20.62+")
        assertEquals(
            BigDecimal("20.62").stripTrailingZeros(),
            (result as ExpressionEvaluator.Result.Success).value.stripTrailingZeros(),
        )
    }

    @Test
    fun `частичный ввод закрывает незакрытые скобки`() {
        val result = ExpressionEvaluator.evaluatePartial("(2+3")
        assertEquals(
            BigDecimal("5"),
            (result as ExpressionEvaluator.Result.Success).value.stripTrailingZeros(),
        )
    }

    @Test
    fun `слишком длинный ввод отклоняется`() {
        val long = "1+".repeat(40) + "1"
        assertEquals(
            ExpressionEvaluator.Result.Error(ExpressionEvaluator.ErrorKind.TOO_LONG),
            ExpressionEvaluator.evaluate(long),
        )
    }

    @Test
    fun `деление сохраняет точность без переполнения`() {
        // 1/3 не представимо конечной дробью: важно, что не бросается исключение
        val result = value("1/3")
        assertTrue(result.toPlainString().startsWith("0.333333"))
    }

    @Test
    fun `сумма с разделителем разрядов участвует в выражении`() {
        // до исправления «8,433.00+50» ломало разбор: запятая превращалась в точку
        assertValue("8483", "8,433.00+50")
        assertValue("8483", "8 433,00+50")
        assertValue("1234567.5", "1,234,567.5")
        assertValue("1234567", "1.234.567")
    }

    @Test
    fun `одиночная запятая — десятичный разделитель`() {
        assertValue("1.5", "1,5")
        assertValue("4", "1,5+2,5")
    }

    @Test
    fun `смешанные разделители — последний считается десятичным`() {
        assertValue("8433.21", "8,433.21")
        assertValue("8433.21", "8.433,21")
    }

    @Test
    fun `число с двумя десятичными разделителями отклоняется`() {
        assertTrue(
            ExpressionEvaluator.evaluate("8,433.21.5") is ExpressionEvaluator.Result.Error,
        )
    }

    @Test
    fun `распознавание выражения`() {
        assertTrue(ExpressionEvaluator.isExpression("20+5"))
        assertTrue(ExpressionEvaluator.isExpression("10%"))
        assertTrue(!ExpressionEvaluator.isExpression("1000"))
        assertTrue(!ExpressionEvaluator.isExpression("-1000"))
    }
}
