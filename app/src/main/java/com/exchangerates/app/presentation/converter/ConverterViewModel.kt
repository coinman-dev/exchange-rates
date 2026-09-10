package com.exchangerates.app.presentation.converter

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exchangerates.app.core.format.AmountFormatter
import com.exchangerates.app.core.math.ExpressionEvaluator
import com.exchangerates.app.data.catalog.CurrencyCatalog
import com.exchangerates.app.data.local.AppLanguage
import com.exchangerates.app.data.local.AppSettings
import com.exchangerates.app.data.local.SettingsStore
import com.exchangerates.app.data.repository.RatesRepository
import com.exchangerates.app.data.repository.UserCurrencies
import com.exchangerates.app.domain.model.Currency
import com.exchangerates.app.domain.model.RateMode
import com.exchangerates.app.domain.model.RateTable
import com.exchangerates.app.domain.model.SourceStatus
import com.exchangerates.app.presentation.converter.components.CurrencyCardData
import com.exchangerates.app.presentation.converter.components.MathOperator
import com.exchangerates.app.presentation.converter.components.textFieldValue
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.time.Instant
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Что и в какой валюте ввёл пользователь. Остальные карточки считаются от этого. */
private data class Anchor(val code: String, val amount: BigDecimal)

private data class Editing(
    val activeCode: String? = null,
    val input: TextFieldValue = TextFieldValue(),
    val pristine: Boolean = true,
    val error: Boolean = false,
)

data class ConverterUiState(
    val rows: List<CurrencyCardData> = emptyList(),
    val activeCode: String? = null,
    val input: TextFieldValue = TextFieldValue(),
    val inputPristine: Boolean = true,
    val inputError: Boolean = false,
    val rateMode: RateMode = RateMode.MID_MARKET,
    val fetchedAt: Instant? = null,
    val dataAsOf: Instant? = null,
    val isOffline: Boolean = false,
    val isRefreshing: Boolean = false,
    val statuses: List<SourceStatus> = emptyList(),
    val catalog: List<Currency> = emptyList(),
    val russian: Boolean = true,
) {
    val baseCode: String? get() = rows.firstOrNull { it.isBase }?.currency?.code
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ConverterViewModel @Inject constructor(
    private val repository: RatesRepository,
    private val catalog: CurrencyCatalog,
    private val settings: SettingsStore,
) : ViewModel() {

    private val anchor = MutableStateFlow(Anchor(RatesRepository.DEFAULT_BASE, DEFAULT_AMOUNT))

    /** Последние применённые настройки: нужны для локали при подстановке в поле. */
    @Volatile
    private var currentSettings: AppSettings = AppSettings()
    private val editing = MutableStateFlow(Editing())
    private val catalogFlow = MutableStateFlow<List<Currency>>(emptyList())

    private val ratesFlow = settings.settings
        .map { it.rateMode }
        .distinctUntilChanged()
        .flatMapLatest { repository.observeTable(it) }

    val state: StateFlow<ConverterUiState> = combine(
        settings.settings,
        repository.observeUserCurrencies(),
        ratesFlow,
        anchor,
        combine(editing, catalogFlow) { edit, list -> edit to list },
    ) { appSettings, userCurrencies, table, currentAnchor, (edit, currencies) ->
        build(appSettings, userCurrencies, table, currentAnchor, edit, currencies)
    }.combine(repository.refreshing) { uiState, refreshing ->
        uiState.copy(isRefreshing = refreshing)
    }.combine(repository.statuses) { uiState, statuses ->
        uiState.copy(statuses = statuses)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConverterUiState())

    init {
        viewModelScope.launch {
            catalogFlow.value = catalog.all()
            repository.seedUserCurrenciesIfEmpty()
            repository.seedIfEmpty()
            val mode = settings.settings.first().rateMode
            repository.refresh(mode)
        }
    }

    // ---------- построение состояния ----------

    private suspend fun build(
        appSettings: AppSettings,
        userCurrencies: UserCurrencies,
        table: RateTable,
        currentAnchor: Anchor,
        edit: Editing,
        currencies: List<Currency>,
    ): ConverterUiState {
        val byCode = if (currencies.isEmpty()) catalog.byCode() else currencies.associateBy { it.code }
        currentSettings = appSettings
        val russian = isRussian(appSettings.language)
        val formatter = AmountFormatter(localeOf(appSettings.language))

        val codes = userCurrencies.all.filter { byCode.containsKey(it) }
        val baseCode = userCurrencies.base
        val anchorCode = if (currentAnchor.code in codes) currentAnchor.code else baseCode

        val rows = codes.map { code ->
            val currency = byCode.getValue(code)
            val amount = table.convert(currentAnchor.amount, anchorCode, code)
            val unitRate = if (code == baseCode) null else table.cross(baseCode, code)
            val change = if (appSettings.showChangePercent && code != baseCode) {
                table.dayChangePercent(baseCode, code)
            } else {
                null
            }
            val decimals = decimalsFor(currency, appSettings.decimals)
            CurrencyCardData(
                currency = currency,
                isBase = code == baseCode,
                amount = amount,
                amountText = amount?.let { formatter.formatAmount(it, decimals, appSettings.grouping) }
                    ?: "",
                unitRateText = unitRate?.let {
                    "1 $baseCode → ${formatter.formatUnitRate(it)} $code"
                },
                changePercentText = change
                    ?.takeIf { it.abs() >= MIN_VISIBLE_CHANGE }
                    ?.let { formatter.formatPercent(it) },
                changePositive = (change ?: BigDecimal.ZERO).signum() >= 0,
                hasRate = amount != null,
            )
        }

        return ConverterUiState(
            rows = rows,
            activeCode = edit.activeCode,
            input = edit.input,
            inputPristine = edit.pristine,
            inputError = edit.error,
            rateMode = table.mode,
            fetchedAt = table.fetchedAt,
            dataAsOf = table.dataAsOf,
            isOffline = table.isOffline,
            statuses = emptyList(),
            catalog = currencies,
            russian = russian,
        )
    }

    private fun decimalsFor(currency: Currency, settingsDecimals: Int): Int = when {
        currency.kind == com.exchangerates.app.domain.model.CurrencyKind.CRYPTO -> currency.decimals
        currency.kind == com.exchangerates.app.domain.model.CurrencyKind.METAL -> currency.decimals
        else -> settingsDecimals
    }

    private fun isRussian(language: AppLanguage): Boolean = when (language) {
        AppLanguage.RUSSIAN -> true
        AppLanguage.ENGLISH -> false
        AppLanguage.SYSTEM -> Locale.getDefault().language == "ru"
    }

    private fun localeOf(language: AppLanguage): Locale = when (language) {
        AppLanguage.RUSSIAN -> Locale.forLanguageTag("ru")
        AppLanguage.ENGLISH -> Locale.forLanguageTag("en")
        AppLanguage.SYSTEM -> Locale.getDefault()
    }

    // ---------- действия пользователя ----------

    fun onCardSelected(code: String) {
        val row = state.value.rows.firstOrNull { it.currency.code == code } ?: return
        // в поле подставляется значение без разделителей разрядов, иначе
        // «8 433,00 + 50» невозможно разобрать как выражение
        val editable = row.amount?.let {
            formatter().formatForEditing(it, decimalsFor(row.currency, currentSettings.decimals))
        }.orEmpty()
        editing.value = Editing(
            activeCode = code,
            input = textFieldValue(editable),
            pristine = true,
            error = false,
        )
    }

    fun onInputChanged(value: TextFieldValue) {
        val current = editing.value
        val activeCode = current.activeCode ?: return
        editing.value = current.copy(input = value, pristine = false, error = false)
        recalculate(activeCode, value.text)
    }

    fun onOperator(operator: MathOperator) {
        val current = editing.value
        val activeCode = current.activeCode ?: return
        if (operator == MathOperator.EQUALS) {
            commit(activeCode, current.input.text, keepActive = true)
            return
        }
        val text = current.input.text
        val cursor = current.input.selection.end.coerceIn(0, text.length)
        val next = text.substring(0, cursor) + operator.insert + text.substring(cursor)
        val value = TextFieldValue(
            text = next,
            selection = androidx.compose.ui.text.TextRange(cursor + operator.insert.length),
        )
        editing.value = current.copy(input = value, pristine = false, error = false)
        recalculate(activeCode, next)
    }

    fun onCommit() {
        val current = editing.value
        val activeCode = current.activeCode ?: return
        commit(activeCode, current.input.text, keepActive = false)
    }

    fun onClearInput() {
        val current = editing.value
        val activeCode = current.activeCode ?: return
        editing.value = current.copy(input = TextFieldValue(""), pristine = false, error = false)
        anchor.value = Anchor(activeCode, BigDecimal.ZERO)
    }

    fun onDismissEditing() {
        val current = editing.value
        val activeCode = current.activeCode
        if (activeCode != null && !current.pristine) {
            commit(activeCode, current.input.text, keepActive = false)
        } else {
            editing.value = Editing()
        }
    }

    private fun commit(code: String, text: String, keepActive: Boolean) {
        val result = ExpressionEvaluator.evaluate(text)
        when (result) {
            is ExpressionEvaluator.Result.Success -> {
                anchor.value = Anchor(code, result.value)
                val formatted = formattedForEditing(code, result.value)
                editing.value = Editing(
                    activeCode = if (keepActive) code else null,
                    input = if (keepActive) textFieldValue(formatted) else TextFieldValue(),
                    pristine = false,
                    error = false,
                )
            }
            ExpressionEvaluator.Result.Empty -> {
                anchor.value = Anchor(code, BigDecimal.ZERO)
                editing.value = if (keepActive) {
                    Editing(code, TextFieldValue(""), pristine = false, error = false)
                } else {
                    Editing()
                }
            }
            is ExpressionEvaluator.Result.Error -> {
                editing.value = editing.value.copy(error = true)
            }
        }
    }

    private fun recalculate(code: String, text: String) {
        when (val result = ExpressionEvaluator.evaluatePartial(text)) {
            is ExpressionEvaluator.Result.Success -> anchor.value = Anchor(code, result.value)
            ExpressionEvaluator.Result.Empty -> anchor.value = Anchor(code, BigDecimal.ZERO)
            is ExpressionEvaluator.Result.Error ->
                editing.value = editing.value.copy(error = true)
        }
    }

    private fun formattedForEditing(code: String, value: BigDecimal): String {
        val currency = state.value.catalog.firstOrNull { it.code == code }
        val decimals = currency?.let { decimalsFor(it, currentSettings.decimals) } ?: 2
        return formatter().formatForEditing(value, decimals)
    }

    private fun formatter() = AmountFormatter(localeOf(currentSettings.language))

    // ---------- управление списком валют ----------

    fun addCurrency(code: String) = viewModelScope.launch { repository.addCurrency(code) }

    fun removeCurrency(code: String) = viewModelScope.launch {
        if (editing.value.activeCode == code) editing.value = Editing()
        repository.removeCurrency(code)
    }

    fun replaceCurrency(oldCode: String, newCode: String) = viewModelScope.launch {
        if (editing.value.activeCode == oldCode) editing.value = Editing()
        repository.replaceCurrency(oldCode, newCode)
        if (anchor.value.code == oldCode) {
            anchor.value = Anchor(newCode, anchor.value.amount)
        }
    }

    fun setBase(code: String) = viewModelScope.launch { repository.setBase(code) }

    fun reorder(base: String, others: List<String>) = viewModelScope.launch {
        repository.reorder(base, others)
    }

    fun refresh() = viewModelScope.launch {
        repository.refresh(state.value.rateMode, force = true)
    }

    fun setRateMode(mode: RateMode) = viewModelScope.launch {
        settings.setRateMode(mode)
        repository.refresh(mode, force = true)
    }

    private companion object {
        val DEFAULT_AMOUNT: BigDecimal = BigDecimal(100)
        val MIN_VISIBLE_CHANGE: BigDecimal = BigDecimal("0.01")
    }
}
