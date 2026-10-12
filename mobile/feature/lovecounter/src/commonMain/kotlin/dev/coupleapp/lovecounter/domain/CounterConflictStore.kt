package dev.coupleapp.lovecounter.domain

import kotlinx.coroutines.flow.Flow

data class CounterConflict(val snapshot: CounterSnapshot, val localRevision: Long)
class CounterConflictPending : IllegalStateException("conflict_pending")

interface CounterConflictStore {
    fun observeConflict(): Flow<CounterConflict?>
    suspend fun record(snapshot: CounterSnapshot)
    /** Reject a stale choice if local edits or the displayed server snapshot changed. */
    suspend fun resolve(uid: String, localRevision: Long, serverVersion: Long, keepLocal: Boolean)
}
