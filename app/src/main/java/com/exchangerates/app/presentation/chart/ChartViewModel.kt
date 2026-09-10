package com.exchangerates.app.presentation.chart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exchangerates.app.data.catalog.CurrencyCatalog
import com.exchangerates.app.data.repository.HistoryRepository
import com.exchangerates.app.data.repository.RatesRepository
import com.exchangerates.app.domain.model.ChartRange
import com.exchangerates.app.domain.model.Currency
import com.exchangerates.app.domain.model.HistoryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ChartStats(
    val min: BigDecimal,
    val max: BigDecimal,
    val average: BigDecimal,
    val changeAbsolute: BigDecimal,
    val changePercent: BigDecimal,
)

data class ChartUiState(
    val baseCode: String = "USD",
    val quoteCode: String = "RUB",
    val range: ChartRange = ChartRange.MONTH,
    val points: List<HistoryPoint> = emptyList(),
    val stats: ChartStats? = null,
    val isLoading: Boolean = false,
    val scrubIndex: Int? = null,
    val catalog: List<Currency> = emptyList(),
    val russian: Boolean = true,
) {
    val current: BigDecimal? get() = points.lastOrNull()?.rate
    val scrubbed: HistoryPoint? get() = scrubIndex?.let { points.getOrNull(it) }
}

@HiltViewModel
class ChartViewModel @Inject constructor(
    private val history: HistoryRepository,
    private val rates: RatesRepository,
    private val catalog: CurrencyCatalog,
) : ViewModel() {

    private val _state = MutableStateFlow(ChartUiState())
    val state: StateFlow<ChartUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            val currencies = catalog.all()
            val user = rates.observeUserCurrencies().first()
            _state.value = _state.value.copy(
                catalog = currencies,
                baseCode = user.base,
                quoteCode = user.others.firstOrNull()
                    ?: currencies.firstOrNull { it.code != user.base }?.code
                    ?: "RUB",
            )
            reload()
        }
    }

    fun setPair(base: String, quote: String) {
        if (base == quote) return
        _state.value = _state.value.copy(baseCode = base, quoteCode = quote, scrubIndex = null)
        reload()
    }

    fun setQuote(code: String) = setPair(_state.value.baseCode, code)

    fun setBase(code: String) = setPair(code, _state.value.quoteCode)

    fun swap() {
        val current = _state.value
        setPair(current.quoteCode, current.baseCode)
    }

    fun setRange(range: ChartRange) {
        if (range == _state.value.range) return
        _state.value = _state.value.copy(range = range, scrubIndex = null)
        reload()
    }

    fun onScrub(index: Int?) {
        _state.value = _state.value.copy(scrubIndex = index)
    }

    private fun reload() {
        val snapshot = _state.value
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val points = history.series(snapshot.baseCode, snapshot.quoteCode, snapshot.range)
            _state.value = _state.value.copy(
                points = points,
                stats = statsOf(points),
                isLoading = false,
            )
        }
    }

    private fun statsOf(points: List<HistoryPoint>): ChartStats? {
        if (points.isEmpty()) return null
        val values = points.map { it.rate }
        val min = values.min()
        val max = values.max()
        val sum = values.fold(BigDecimal.ZERO) { acc, value -> acc.add(value) }
        val average = sum.divide(BigDecimal(values.size), MC)
        val first = values.first()
        val last = values.last()
        val absolute = last.subtract(first)
        val percent = if (first.signum() == 0) {
            BigDecimal.ZERO
        } else {
            absolute.divide(first, MC).multiply(BigDecimal(100), MC)
        }
        return ChartStats(min, max, average, absolute, percent)
    }

    private companion object {
        val MC: MathContext = MathContext(20, RoundingMode.HALF_EVEN)
    }
}
