package com.exchangerates.app.core.theme

import androidx.compose.ui.graphics.Color

/**
 * Токены цвета сняты со скриншотов нового дизайна Xe (тёмная AMOLED-тема)
 * и дополнены светлым вариантом.
 */
internal object Tokens {
    // --- общие ---
    val Primary = Color(0xFF3B5BFF)
    val PrimaryPressed = Color(0xFF2C46D6)
    val Positive = Color(0xFF30D158)
    val Negative = Color(0xFFFF453A)
    val Warning = Color(0xFFFF9F0A)

    // --- тёмная тема ---
    val DarkBackground = Color(0xFF000000)
    val DarkSurface = Color(0xFF1A1A1C)
    val DarkSurfaceElevated = Color(0xFF2A2A2D)
    val DarkOutline = Color(0xFF2C2C2E)
    val DarkTextPrimary = Color(0xFFFFFFFF)
    val DarkTextSecondary = Color(0xFF8E8E93)
    val DarkTextTertiary = Color(0xFF5A5A5F)

    // --- светлая тема ---
    val LightBackground = Color(0xFFF2F2F7)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceElevated = Color(0xFFE9E9EE)
    val LightOutline = Color(0xFFD8D8DE)
    val LightTextPrimary = Color(0xFF0A0A0C)
    val LightTextSecondary = Color(0xFF6E6E73)
    val LightTextTertiary = Color(0xFF9E9EA4)
}
