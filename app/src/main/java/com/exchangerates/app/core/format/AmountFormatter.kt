package com.exchangerates.app.core.format

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Форматирование сумм и курсов.
 *
 * Отличие от Xe: адаптивная точность. Xe показывает биткойн как `0.00`,
 * здесь мелкие значения выводятся значащими цифрами (`0.00002064`).
 */
class AmountFormatter(locale: Locale = Locale.getDefault()) {

    private val symbols: DecimalFormatSymbols = DecimalFormatSymbols.getInstance(locale)

    val decimalSeparator: Char get() = symbols.decimalSeparator
    val groupingSeparator: Char get() = symbols.groupingSeparator

    /**
     * Сумма в карточке валюты.
     *
     * @param decimals «штатное» число знаков валюты (2 для фиата, 8 для крипты)
     * @param grouped разделять ли разряды
     */
    fun formatAmount(value: BigDecimal, decimals: Int, grouped: Boolean = true): String {
        if (value.signum() == 0) return pattern(decimals, grouped).format(BigDecimal.ZERO)
        val abs = value.abs()
        val rounded = value.setScale(decimals, RoundingMode.HALF_EVEN)
        // значение не должно превращаться в 0.00 — показываем значащие цифры
        if (rounded.signum() == 0) {
            return formatSignificant(value, SIGNIFICANT_DIGITS_SMALL, grouped)
        }
        // очень большие значения крипты/слабых валют не нужны с 8 знаками
        val effective = if (abs >= BigDecimal.ONE && decimals > 4) 4 else decimals
        val text = pattern(effective, grouped).format(value)
        return if (effective > 2) text.trimTrailingZeros() else text
    }

    /**
     * Удельный курс под суммой: `1 TRY → 0.0206 USD`.
     * Xe всегда печатает 4 знака; для микро-курсов переходим на значащие цифры.
     */
    fun formatUnitRate(rate: BigDecimal): String {
        if (rate.signum() == 0) return pattern(RATE_DECIMALS, false).format(BigDecimal.ZERO)
        val abs = rate.abs()
        return when {
            abs < MIN_RATE_FOR_FIXED -> formatSignificant(rate, SIGNIFICANT_DIGITS_RATE, false)
            abs >= BigDecimal(1000) -> pattern(2, true).format(rate)
            else -> pattern(RATE_DECIMALS, true).format(rate)
        }
    }

    /** Значение для графика и статистики (мин/макс/среднее). */
    fun formatChartValue(rate: BigDecimal): String = formatUnitRate(rate)

    /** Изменение в процентах: `+1.24 %`. */
    fun formatPercent(change: BigDecimal): String {
        val df = pattern(2, false)
        val sign = if (change.signum() > 0) "+" else ""
        return sign + df.format(change) + " %"
    }

    /** Текст, который подставляется в поле ввода при фокусе. */
    fun formatForEditing(value: BigDecimal, decimals: Int): String {
        if (value.signum() == 0) return ""
        val effective = if (value.abs() >= BigDecimal.ONE && decimals > 4) 4 else decimals
        val rounded = value.setScale(effective, RoundingMode.HALF_EVEN)
        val plain = (if (rounded.signum() == 0) value.round(MathContext(SIGNIFICANT_DIGITS_SMALL)) else rounded)
            .stripTrailingZeros()
            .toPlainString()
        return plain.replace('.', decimalSeparator)
    }

    private fun formatSignificant(value: BigDecimal, digits: Int, grouped: Boolean): String {
        val rounded = value.round(MathContext(digits, RoundingMode.HALF_EVEN)).stripTrailingZeros()
        val scale = rounded.scale().coerceIn(0, MAX_SCALE)
        return pattern(scale, grouped).format(rounded).trimTrailingZeros()
    }

    private fun pattern(decimals: Int, grouped: Boolean): DecimalFormat {
        val safeDecimals = decimals.coerceIn(0, MAX_SCALE)
        val fraction = if (safeDecimals > 0) "." + "0".repeat(safeDecimals) else ""
        val integer = if (grouped) "#,##0" else "0"
        return DecimalFormat(integer + fraction, symbols).apply {
            roundingMode = RoundingMode.HALF_EVEN
            isGroupingUsed = grouped
        }
    }

    private fun String.trimTrailingZeros(): String {
        if (!contains(decimalSeparator)) return this
        return trimEnd('0').trimEnd(decimalSeparator)
    }

    private companion object {
        const val RATE_DECIMALS = 4
        const val SIGNIFICANT_DIGITS_SMALL = 6
        const val SIGNIFICANT_DIGITS_RATE = 4
        const val MAX_SCALE = 12
        val MIN_RATE_FOR_FIXED: BigDecimal = BigDecimal("0.0001")
    }
}
