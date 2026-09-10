package com.exchangerates.app.domain

import com.exchangerates.app.domain.model.RateEntry
import com.exchangerates.app.domain.model.RateMode
import com.exchangerates.app.domain.model.RateTable
import java.math.BigDecimal
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RateTableTest {

    private val now = Instant.parse("2026-09-10T12:00:00Z")

    private fun table(vararg rates: Pair<String, String>) = RateTable(
        mode = RateMode.MID_MARKET,
        entries = rates.associate { (code, value) ->
            code to RateEntry(code, BigDecimal(value), "test", now)
        },
        fetchedAt = now,
        dataAsOf = now,
        isOffline = false,
    )

    @Test
    fun `опорная валюта всегда равна единице`() {
        val t = table("RUB" to "84.33")
        assertEquals(BigDecimal.ONE, t.rateToUsd("USD"))
    }

    @Test
    fun `кросс-курс через доллар`() {
        val t = table("TRY" to "48.469", "RUB" to "85.40")
        val rate = t.cross("TRY", "RUB")!!
        // 85.40 / 48.469 = 1.7619...
        assertTrue("получено $rate", rate.toDouble() in 1.761..1.763)
    }

    @Test
    fun `курс валюты к себе равен единице`() {
        assertEquals(BigDecimal.ONE, table("TRY" to "48.469").cross("TRY", "TRY"))
    }

    @Test
    fun `конвертация суммы`() {
        val t = table("RUB" to "85.00")
        val converted = t.convert(BigDecimal("100"), "USD", "RUB")!!
        // сравнение численное: 8500 и 8.5E+3 равны по значению, но не по equals
        assertEquals(0, BigDecimal("8500").compareTo(converted))
    }

    @Test
    fun `отсутствующая валюта даёт null вместо нуля`() {
        val t = table("RUB" to "85.00")
        assertNull(t.cross("USD", "ZZZ"))
        assertNull(t.convert(BigDecimal.ONE, "ZZZ", "RUB"))
    }

    @Test
    fun `изменение за сутки в процентах`() {
        val t = table("RUB" to "86.00").copy(
            previousDay = mapOf("RUB" to BigDecimal("85.00")),
        )
        val change = t.dayChangePercent("USD", "RUB")!!
        assertTrue("получено $change", change.toDouble() in 1.17..1.18)
    }

    @Test
    fun `без данных за прошлый день изменение недоступно`() {
        assertNull(table("RUB" to "86.00").dayChangePercent("USD", "RUB"))
    }

    @Test
    fun `перечень использованных источников`() {
        val t = table("RUB" to "85.00", "TRY" to "48.47")
        assertEquals(mapOf("test" to 2), t.sourcesUsed())
    }
}
