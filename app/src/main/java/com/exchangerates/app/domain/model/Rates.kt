package com.exchangerates.app.domain.model

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.Instant

/** Один курс относительно опорной валюты (всегда USD). */
data class RateEntry(
    val quote: String,
    val rate: BigDecimal,
    val sourceId: String,
    val asOf: Instant,
)

/**
 * Сетка курсов с опорой USD: `rate` — сколько единиц валюты за 1 USD.
 * Кросс-курс считается делением, поэтому база в интерфейсе может быть любой.
 */
data class RateTable(
    val mode: RateMode,
    val entries: Map<String, RateEntry>,
    /** Когда приложение последний раз успешно обновило сетку. */
    val fetchedAt: Instant?,
    /** Дата самих данных: для курсов ЦБ это день публикации. */
    val dataAsOf: Instant?,
    val isOffline: Boolean,
    /** Сетка за предыдущий день — для изменения за сутки на карточке. */
    val previousDay: Map<String, BigDecimal> = emptyMap(),
) {
    val isEmpty: Boolean get() = entries.isEmpty()

    fun rateToUsd(code: String): BigDecimal? =
        if (code == PIVOT) BigDecimal.ONE else entries[code]?.rate

    /** Курс 1 [from] в [to]. */
    fun cross(from: String, to: String): BigDecimal? {
        if (from == to) return BigDecimal.ONE
        val fromRate = rateToUsd(from) ?: return null
        val toRate = rateToUsd(to) ?: return null
        if (fromRate.signum() == 0) return null
        return toRate.divide(fromRate, MC)
    }

    fun convert(amount: BigDecimal, from: String, to: String): BigDecimal? =
        cross(from, to)?.let { amount.multiply(it, MC) }

    fun sourcesUsed(): Map<String, Int> =
        entries.values.groupingBy { it.sourceId }.eachCount()

    /** Изменение курса [from]→[to] за сутки, в процентах. */
    fun dayChangePercent(from: String, to: String): BigDecimal? {
        if (previousDay.isEmpty()) return null
        val today = cross(from, to) ?: return null
        val prevFrom = if (from == PIVOT) BigDecimal.ONE else previousDay[from] ?: return null
        val prevTo = if (to == PIVOT) BigDecimal.ONE else previousDay[to] ?: return null
        if (prevFrom.signum() == 0) return null
        val yesterday = prevTo.divide(prevFrom, MC)
        if (yesterday.signum() == 0) return null
        return today.subtract(yesterday)
            .divide(yesterday, MC)
            .multiply(BigDecimal(100), MC)
    }

    companion object {
        const val PIVOT = "USD"
        val MC: MathContext = MathContext(24, RoundingMode.HALF_EVEN)

        fun empty(mode: RateMode) = RateTable(
            mode = mode,
            entries = emptyMap(),
            fetchedAt = null,
            dataAsOf = null,
            isOffline = true,
        )
    }
}

/** Точка исторического ряда для графика. */
data class HistoryPoint(val epochMillis: Long, val rate: BigDecimal)

/** Диапазон графика. */
enum class ChartRange(val id: String, val days: Int) {
    DAY("1D", 1),
    WEEK("1W", 7),
    MONTH("1M", 30),
    YEAR("1Y", 365),
    FIVE_YEARS("5Y", 365 * 5),
    ;

    /** Прореживание длинных рядов на стороне Frankfurter. */
    val group: String?
        get() = when (this) {
            YEAR -> "week"
            FIVE_YEARS -> "month"
            else -> null
        }

    companion object {
        fun fromId(id: String?): ChartRange = entries.firstOrNull { it.id == id } ?: MONTH
    }
}
