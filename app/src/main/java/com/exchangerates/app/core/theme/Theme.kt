package com.exchangerates.app.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Дополнительные цвета, которых нет в Material 3 ColorScheme. */
@Immutable
data class AppColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val outline: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val positive: Color,
    val negative: Color,
    val warning: Color,
)

private val DarkAppColors = AppColors(
    isDark = true,
    background = Tokens.DarkBackground,
    surface = Tokens.DarkSurface,
    surfaceElevated = Tokens.DarkSurfaceElevated,
    outline = Tokens.DarkOutline,
    textPrimary = Tokens.DarkTextPrimary,
    textSecondary = Tokens.DarkTextSecondary,
    textTertiary = Tokens.DarkTextTertiary,
    accent = Tokens.Primary,
    positive = Tokens.Positive,
    negative = Tokens.Negative,
    warning = Tokens.Warning,
)

private val LightAppColors = AppColors(
    isDark = false,
    background = Tokens.LightBackground,
    surface = Tokens.LightSurface,
    surfaceElevated = Tokens.LightSurfaceElevated,
    outline = Tokens.LightOutline,
    textPrimary = Tokens.LightTextPrimary,
    textSecondary = Tokens.LightTextSecondary,
    textTertiary = Tokens.LightTextTertiary,
    accent = Tokens.Primary,
    positive = Tokens.Positive,
    negative = Tokens.Negative,
    warning = Tokens.Warning,
)

val LocalAppColors: ProvidableCompositionLocal<AppColors> =
    staticCompositionLocalOf { DarkAppColors }

/** Режим темы, выбранный пользователем. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Composable
fun ExchangeRatesTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val appColors = if (dark) DarkAppColors else LightAppColors
    val scheme = if (dark) {
        darkColorScheme(
            primary = appColors.accent,
            onPrimary = Color.White,
            background = appColors.background,
            onBackground = appColors.textPrimary,
            surface = appColors.surface,
            onSurface = appColors.textPrimary,
            surfaceVariant = appColors.surfaceElevated,
            onSurfaceVariant = appColors.textSecondary,
            outline = appColors.outline,
            error = appColors.negative,
        )
    } else {
        lightColorScheme(
            primary = appColors.accent,
            onPrimary = Color.White,
            background = appColors.background,
            onBackground = appColors.textPrimary,
            surface = appColors.surface,
            onSurface = appColors.textPrimary,
            surfaceVariant = appColors.surfaceElevated,
            onSurfaceVariant = appColors.textSecondary,
            outline = appColors.outline,
            error = appColors.negative,
        )
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

/** Быстрый доступ к расширенной палитре: `AppTheme.colors.textSecondary`. */
object AppTheme {
    val colors: AppColors
        @Composable get() = LocalAppColors.current
}
