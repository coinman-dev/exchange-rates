package com.exchangerates.app.presentation.converter

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.exchangerates.app.presentation.converter.components.textFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class PristineInputTest {

    private fun edit(old: String, new: String) =
        editPristineInput(textFieldValue(old), textFieldValue(new))

    @Test
    fun `цифра начинает новое число`() {
        val result = edit("8433,05", "8433,055")
        assertEquals("5", result.text)
        assertEquals(TextRange(1), result.selection)
    }

    @Test
    fun `цифра стирает старое значение, где бы ни стоял курсор`() {
        val typedInMiddle = TextFieldValue("1700", selection = TextRange(2))
        assertEquals("7", editPristineInput(textFieldValue("100"), typedInMiddle).text)
    }

    @Test
    fun `десятичный разделитель тоже начинает новое число`() {
        assertEquals(",", edit("100", "100,").text)
        assertEquals(".", edit("100", "100.").text)
    }

    @Test
    fun `вставка числа из буфера заменяет старое значение`() {
        assertEquals("2500", edit("100", "1002500").text)
    }

    @Test
    fun `знак действия продолжает старое значение`() {
        for (sign in listOf("+", "-", "*", "/", "×", "÷", "−", "%")) {
            assertEquals("100$sign", edit("100", "100$sign").text)
        }
    }

    @Test
    fun `стирание работает как в обычном поле`() {
        assertEquals("10", edit("100", "10").text)
    }

    @Test
    fun `замена выделенного работает как в обычном поле`() {
        assertEquals("12345", edit("100", "12345").text)
        assertEquals("150", edit("100", "150").text)
    }

    @Test
    fun `в пустое поле цифра вводится как есть`() {
        assertEquals("5", edit("", "5").text)
    }
}
