package com.exchangerates.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.exchangerates.app.core.theme.ThemeMode
import com.exchangerates.app.domain.model.RateMode
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Язык интерфейса. SYSTEM — как в системе. */
enum class AppLanguage(val tag: String) {
    SYSTEM("system"), RUSSIAN("ru"), ENGLISH("en");

    companion object {
        fun fromTag(tag: String?): AppLanguage = entries.firstOrNull { it.tag == tag } ?: SYSTEM
    }
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val rateMode: RateMode = RateMode.MID_MARKET,
    val decimals: Int = 2,
    val grouping: Boolean = true,
    val syncOnlyOnWifi: Boolean = false,
    val syncIntervalHours: Int = 6,
    val showChangePercent: Boolean = true,
) {
    companion object {
        /** Наибольшее число знаков после запятой, которое можно выбрать в настройках. */
        const val MAX_DECIMALS = 6
    }
}

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsStore @Inject constructor(private val context: Context) {

    val settings: Flow<AppSettings> = context.dataStore.data.map { it.toSettings() }

    suspend fun setThemeMode(mode: ThemeMode) = put(Keys.THEME, mode.name)

    suspend fun setLanguage(language: AppLanguage) {
        // сначала дубль: Activity пересоздаётся, как только DataStore сообщит о смене
        rememberLanguageForStartup(language)
        put(Keys.LANGUAGE, language.tag)
    }

    /**
     * Дубль в SharedPreferences: язык нужен синхронно в Activity.attachBaseContext,
     * до того как DataStore успеет отдать значение.
     */
    fun rememberLanguageForStartup(language: AppLanguage) {
        context.getSharedPreferences(SYNC_PREFS, Context.MODE_PRIVATE)
            .edit().putString(Keys.LANGUAGE.name, language.tag).apply()
    }

    suspend fun setRateMode(mode: RateMode) = put(Keys.RATE_MODE, mode.id)

    suspend fun setDecimals(value: Int) {
        context.dataStore.edit { it[Keys.DECIMALS] = value.coerceIn(0, AppSettings.MAX_DECIMALS) }
    }

    suspend fun setGrouping(enabled: Boolean) {
        context.dataStore.edit { it[Keys.GROUPING] = enabled }
    }

    suspend fun setSyncOnlyOnWifi(enabled: Boolean) {
        context.dataStore.edit { it[Keys.WIFI_ONLY] = enabled }
    }

    suspend fun setSyncIntervalHours(hours: Int) {
        context.dataStore.edit { it[Keys.SYNC_INTERVAL] = hours.coerceIn(1, 48) }
    }

    suspend fun setShowChangePercent(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_CHANGE] = enabled }
    }

    private suspend fun put(key: Preferences.Key<String>, value: String) {
        context.dataStore.edit { it[key] = value }
    }

    /** Синхронное чтение языка для подмены локали при создании Activity. */
    fun languageBlocking(): AppLanguage = AppLanguage.fromTag(
        context.getSharedPreferences(SYNC_PREFS, Context.MODE_PRIVATE)
            .getString(Keys.LANGUAGE.name, null),
    )

    private fun Preferences.toSettings() = AppSettings(
        themeMode = runCatching { ThemeMode.valueOf(this[Keys.THEME] ?: "") }
            .getOrDefault(ThemeMode.SYSTEM),
        language = AppLanguage.fromTag(this[Keys.LANGUAGE]),
        rateMode = RateMode.fromId(this[Keys.RATE_MODE]),
        decimals = this[Keys.DECIMALS] ?: 2,
        grouping = this[Keys.GROUPING] ?: true,
        syncOnlyOnWifi = this[Keys.WIFI_ONLY] ?: false,
        syncIntervalHours = this[Keys.SYNC_INTERVAL] ?: 6,
        showChangePercent = this[Keys.SHOW_CHANGE] ?: true,
    )

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val RATE_MODE = stringPreferencesKey("rate_mode")
        val DECIMALS = intPreferencesKey("decimals")
        val GROUPING = booleanPreferencesKey("grouping")
        val WIFI_ONLY = booleanPreferencesKey("sync_wifi_only")
        val SYNC_INTERVAL = intPreferencesKey("sync_interval_hours")
        val SHOW_CHANGE = booleanPreferencesKey("show_change")
    }

    private companion object {
        const val SYNC_PREFS = "settings_sync"
    }
}
