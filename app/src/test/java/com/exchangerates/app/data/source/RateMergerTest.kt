package com.exchangerates.app.data.source

import com.exchangerates.app.domain.model.CurrencyKind
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RateMergerTest {

    private val now = Instant.parse("2026-09-10T12:00:00Z")

    private fun candidate(
        id: String,
        priority: Int,
        kind: CurrencyKind = CurrencyKind.FIAT,
        asOf: Instant = now,
        ttl: Duration = Duration.ofMinutes(20),
        vararg rates: Pair<String, String>,
    ) = SourceCandidate(
        sourceId = id,
        priorities = mapOf(kind to priority),
        ttl = ttl,
        result = SourceResult(id, rates.associate { it.first to BigDecimal(it.second) }, asOf),
    )

    @Test
    fun `выбирается источник с наивысшим приоритетом`() {
        val merged = RateMerger.merge(
            kinds = mapOf("TRY" to CurrencyKind.FIAT),
            candidates = listOf(
                candidate("slow", 50, rates = arrayOf("TRY" to "48.00")),
                candidate("fast", 10, rates = arrayOf("TRY" to "48.50")),
            ),
            referenceSourceId = null,
            now = now,
        )
        assertEquals("fast", merged.getValue("TRY").sourceId)
        assertEquals(BigDecimal("48.50"), merged.getValue("TRY").rate)
    }

    @Test
    fun `источник без данных по валюте пропускается`() {
        val merged = RateMerger.merge(
            kinds = mapOf("TRY" to CurrencyKind.FIAT, "KZT" to CurrencyKind.FIAT),
            candidates = listOf(
                candidate("fast", 10, rates = arrayOf("TRY" to "48.50")),
                candidate("wide", 50, rates = arrayOf("TRY" to "48.00", "KZT" to "455.0")),
            ),
            referenceSourceId = null,
            now = now,
        )
        assertEquals("fast", merged.getValue("TRY").sourceId)
        assertEquals("wide", merged.getValue("KZT").sourceId)
    }

    @Test
    fun `устаревший источник не используется`() {
        val merged = RateMerger.merge(
            kinds = mapOf("TRY" to CurrencyKind.FIAT),
            candidates = listOf(
                candidate(
                    "stale", 10,
                    asOf = now.minus(Duration.ofDays(3)),
                    ttl = Duration.ofMinutes(20),
                    rates = arrayOf("TRY" to "40.00"),
                ),
                candidate("fresh", 50, rates = arrayOf("TRY" to "48.50")),
            ),
            referenceSourceId = null,
            now = now,
        )
        assertEquals("fresh", merged.getValue("TRY").sourceId)
    }

    @Test
    fun `значение с грубым отклонением от эталона отбрасывается`() {
        // «быстрый» источник вернул курс за 100 единиц — ошибка масштаба
        val merged = RateMerger.merge(
            kinds = mapOf("TRY" to CurrencyKind.FIAT),
            candidates = listOf(
                candidate("fast", 10, rates = arrayOf("TRY" to "4846.9")),
                candidate("second", 20, rates = arrayOf("TRY" to "48.47")),
                candidate("frankfurter", 30, rates = arrayOf("TRY" to "48.469")),
            ),
            referenceSourceId = "frankfurter",
            now = now,
        )
        assertEquals("second", merged.getValue("TRY").sourceId)
    }

    @Test
    fun `небольшое расхождение с эталоном допустимо`() {
        val merged = RateMerger.merge(
            kinds = mapOf("TRY" to CurrencyKind.FIAT),
            candidates = listOf(
                candidate("fast", 10, rates = arrayOf("TRY" to "49.20")),
                candidate("frankfurter", 30, rates = arrayOf("TRY" to "48.469")),
            ),
            referenceSourceId = "frankfurter",
            now = now,
        )
        assertEquals("fast", merged.getValue("TRY").sourceId)
    }

    @Test
    fun `эталон сам не проверяется на отклонение`() {
        val merged = RateMerger.merge(
            kinds = mapOf("TRY" to CurrencyKind.FIAT),
            candidates = listOf(
                candidate("frankfurter", 30, rates = arrayOf("TRY" to "48.469")),
            ),
            referenceSourceId = "frankfurter",
            now = now,
        )
        assertEquals("frankfurter", merged.getValue("TRY").sourceId)
    }

    @Test
    fun `у крипты порог отклонения шире`() {
        assertTrue(
            RateMerger.withinTolerance(
                BigDecimal("0.000015"), BigDecimal("0.0000128"), CurrencyKind.CRYPTO,
            ),
        )
        assertFalse(
            RateMerger.withinTolerance(
                BigDecimal("0.000015"), BigDecimal("0.0000128"), CurrencyKind.FIAT,
            ),
        )
    }

    @Test
    fun `нулевые и отрицательные курсы игнорируются`() {
        val merged = RateMerger.merge(
            kinds = mapOf("TRY" to CurrencyKind.FIAT),
            candidates = listOf(
                candidate("broken", 10, rates = arrayOf("TRY" to "0")),
                candidate("good", 50, rates = arrayOf("TRY" to "48.47")),
            ),
            referenceSourceId = null,
            now = now,
        )
        assertEquals("good", merged.getValue("TRY").sourceId)
    }

    @Test
    fun `источник не отдающий нужный тип актива не участвует`() {
        val merged = RateMerger.merge(
            kinds = mapOf("BTC" to CurrencyKind.CRYPTO),
            candidates = listOf(
                candidate("fiatOnly", 10, kind = CurrencyKind.FIAT, rates = arrayOf("BTC" to "0.5")),
            ),
            referenceSourceId = null,
            now = now,
        )
        assertNull(merged["BTC"])
    }

    @Test
    fun `часы источника впереди не считаются устаревшими`() {
        val candidate = candidate(
            "future", 10,
            asOf = now.plus(Duration.ofHours(2)),
            rates = arrayOf("TRY" to "48.47"),
        )
        assertFalse(RateMerger.isStale(candidate, now))
    }
}
