package dev.coupleapp.lovecounter.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

data class CounterSettings(
    val localOwnerId: String,
    val displayName: String,
    val partnerName: String,
    val startDate: LocalDate,
    val timeZoneId: String,
    val localRevision: Long,
)

data class CounterDraft(
    val displayName: String,
    val partnerName: String,
    val startDate: LocalDate,
    val timeZoneId: String,
) {
    fun validated(today: LocalDate): CounterDraft {
        require(startDate <= today) { "future_date" }
        require(displayName.trim().length <= 80) { "name_too_long" }
        require(partnerName.trim().length <= 80) { "name_too_long" }
        TimeZone.of(timeZoneId)
        return copy(displayName = displayName.trim(), partnerName = partnerName.trim())
    }
}

interface CounterRepository {
    fun observeSettings(): Flow<CounterSettings?>
    fun observePendingCount(): Flow<Int>
    suspend fun readSettings(): CounterSettings?
    suspend fun save(draft: CounterDraft, today: LocalDate, expectedRevision: Long? = null): CounterSettings
}

class CounterEditChanged : IllegalStateException("edit_changed")
