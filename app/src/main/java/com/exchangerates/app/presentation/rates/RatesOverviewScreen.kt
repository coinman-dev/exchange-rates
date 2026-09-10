package com.exchangerates.app.presentation.rates

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.exchangerates.app.R
import com.exchangerates.app.core.theme.AppDimens
import com.exchangerates.app.core.theme.AppTheme
import com.exchangerates.app.presentation.converter.ConverterUiState
import com.exchangerates.app.presentation.converter.components.CurrencyIcon
import com.exchangerates.app.presentation.navigation.FooterStatus
import com.exchangerates.app.presentation.util.formatUpdatedAt
import com.exchangerates.app.presentation.util.rateModeShortLabel

/**
 * Обзор курсов: тот же список валют, но без ввода — только курс к базовой
 * валюте и изменение за сутки. Открывается левой кнопкой верхней пилюли.
 */
@Composable
fun RatesOverviewScreen(
    state: ConverterUiState,
    onRefresh: () -> Unit,
    onOpenChart: (String) -> Unit,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val baseCode = state.baseCode ?: ""

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize().background(colors.background),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = AppDimens.screenPadding,
                end = AppDimens.screenPadding,
                bottom = 140.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.rates_overview_hint, baseCode),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(vertical = 10.dp),
                )
            }
            items(state.rows.filterNot { it.isBase }, key = { it.currency.code }) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surface, RoundedCornerShape(AppDimens.cardCorner))
                        .clickable { onOpenChart(row.currency.code) }
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CurrencyIcon(row.currency)
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = row.currency.code,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = colors.textPrimary,
                        )
                        Text(
                            text = row.currency.displayName(state.russian),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                            maxLines = 1,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = row.unitRateText.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textPrimary,
                        )
                        if (row.changePercentText != null) {
                            Text(
                                text = row.changePercentText,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (row.changePositive) colors.positive else colors.negative,
                            )
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(4.dp))
                FooterStatus(
                    updatedText = formatUpdatedAt(state),
                    modeText = rateModeShortLabel(state.rateMode),
                    isOffline = state.isOffline,
                    onInfoClick = onInfoClick,
                )
            }
        }
    }
}
