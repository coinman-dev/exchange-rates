package com.exchangerates.app.presentation.converter

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.exchangerates.app.R
import com.exchangerates.app.core.theme.AppDimens
import com.exchangerates.app.core.theme.AppTheme
import com.exchangerates.app.domain.model.RateMode
import com.exchangerates.app.presentation.converter.components.CurrencyCard
import com.exchangerates.app.presentation.converter.components.MathOperator
import com.exchangerates.app.presentation.converter.components.OperatorBar
import com.exchangerates.app.presentation.navigation.AddCurrencyButton
import com.exchangerates.app.presentation.navigation.FooterStatus
import com.exchangerates.app.presentation.picker.CurrencyPickerSheet
import com.exchangerates.app.presentation.picker.CardActionsSheet
import com.exchangerates.app.presentation.more.SourcesDialog
import com.exchangerates.app.presentation.util.formatUpdatedAt
import com.exchangerates.app.presentation.util.rateModeShortLabel

/** Что открыто поверх экрана. */
private sealed interface Overlay {
    data object None : Overlay
    data object AddCurrency : Overlay
    data class ReplaceCurrency(val code: String) : Overlay
    data class CardActions(val code: String, val isBase: Boolean) : Overlay
    data object Sources : Overlay
}

@Composable
fun ConverterScreen(
    state: ConverterUiState,
    onCardSelected: (String) -> Unit,
    onInputChanged: (TextFieldValue) -> Unit,
    onOperator: (MathOperator) -> Unit,
    onCommit: () -> Unit,
    onClearInput: () -> Unit,
    onDismissEditing: () -> Unit,
    onAddCurrency: (String) -> Unit,
    onReplaceCurrency: (String, String) -> Unit,
    onRemoveCurrency: (String) -> Unit,
    onSetBase: (String) -> Unit,
    onMove: (String, Int) -> Unit,
    onRefresh: () -> Unit,
    onOpenChart: (String) -> Unit,
    onRateModeChange: (RateMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    var overlay by remember { mutableStateOf<Overlay>(Overlay.None) }
    val keyboard = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0

    // клавиатуру закрыли системной кнопкой «назад» — вычисляем введённое
    LaunchedEffect(imeVisible) {
        if (!imeVisible && state.activeCode != null) onDismissEditing()
    }

    Box(modifier = modifier.fillMaxSize().background(colors.background)) {
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = AppDimens.screenPadding,
                    end = AppDimens.screenPadding,
                    top = 4.dp,
                    bottom = 140.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(AppDimens.cardGap),
            ) {
                val baseRow = state.rows.firstOrNull { it.isBase }
                if (baseRow != null) {
                    item(key = "base:${baseRow.currency.code}") {
                        CurrencyCard(
                            data = baseRow,
                            isActive = state.activeCode == baseRow.currency.code,
                            input = state.input,
                            inputPristine = state.inputPristine,
                            inputError = state.inputError,
                            baseLabel = stringResource(R.string.you_convert),
                            noRateLabel = stringResource(R.string.no_rate),
                            onSelect = { onCardSelected(baseRow.currency.code) },
                            onLongPress = {
                                overlay = Overlay.CardActions(baseRow.currency.code, isBase = true)
                            },
                            onPickCurrency = {
                                overlay = Overlay.ReplaceCurrency(baseRow.currency.code)
                            },
                            onInputChange = onInputChanged,
                            onCommit = {
                                onCommit()
                                keyboard?.hide()
                            },
                            onClear = onClearInput,
                        )
                    }
                    item(key = "divider") {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = colors.outline,
                        )
                    }
                }

                items(
                    items = state.rows.filterNot { it.isBase },
                    key = { it.currency.code },
                ) { row ->
                    CurrencyCard(
                        data = row,
                        isActive = state.activeCode == row.currency.code,
                        input = state.input,
                        inputPristine = state.inputPristine,
                        inputError = state.inputError,
                        baseLabel = "",
                        noRateLabel = stringResource(R.string.no_rate),
                        onSelect = { onCardSelected(row.currency.code) },
                        onLongPress = {
                            overlay = Overlay.CardActions(row.currency.code, isBase = false)
                        },
                        onPickCurrency = { overlay = Overlay.ReplaceCurrency(row.currency.code) },
                        onInputChange = onInputChanged,
                        onCommit = {
                            onCommit()
                            keyboard?.hide()
                        },
                        onClear = onClearInput,
                    )
                }

                item(key = "add") {
                    Spacer(Modifier.height(2.dp))
                    AddCurrencyButton(
                        title = stringResource(R.string.add_another_currency),
                        onClick = { overlay = Overlay.AddCurrency },
                    )
                }

                item(key = "footer") {
                    FooterStatus(
                        updatedText = formatUpdatedAt(state),
                        modeText = rateModeShortLabel(state.rateMode),
                        isOffline = state.isOffline,
                        onInfoClick = { overlay = Overlay.Sources },
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = state.activeCode != null,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .background(colors.background.copy(alpha = 0.96f)),
            ) {
                OperatorBar(onOperator = onOperator)
            }
        }
    }

    when (val current = overlay) {
        Overlay.None -> Unit
        Overlay.AddCurrency -> CurrencyPickerSheet(
            currencies = state.catalog,
            russian = state.russian,
            alreadyAdded = state.rows.map { it.currency.code }.toSet(),
            title = stringResource(R.string.add_another_currency),
            onDismiss = { overlay = Overlay.None },
            onSelect = { code ->
                onAddCurrency(code)
                overlay = Overlay.None
            },
        )
        is Overlay.ReplaceCurrency -> CurrencyPickerSheet(
            currencies = state.catalog,
            russian = state.russian,
            alreadyAdded = state.rows.map { it.currency.code }.toSet() - current.code,
            title = stringResource(R.string.replace_currency),
            onDismiss = { overlay = Overlay.None },
            onSelect = { code ->
                onReplaceCurrency(current.code, code)
                overlay = Overlay.None
            },
        )
        is Overlay.CardActions -> CardActionsSheet(
            code = current.code,
            isBase = current.isBase,
            canRemove = state.rows.size > 2,
            onDismiss = { overlay = Overlay.None },
            onMakeBase = {
                onSetBase(current.code)
                overlay = Overlay.None
            },
            onMoveUp = {
                onMove(current.code, -1)
                overlay = Overlay.None
            },
            onMoveDown = {
                onMove(current.code, 1)
                overlay = Overlay.None
            },
            onRemove = {
                onRemoveCurrency(current.code)
                overlay = Overlay.None
            },
            onOpenChart = {
                onOpenChart(current.code)
                overlay = Overlay.None
            },
        )
        Overlay.Sources -> SourcesDialog(
            statuses = state.statuses,
            rateMode = state.rateMode,
            onRateModeChange = {
                onRateModeChange(it)
                overlay = Overlay.None
            },
            onDismiss = { overlay = Overlay.None },
        )
    }

    if (state.rows.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.loading_rates),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textSecondary,
            )
        }
    }
}
