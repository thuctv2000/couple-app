package dev.coupleapp.lovecounter.data.local

import dev.coupleapp.lovecounter.domain.*

class LocalCounterSnapshotStore(private val database: CounterDatabase) : CounterSnapshotStore {
    override suspend fun apply(snapshot: CounterSnapshot): CounterSnapshotApplication {
        val payload = snapshot.payload
        return database.counterDao().applySnapshot(snapshot.firebaseUid, snapshot.serverVersion,
            payload.displayName, payload.partnerName, payload.startDate.toString(), payload.timeZoneId)
    }
}
