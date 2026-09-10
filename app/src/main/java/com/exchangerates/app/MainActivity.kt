package com.exchangerates.app

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.exchangerates.app.core.theme.ExchangeRatesTheme
import com.exchangerates.app.data.local.AppLanguage
import com.exchangerates.app.data.local.AppSettings
import com.exchangerates.app.data.local.SettingsStore
import com.exchangerates.app.presentation.navigation.MainScreen
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsStore: SettingsStore

    /**
     * Язык применяется подменой конфигурации базового контекста: это работает
     * на всех поддерживаемых версиях Android и не требует AppCompat.
     */
    override fun attachBaseContext(newBase: Context) {
        val entryPoint = EntryPointAccessors.fromApplication(
            newBase.applicationContext,
            SettingsEntryPoint::class.java,
        )
        val language = entryPoint.settingsStore().languageBlocking()
        super.attachBaseContext(wrapLocale(newBase, language))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val settingsFlow = settingsStore.settings.stateIn(
            scope = lifecycleScope,
            started = SharingStarted.Eagerly,
            initialValue = AppSettings(),
        )
        setContent {
            val settings by settingsFlow.collectAsStateWithLifecycle()
            ExchangeRatesTheme(themeMode = settings.themeMode) {
                MainScreen()
            }
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SettingsEntryPoint {
        fun settingsStore(): SettingsStore
    }

    private companion object {
        fun wrapLocale(context: Context, language: AppLanguage): Context {
            if (language == AppLanguage.SYSTEM) return context
            val locale = Locale.forLanguageTag(language.tag)
            Locale.setDefault(locale)
            val configuration = Configuration(context.resources.configuration)
            configuration.setLocale(locale)
            configuration.setLayoutDirection(locale)
            return context.createConfigurationContext(configuration)
        }
    }
}
