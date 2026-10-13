package dev.coupleapp.lovecounter.data.local

import dev.coupleapp.lovecounter.domain.*
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

class LocalCounterConflictStore(private val database: CounterDatabase) : CounterConflictStore {
    override fun observeConflict() = database.counterDao().observeConflict().map { value -> value?.let {
        CounterConflict(CounterSnapshot(it.firebaseUid, it.serverVersion, CounterDraft(it.displayName, it.partnerName, LocalDate.parse(it.startDate), it.timeZoneId)), it.localRevision)
    } }
    override suspend fun record(snapshot: CounterSnapshot) {
        val p = snapshot.payload
        require(p.displayName == p.displayName.trim() && p.displayName.length <= 80)
        require(p.partnerName == p.partnerName.trim() && p.partnerName.length <= 80)
        TimeZone.of(p.timeZoneId)
        database.counterDao().recordConflict(snapshot.firebaseUid, snapshot.serverVersion, p.displayName, p.partnerName, p.startDate.toString(), p.timeZoneId)
    }
    override suspend fun resolve(uid: String, localRevision: Long, serverVersion: Long, keepLocal: Boolean) =
        database.counterDao().resolveConflict(uid, localRevision, serverVersion, keepLocal)
}
