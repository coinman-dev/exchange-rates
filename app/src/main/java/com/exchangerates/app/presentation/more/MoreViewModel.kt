package com.exchangerates.app.presentation.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exchangerates.app.core.theme.ThemeMode
import com.exchangerates.app.data.local.AppLanguage
import com.exchangerates.app.data.local.AppSettings
import com.exchangerates.app.data.local.SettingsStore
import com.exchangerates.app.data.repository.RatesRepository
import com.exchangerates.app.domain.model.RateMode
import com.exchangerates.app.domain.model.SourceStatus
import com.exchangerates.app.work.RatesSyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MoreUiState(
    val settings: AppSettings = AppSettings(),
    val statuses: List<SourceStatus> = emptyList(),
    val isRefreshing: Boolean = false,
)

@HiltViewModel
class MoreViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
    private val repository: RatesRepository,
    private val scheduler: RatesSyncScheduler,
) : ViewModel() {

    val state: StateFlow<MoreUiState> = combine(
        settingsStore.settings,
        repository.statuses,
        repository.refreshing,
    ) { settings, statuses, refreshing ->
        MoreUiState(settings, statuses, refreshing)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MoreUiState())

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { settingsStore.setThemeMode(mode) }

    fun setLanguage(language: AppLanguage) = viewModelScope.launch {
        settingsStore.setLanguage(language)
    }

    fun setDecimals(value: Int) = viewModelScope.launch { settingsStore.setDecimals(value) }

    fun setGrouping(enabled: Boolean) = viewModelScope.launch { settingsStore.setGrouping(enabled) }

    fun setShowChange(enabled: Boolean) = viewModelScope.launch {
        settingsStore.setShowChangePercent(enabled)
    }

    fun setRateMode(mode: RateMode) = viewModelScope.launch {
        settingsStore.setRateMode(mode)
        repository.refresh(mode, force = true)
    }

    fun setWifiOnly(enabled: Boolean) = viewModelScope.launch {
        settingsStore.setSyncOnlyOnWifi(enabled)
        rescheduleSync()
    }

    fun setInterval(hours: Int) = viewModelScope.launch {
        settingsStore.setSyncIntervalHours(hours)
        rescheduleSync()
    }

    fun refreshNow() = viewModelScope.launch {
        val mode = state.value.settings.rateMode
        repository.refresh(mode, force = true)
    }

    private suspend fun rescheduleSync() {
        val settings = state.value.settings
        scheduler.schedule(settings.syncIntervalHours, settings.syncOnlyOnWifi)
    }
}
