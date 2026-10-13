package dev.coupleapp.lovecounter.data.local

import dev.coupleapp.lovecounter.domain.CounterDraft
import dev.coupleapp.lovecounter.domain.CounterRepository
import dev.coupleapp.lovecounter.domain.CounterSettings
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

class LocalCounterRepository(private val database: CounterDatabase) : CounterRepository {
    private val dao get() = database.counterDao()
    override fun observeSettings() = dao.observeCurrent().map { it?.toDomain() }
    override fun observePendingCount() = dao.observePendingCount()
    override suspend fun readSettings() = dao.current()?.toDomain()
    override suspend fun save(draft: CounterDraft, today: LocalDate, expectedRevision: Long?): CounterSettings {
        val value = draft.validated(today)
        return dao.save(value.displayName, value.partnerName, value.startDate.toString(), value.timeZoneId, expectedRevision = expectedRevision).toDomain()
    }
}

private fun CounterEntity.toDomain() = CounterSettings(localOwnerId, displayName, partnerName, LocalDate.parse(startDate), timeZoneId, localRevision)

/** Keeps Room types behind the feature boundary used by host composition. */
class LocalCounterStore internal constructor(private val database: CounterDatabase) {
    val repository: CounterRepository = LocalCounterRepository(database)
    val syncQueue: dev.coupleapp.lovecounter.domain.CounterSyncQueue = LocalCounterSyncQueue(database)
    val conflicts: dev.coupleapp.lovecounter.domain.CounterConflictStore = LocalCounterConflictStore(database)
    val snapshots: dev.coupleapp.lovecounter.domain.CounterSnapshotStore = LocalCounterSnapshotStore(database)
    fun close() = database.close()
}
