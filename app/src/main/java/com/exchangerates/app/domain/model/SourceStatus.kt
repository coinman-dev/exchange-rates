package com.exchangerates.app.domain.model

import java.time.Instant

/** Итог последнего опроса одного источника — для диалога «Об источниках». */
data class SourceStatus(
    val sourceId: String,
    val title: String,
    val attribution: String?,
    val ratesCount: Int,
    val asOf: Instant?,
    val error: String?,
    val usedFor: Int = 0,
) {
    val isOk: Boolean get() = error == null && ratesCount > 0
}
