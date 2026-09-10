package com.exchangerates.app.presentation.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.exchangerates.app.R
import com.exchangerates.app.core.format.AmountFormatter
import com.exchangerates.app.core.theme.AppDimens
import com.exchangerates.app.core.theme.AppTheme
import com.exchangerates.app.domain.model.ChartRange
import com.exchangerates.app.presentation.converter.components.CurrencyIcon
import com.exchangerates.app.presentation.picker.CurrencyPickerSheet
import com.exchangerates.app.presentation.util.formatDate
import com.exchangerates.app.presentation.util.formatDateTime
import com.exchangerates.app.presentation.util.rememberAppLocale
import java.math.BigDecimal
import java.time.Instant
import java.util.Locale

private sealed interface PickerTarget {
    data object None : PickerTarget
    data object Base : PickerTarget
    data object Quote : PickerTarget
}

@Composable
fun ChartScreen(
    state: ChartUiState,
    onSetBase: (String) -> Unit,
    onSetQuote: (String) -> Unit,
    onSwap: () -> Unit,
    onRange: (ChartRange) -> Unit,
    onScrub: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val locale = rememberAppLocale()
    val formatter = remember(locale) { AmountFormatter(locale) }
    var picker by remember { mutableStateOf<PickerTarget>(PickerTarget.None) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AppDimens.screenPadding),
    ) {
        Text(
            text = stringResource(R.string.chart_title),
            style = MaterialTheme.typography.headlineSmall,
            color = colors.textPrimary,
            modifier = Modifier.padding(vertical = 12.dp),
        )

        PairSelector(
            baseCode = state.baseCode,
            quoteCode = state.quoteCode,
            onPickBase = { picker = PickerTarget.Base },
            onPickQuote = { picker = PickerTarget.Quote },
            onSwap = onSwap,
        )

        Spacer(Modifier.height(14.dp))

        val displayPoint = state.scrubbed
        val displayRate = displayPoint?.rate ?: state.current
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = displayRate?.let { formatter.formatChartValue(it) } ?: "—",
                style = MaterialTheme.typography.displaySmall,
                color = colors.textPrimary,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = state.quoteCode,
                style = MaterialTheme.typography.titleLarge,
                color = colors.textSecondary,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        Text(
            text = when {
                displayPoint != null -> formatMoment(displayPoint.epochMillis, state.range, locale)
                state.stats != null -> formatter.formatPercent(state.stats.changePercent) +
                    " · " + rangeLabel(state.range)
                else -> ""
            },
            style = MaterialTheme.typography.bodyMedium,
            color = when {
                displayPoint != null -> colors.textSecondary
                (state.stats?.changePercent ?: BigDecimal.ZERO).signum() >= 0 -> colors.positive
                else -> colors.negative
            },
        )

        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            contentAlignment = Alignment.Center,
        ) {
            when {
                state.isLoading && state.points.isEmpty() -> CircularProgressIndicator(
                    color = colors.accent,
                )
                state.points.size < 2 -> Text(
                    text = stringResource(R.string.chart_no_data),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textSecondary,
                )
                else -> RateLineChart(
                    points = state.points,
                    lineColor = colors.accent,
                    fillTop = colors.accent,
                    gridColor = colors.outline,
                    onScrub = onScrub,
                    scrubIndex = state.scrubIndex,
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        RangeSelector(selected = state.range, onSelect = onRange)
        Spacer(Modifier.height(18.dp))

        state.stats?.let { stats ->
            StatsBlock(
                minText = formatter.formatChartValue(stats.min),
                maxText = formatter.formatChartValue(stats.max),
                averageText = formatter.formatChartValue(stats.average),
                changeText = formatter.formatPercent(stats.changePercent),
                changePositive = stats.changePercent.signum() >= 0,
            )
        }
        Spacer(Modifier.height(AppDimens.bottomBarSpace))
    }

    when (picker) {
        PickerTarget.None -> Unit
        PickerTarget.Base -> CurrencyPickerSheet(
            currencies = state.catalog,
            russian = state.russian,
            alreadyAdded = setOf(state.baseCode),
            title = stringResource(R.string.chart_pick_pair),
            onDismiss = { picker = PickerTarget.None },
            onSelect = {
                onSetBase(it)
                picker = PickerTarget.None
            },
        )
        PickerTarget.Quote -> CurrencyPickerSheet(
            currencies = state.catalog,
            russian = state.russian,
            alreadyAdded = setOf(state.quoteCode),
            title = stringResource(R.string.chart_pick_pair),
            onDismiss = { picker = PickerTarget.None },
            onSelect = {
                onSetQuote(it)
                picker = PickerTarget.None
            },
        )
    }
}

@Composable
private fun PairSelector(
    baseCode: String,
    quoteCode: String,
    onPickBase: () -> Unit,
    onPickQuote: () -> Unit,
    onSwap: () -> Unit,
) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CodeChip(code = baseCode, onClick = onPickBase, modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(colors.surfaceElevated, CircleShape)
                .clickable(onClick = onSwap),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.SwapVert,
                contentDescription = stringResource(R.string.chart_swap),
                tint = colors.textPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
        CodeChip(code = quoteCode, onClick = onPickQuote, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun CodeChip(code: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    Row(
        modifier = modifier
            .background(colors.surface, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = code,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = colors.textPrimary,
        )
    }
}

@Composable
private fun RangeSelector(selected: ChartRange, onSelect: (ChartRange) -> Unit) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, CircleShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        ChartRange.entries.forEach { range ->
            val isSelected = range == selected
            Text(
                text = range.id,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                softWrap = false,
                color = if (isSelected) colors.textPrimary else colors.textSecondary,
                modifier = Modifier
                    .background(
                        if (isSelected) colors.surfaceElevated else androidx.compose.ui.graphics.Color.Transparent,
                        CircleShape,
                    )
                    .clickable { onSelect(range) }
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun StatsBlock(
    minText: String,
    maxText: String,
    averageText: String,
    changeText: String,
    changePositive: Boolean,
) {
    val colors = AppTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(AppDimens.cardCorner))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatRow(stringResource(R.string.chart_min), minText, colors.textPrimary)
        StatRow(stringResource(R.string.chart_max), maxText, colors.textPrimary)
        StatRow(stringResource(R.string.chart_avg), averageText, colors.textPrimary)
        StatRow(
            stringResource(R.string.chart_change),
            changeText,
            if (changePositive) colors.positive else colors.negative,
        )
    }
}

@Composable
private fun StatRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color) {
    val colors = AppTheme.colors
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textSecondary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = valueColor,
        )
    }
}

private fun rangeLabel(range: ChartRange): String = range.id

private fun formatMoment(epochMillis: Long, range: ChartRange, locale: Locale): String {
    val instant = Instant.ofEpochMilli(epochMillis)
    return if (range == ChartRange.DAY) {
        formatDateTime(instant, locale)
    } else {
        formatDate(instant, locale)
    }
}
