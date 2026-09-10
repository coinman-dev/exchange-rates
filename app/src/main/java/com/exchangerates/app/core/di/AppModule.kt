package com.exchangerates.app.core.di

import android.content.Context
import androidx.room.Room
import com.exchangerates.app.data.local.AppDatabase
import com.exchangerates.app.data.local.HistoryDao
import com.exchangerates.app.data.local.RateDao
import com.exchangerates.app.data.local.UserCurrencyDao
import com.exchangerates.app.data.remote.RatesApi
import com.exchangerates.app.data.source.BinanceSource
import com.exchangerates.app.data.source.CoinGeckoSource
import com.exchangerates.app.data.source.CoinbaseSource
import com.exchangerates.app.data.source.ErApiSource
import com.exchangerates.app.data.source.FawazahmedSource
import com.exchangerates.app.data.source.FloatratesSource
import com.exchangerates.app.data.source.FrankfurterSource
import com.exchangerates.app.data.source.GoldApiSource
import com.exchangerates.app.data.source.RateSource
import com.exchangerates.app.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Duration
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun context(@ApplicationContext context: Context): Context = context

    @Provides
    @Singleton
    fun json(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun okHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(Duration.ofSeconds(10))
        .readTimeout(Duration.ofSeconds(15))
        .callTimeout(Duration.ofSeconds(20))
        .retryOnConnectionFailure(true)
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(
                    HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC },
                )
            }
        }
        .build()

    @Provides
    @Singleton
    fun retrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        // Реальные адреса приходят через @Url, базовый нужен Retrofit формально.
        .baseUrl("https://api.frankfurter.dev/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun ratesApi(retrofit: Retrofit): RatesApi = retrofit.create(RatesApi::class.java)

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun rateDao(db: AppDatabase): RateDao = db.rateDao()

    @Provides
    fun userCurrencyDao(db: AppDatabase): UserCurrencyDao = db.userCurrencyDao()

    @Provides
    fun historyDao(db: AppDatabase): HistoryDao = db.historyDao()

    /**
     * Порядок в списке не важен: приоритет каждого источника задаётся его
     * свойством [RateSource.priorities] и может быть переопределён манифестом.
     */
    @Provides
    @Singleton
    fun rateSources(
        coinbase: CoinbaseSource,
        floatrates: FloatratesSource,
        frankfurter: FrankfurterSource,
        erApi: ErApiSource,
        fawazahmed: FawazahmedSource,
        binance: BinanceSource,
        coinGecko: CoinGeckoSource,
        goldApi: GoldApiSource,
    ): List<@JvmSuppressWildcards RateSource> = listOf(
        coinbase, floatrates, frankfurter, erApi, fawazahmed, binance, coinGecko, goldApi,
    )
}
