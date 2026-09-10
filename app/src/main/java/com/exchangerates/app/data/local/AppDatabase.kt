package com.exchangerates.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [RateEntity::class, UserCurrencyEntity::class, HistoryEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun rateDao(): RateDao
    abstract fun userCurrencyDao(): UserCurrencyDao
    abstract fun historyDao(): HistoryDao

    companion object {
        const val NAME = "exchange_rates.db"
    }
}
