package com.exchangerates.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Курс к USD, полученный от конкретного источника в конкретном режиме.
 * Ключ включает режим, чтобы mid-market и курсы ЦБ не перетирали друг друга.
 */
@Entity(
    tableName = "rates",
    primaryKeys = ["mode", "quote"],
    indices = [Index("mode")],
)
data class RateEntity(
    val mode: String,
    val quote: String,
    val rate: String,
    val sourceId: String,
    val asOf: Long,
    val fetchedAt: Long,
)

/** Список валют пользователя: порядок карточек и признак базовой. */
@Entity(tableName = "user_currencies")
data class UserCurrencyEntity(
    @PrimaryKey val code: String,
    val position: Int,
    val isBase: Boolean,
)

/** Кэш исторических точек для графика. */
@Entity(
    tableName = "history",
    primaryKeys = ["pair", "epochDay"],
    indices = [Index("pair")],
)
data class HistoryEntity(
    val pair: String,
    val epochDay: Long,
    val rate: String,
    val fetchedAt: Long,
)
