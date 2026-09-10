package com.exchangerates.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface RateDao {
    @Query("SELECT * FROM rates WHERE mode = :mode")
    fun observe(mode: String): Flow<List<RateEntity>>

    @Query("SELECT * FROM rates WHERE mode = :mode")
    suspend fun get(mode: String): List<RateEntity>

    @Query("SELECT MAX(fetchedAt) FROM rates WHERE mode = :mode")
    suspend fun lastFetchedAt(mode: String): Long?

    @Upsert
    suspend fun upsert(rates: List<RateEntity>)

    @Query("DELETE FROM rates WHERE mode = :mode")
    suspend fun clear(mode: String)

    @Transaction
    suspend fun replace(mode: String, rates: List<RateEntity>) {
        clear(mode)
        upsert(rates)
    }
}

@Dao
interface UserCurrencyDao {
    @Query("SELECT * FROM user_currencies ORDER BY position ASC")
    fun observe(): Flow<List<UserCurrencyEntity>>

    @Query("SELECT COUNT(*) FROM user_currencies")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(items: List<UserCurrencyEntity>)

    @Query("DELETE FROM user_currencies WHERE code = :code")
    suspend fun delete(code: String)

    @Query("DELETE FROM user_currencies")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(items: List<UserCurrencyEntity>) {
        clear()
        upsert(items)
    }
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history WHERE pair = :pair AND epochDay >= :fromDay ORDER BY epochDay ASC")
    suspend fun range(pair: String, fromDay: Long): List<HistoryEntity>

    @Query("SELECT MAX(fetchedAt) FROM history WHERE pair = :pair")
    suspend fun lastFetchedAt(pair: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(points: List<HistoryEntity>)

    @Query("DELETE FROM history WHERE fetchedAt < :olderThan")
    suspend fun prune(olderThan: Long)
}
