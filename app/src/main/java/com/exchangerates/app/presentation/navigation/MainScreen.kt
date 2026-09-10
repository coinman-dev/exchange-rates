package com.exchangerates.app.presentation.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.exchangerates.app.R
import com.exchangerates.app.core.theme.AppTheme
import com.exchangerates.app.presentation.chart.ChartScreen
import com.exchangerates.app.presentation.chart.ChartViewModel
import com.exchangerates.app.presentation.converter.ConverterScreen
import com.exchangerates.app.presentation.converter.ConverterViewModel
import com.exchangerates.app.presentation.more.MoreScreen
import com.exchangerates.app.presentation.more.MoreViewModel
import com.exchangerates.app.presentation.rates.RatesOverviewScreen

/** Режим вкладки «Главная»: обзор курсов или конвертер. */
private enum class HomeMode { RATES, CONVERTER }

@Composable
fun MainScreen() {
    val colors = AppTheme.colors
    var tab by remember { mutableStateOf(AppTab.HOME) }
    var homeMode by remember { mutableStateOf(HomeMode.CONVERTER) }

    val converterViewModel: ConverterViewModel = hiltViewModel()
    val chartViewModel: ChartViewModel = hiltViewModel()
    val moreViewModel: MoreViewModel = hiltViewModel()

    val converterState by converterViewModel.state.collectAsStateWithLifecycle()
    val chartState by chartViewModel.state.collectAsStateWithLifecycle()
    val moreState by moreViewModel.state.collectAsStateWithLifecycle()

    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0

    val tabLabels = mapOf(
        AppTab.HOME to stringResource(R.string.tab_home),
        AppTab.CHART to stringResource(R.string.tab_chart),
        AppTab.MORE to stringResource(R.string.tab_more),
    )

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            if (tab == AppTab.HOME) {
                TopSegmentedBar(
                    convertTitle = stringResource(R.string.convert_title),
                    ratesContentDescription = stringResource(R.string.rates_title),
                    isConverterSelected = homeMode == HomeMode.CONVERTER,
                    onSelectRates = { homeMode = HomeMode.RATES },
                    onSelectConverter = { homeMode = HomeMode.CONVERTER },
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (tab) {
                    AppTab.HOME -> when (homeMode) {
                        HomeMode.CONVERTER -> ConverterScreen(
                            state = converterState,
                            onCardSelected = converterViewModel::onCardSelected,
                            onInputChanged = converterViewModel::onInputChanged,
                            onOperator = converterViewModel::onOperator,
                            onCommit = converterViewModel::onCommit,
                            onClearInput = converterViewModel::onClearInput,
                            onDismissEditing = converterViewModel::onDismissEditing,
                            onAddCurrency = { converterViewModel.addCurrency(it) },
                            onReplaceCurrency = { old, new ->
                                converterViewModel.replaceCurrency(old, new)
                            },
                            onRemoveCurrency = { converterViewModel.removeCurrency(it) },
                            onSetBase = { converterViewModel.setBase(it) },
                            onMove = { code, delta -> moveCurrency(converterViewModel, converterState, code, delta) },
                            onRefresh = { converterViewModel.refresh() },
                            onOpenChart = { code ->
                                chartViewModel.setPair(converterState.baseCode ?: "USD", code)
                                tab = AppTab.CHART
                            },
                            onRateModeChange = { converterViewModel.setRateMode(it) },
                        )
                        HomeMode.RATES -> RatesOverviewScreen(
                            state = converterState,
                            onRefresh = { converterViewModel.refresh() },
                            onOpenChart = { code ->
                                chartViewModel.setPair(converterState.baseCode ?: "USD", code)
                                tab = AppTab.CHART
                            },
                            onInfoClick = { homeMode = HomeMode.CONVERTER },
                        )
                    }
                    AppTab.CHART -> ChartScreen(
                        state = chartState,
                        onSetBase = chartViewModel::setBase,
                        onSetQuote = chartViewModel::setQuote,
                        onSwap = chartViewModel::swap,
                        onRange = chartViewModel::setRange,
                        onScrub = chartViewModel::onScrub,
                    )
                    AppTab.MORE -> MoreScreen(
                        state = moreState,
                        onTheme = { moreViewModel.setTheme(it) },
                        onLanguage = { moreViewModel.setLanguage(it) },
                        onDecimals = { moreViewModel.setDecimals(it) },
                        onGrouping = { moreViewModel.setGrouping(it) },
                        onShowChange = { moreViewModel.setShowChange(it) },
                        onRateMode = { moreViewModel.setRateMode(it) },
                        onWifiOnly = { moreViewModel.setWifiOnly(it) },
                        onInterval = { moreViewModel.setInterval(it) },
                        onRefreshNow = { moreViewModel.refreshNow() },
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = !imeVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            FloatingBottomBar(
                current = tab,
                labels = tabLabels,
                onSelect = { tab = it },
                modifier = Modifier.navigationBarsPadding(),
            )
        }
    }
}

/** Перемещение карточки вверх или вниз в списке пользователя. */
private fun moveCurrency(
    viewModel: ConverterViewModel,
    state: com.exchangerates.app.presentation.converter.ConverterUiState,
    code: String,
    delta: Int,
) {
    val base = state.baseCode ?: return
    val others = state.rows.filterNot { it.isBase }.map { it.currency.code }.toMutableList()
    val index = others.indexOf(code)
    if (index < 0) return
    val target = (index + delta).coerceIn(0, others.lastIndex)
    if (target == index) return
    others.removeAt(index)
    others.add(target, code)
    viewModel.reorder(base, others)
}
