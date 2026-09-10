package com.exchangerates.app.core.util

/**
 * Флаг страны как emoji: ISO-код 3166-1 alpha-2 -> пара regional indicator symbols.
 * Не требует ни ресурсов в APK, ни сети, и корректно масштабируется.
 */
object Flags {
    private const val REGIONAL_INDICATOR_A = 0x1F1E6
    private const val LETTER_A = 'A'.code

    fun emoji(countryCode: String?): String? {
        val cc = countryCode?.trim()?.uppercase() ?: return null
        if (cc.length != 2 || cc.any { it !in 'A'..'Z' }) return null
        val first = REGIONAL_INDICATOR_A + (cc[0].code - LETTER_A)
        val second = REGIONAL_INDICATOR_A + (cc[1].code - LETTER_A)
        return String(Character.toChars(first)) + String(Character.toChars(second))
    }
}
