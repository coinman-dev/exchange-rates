package com.exchangerates.app.data.repository

import android.util.Log
import com.exchangerates.app.data.catalog.CurrencyCatalog
import com.exchangerates.app.data.local.RateDao
import com.exchangerates.app.data.local.RateEntity
import com.exchangerates.app.data.local.UserCurrencyDao
import com.exchangerates.app.data.local.UserCurrencyEntity
import com.exchangerates.app.data.source.RateResolver
import com.exchangerates.app.domain.model.RateEntry
import com.exchangerates.app.domain.model.RateMode
import com.exchangerates.app.domain.model.RateTable
import com.exchangerates.app.domain.model.SourceStatus
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Список валют пользователя: базовая + порядок остальных карточек. */
data class UserCurrencies(
    val base: String,
    val others: List<String>,
) {
    val all: List<String> get() = listOf(base) + others
    val isEmpty: Boolean get() = others.isEmpty() && base.isEmpty()
}

@Singleton
class RatesRepository @Inject constructor(
    private val rateDao: RateDao,
    private val userDao: UserCurrencyDao,
    private val catalog: CurrencyCatalog,
    private val resolver: RateResolver,
) {
    private val refreshMutex = Mutex()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _statuses = MutableStateFlow<List<SourceStatus>>(emptyList())
    val statuses: StateFlow<List<SourceStatus>> = _statuses.asStateFlow()

    private val _offline = MutableStateFlow(false)
    val offline: StateFlow<Boolean> = _offline.asStateFlow()

    // ---------- курсы ----------

    fun observeTable(mode: RateMode): Flow<RateTable> = combine(
        rateDao.observe(mode.id),
        rateDao.observe(previousModeId(mode)),
        _offline,
    ) { current, previous, isOffline ->
        buildTable(mode, current, previous, isOffline)
    }

    private fun buildTable(
        mode: RateMode,
        current: List<RateEntity>,
        previous: List<RateEntity>,
        isOffline: Boolean,
    ): RateTable {
        val entries = current.associate { entity ->
            entity.quote to RateEntry(
                quote = entity.quote,
                rate = BigDecimal(entity.rate),
                sourceId = entity.sourceId,
                asOf = Instant.ofEpochMilli(entity.asOf),
            )
        }
        return RateTable(
            mode = mode,
            entries = entries,
            fetchedAt = current.maxOfOrNull { it.fetchedAt }?.let(Instant::ofEpochMilli),
            dataAsOf = current.maxOfOrNull { it.asOf }?.let(Instant::ofEpochMilli),
            isOffline = isOffline || current.isEmpty(),
            previousDay = previous.associate { it.quote to BigDecimal(it.rate) },
        )
    }

    /** Первый запуск: подкладываем снимок курсов, вшитый в APK. */
    suspend fun seedIfEmpty() {
        if (rateDao.get(RateMode.MID_MARKET.id).isNotEmpty()) return
        val initial = catalog.initialRates()
        val now = initial.asOf.toEpochMilli()
        val entities = initial.rates.map { (code, rate) ->
            RateEntity(
                mode = RateMode.MID_MARKET.id,
                quote = code,
                rate = rate.toPlainString(),
                sourceId = BUNDLED_SOURCE_ID,
                asOf = now,
                fetchedAt = now,
            )
        }
        rateDao.upsert(entities)
        Log.i(TAG, "подложен стартовый снимок: ${entities.size} курсов от ${initial.asOf}")
    }

    /** @return true, если сетка обновилась из сети. */
    suspend fun refresh(mode: RateMode, force: Boolean = false): Boolean {
        if (refreshMutex.isLocked && !force) return false
        return refreshMutex.withLock {
            if (!force && isFresh(mode)) return@withLock false
            _refreshing.value = true
            try {
                val outcome = resolver.resolve(mode)
                _statuses.value = outcome.statuses
                if (outcome.entries.isEmpty()) {
                    _offline.value = true
                    return@withLock false
                }
                val now = Instant.now().toEpochMilli()
                rateDao.replace(
                    mode.id,
                    outcome.entries.map { entry ->
                        RateEntity(
                            mode = mode.id,
                            quote = entry.quote,
                            rate = entry.rate.toPlainString(),
                            sourceId = entry.sourceId,
                            asOf = entry.asOf.toEpochMilli(),
                            fetchedAt = now,
                        )
                    },
                )
                if (outcome.previousDay.isNotEmpty()) {
                    rateDao.replace(
                        previousModeId(mode),
                        outcome.previousDay.map { (code, rate) ->
                            RateEntity(
                                mode = previousModeId(mode),
                                quote = code,
                                rate = rate.toPlainString(),
                                sourceId = "frankfurter:prev",
                                asOf = now,
                                fetchedAt = now,
                            )
                        },
                    )
                }
                _offline.value = false
                true
            } catch (e: Exception) {
                Log.w(TAG, "обновление курсов не удалось: ${e.message}")
                _offline.value = true
                false
            } finally {
                _refreshing.value = false
            }
        }
    }

    private suspend fun isFresh(mode: RateMode): Boolean {
        val last = rateDao.lastFetchedAt(mode.id) ?: return false
        val age = Duration.between(Instant.ofEpochMilli(last), Instant.now())
        return age < FRESH_WINDOW
    }

    // ---------- список валют пользователя ----------

    fun observeUserCurrencies(): Flow<UserCurrencies> = combine(
        userDao.observe(),
        _offline,
    ) { items, _ -> items.toUserCurrencies() }

    private fun List<UserCurrencyEntity>.toUserCurrencies(): UserCurrencies {
        if (isEmpty()) return UserCurrencies(DEFAULT_BASE, DEFAULT_OTHERS)
        val base = firstOrNull { it.isBase }?.code ?: first().code
        val others = filter { it.code != base }.sortedBy { it.position }.map { it.code }
        return UserCurrencies(base, others)
    }

    suspend fun seedUserCurrenciesIfEmpty() {
        if (userDao.count() > 0) return
        persist(UserCurrencies(DEFAULT_BASE, DEFAULT_OTHERS))
    }

    suspend fun addCurrency(code: String) {
        val current = userDao.observe().first().toUserCurrencies()
        if (code == current.base || code in current.others) return
        persist(current.copy(others = current.others + code))
    }

    suspend fun removeCurrency(code: String) {
        val current = userDao.observe().first().toUserCurrencies()
        if (code == current.base) {
            val next = current.others.firstOrNull() ?: return
            persist(UserCurrencies(next, current.others.drop(1)))
        } else {
            persist(current.copy(others = current.others - code))
        }
    }

    /** Замена валюты в конкретной карточке (нажатие на «USD ⌄»). */
    suspend fun replaceCurrency(oldCode: String, newCode: String) {
        val current = userDao.observe().first().toUserCurrencies()
        if (newCode == oldCode) return
        if (newCode == current.base || newCode in current.others) {
            // валюта уже есть в списке — просто убираем дубль на месте замены
            removeCurrency(oldCode)
            return
        }
        val next = if (oldCode == current.base) {
            current.copy(base = newCode)
        } else {
            current.copy(others = current.others.map { if (it == oldCode) newCode else it })
        }
        persist(next)
    }

    suspend fun setBase(code: String) {
        val current = userDao.observe().first().toUserCurrencies()
        if (code == current.base) return
        val others = buildList {
            addAll(current.others.map { if (it == code) current.base else it })
            if (code !in current.others) add(0, current.base)
        }
        persist(UserCurrencies(code, others))
    }

    suspend fun reorder(base: String, others: List<String>) {
        persist(UserCurrencies(base, others))
    }

    private suspend fun persist(currencies: UserCurrencies) {
        val entities = buildList {
            add(UserCurrencyEntity(currencies.base, 0, isBase = true))
            currencies.others.forEachIndexed { index, code ->
                add(UserCurrencyEntity(code, index + 1, isBase = false))
            }
        }
        userDao.replaceAll(entities)
    }

    private fun previousModeId(mode: RateMode) = "${mode.id}#prev"

    companion object {
        const val BUNDLED_SOURCE_ID = "bundled"
        private const val TAG = "RatesRepository"
        private val FRESH_WINDOW: Duration = Duration.ofMinutes(5)

        /** Стартовый набор по договорённости: база USD и рубль. */
        const val DEFAULT_BASE = "USD"
        val DEFAULT_OTHERS = listOf("RUB")
    }
}
