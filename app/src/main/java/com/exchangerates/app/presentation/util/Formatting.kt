package com.exchangerates.app.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.exchangerates.app.R
import com.exchangerates.app.domain.model.RateMode
import com.exchangerates.app.presentation.converter.ConverterUiState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Язык приложения из конфигурации, за которой Compose следит. Locale.getDefault()
 * не годится: система возвращает ей язык телефона при каждой смене конфигурации
 * (поворот, тёмный режим), даже когда в приложении выбран другой язык.
 */
@Composable
fun rememberAppLocale(): Locale = LocalConfiguration.current.locales[0]

/** «Обновлено: 10 сент., 13:38» или «Курс ЦБ РФ на 10.09.2026». */
@Composable
fun formatUpdatedAt(state: ConverterUiState): String {
    val locale = rememberAppLocale()
    return if (state.rateMode.isCentralBank) {
        val date = state.dataAsOf?.let { formatDate(it, locale) }
        if (date == null) {
            stringResource(R.string.updated_never)
        } else {
            stringResource(R.string.cb_rate_on_date, date)
        }
    } else {
        val moment = state.fetchedAt?.let { formatDateTime(it, locale) }
        when {
            moment == null -> stringResource(R.string.updated_never)
            state.isOffline -> stringResource(R.string.updated_offline, moment)
            else -> stringResource(R.string.updated_at, moment)
        }
    }
}

@Composable
fun rateModeShortLabel(mode: RateMode): String = stringResource(
    when (mode) {
        RateMode.MID_MARKET -> R.string.rate_mode_mid_short
        RateMode.CBR -> R.string.rate_mode_cbr_short
        RateMode.TCMB -> R.string.rate_mode_tcmb_short
        RateMode.NBK -> R.string.rate_mode_nbk_short
    },
)

@Composable
fun rateModeTitle(mode: RateMode): String = stringResource(
    when (mode) {
        RateMode.MID_MARKET -> R.string.rate_mode_mid
        RateMode.CBR -> R.string.rate_mode_cbr
        RateMode.TCMB -> R.string.rate_mode_tcmb
        RateMode.NBK -> R.string.rate_mode_nbk
    },
)

/**
 * Компактная дата со временем: «10 сент. 2026, 15:53».
 * Локализованный формат добавляет «г.» и лишние запятые, из-за чего строка
 * не помещалась в футер на узком экране.
 */
fun formatDateTime(instant: Instant, locale: Locale): String =
    DateTimeFormatter.ofPattern(if (locale.language == "ru") "d MMM yyyy, HH:mm" else "MMM d, yyyy, HH:mm")
        .withLocale(locale)
        .format(instant.atZone(ZoneId.systemDefault()))

fun formatDate(instant: Instant, locale: Locale): String =
    DateTimeFormatter.ofPattern(if (locale.language == "ru") "d MMM yyyy" else "MMM d, yyyy")
        .withLocale(locale)
        .format(instant.atZone(ZoneId.systemDefault()))

fun formatTime(instant: Instant, locale: Locale): String =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        .withLocale(locale)
        .format(instant.atZone(ZoneId.systemDefault()))
