package dev.coupleapp.lovecounter.data.local

import dev.coupleapp.lovecounter.domain.CounterDraft
import dev.coupleapp.lovecounter.domain.CounterSyncQueue
import dev.coupleapp.lovecounter.domain.PreparedCounterOperation
import kotlinx.datetime.LocalDate

class LocalCounterSyncQueue(private val database: CounterDatabase) : CounterSyncQueue {
    override suspend fun bindFirebaseUid(uid: String) = database.counterDao().bindFirebaseUid(uid)

    override suspend fun prepareNext(uid: String): PreparedCounterOperation? =
        database.counterDao().prepareNextOperation(uid)?.let {
            PreparedCounterOperation(
                it.operationId, requireNotNull(it.firebaseUid), requireNotNull(it.baseServerVersion),
                it.localRevision, it.payloadVersion,
                CounterDraft(it.displayName, it.partnerName, LocalDate.parse(it.startDate), it.timeZoneId),
            )
        }

    override suspend fun acknowledge(uid: String, operationId: String, serverVersion: Long) =
        database.counterDao().acknowledge(uid, operationId, serverVersion)
}
