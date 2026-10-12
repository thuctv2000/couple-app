package dev.coupleapp.lovecounter.domain

import kotlinx.coroutines.CancellationException

class LoadCounterConflict(
    private val identity: CounterIdentity,
    private val reader: CounterSnapshotReader,
    private val store: CounterConflictStore,
) {
    suspend fun load(): CounterSyncResult {
        try {
            var credential = identity.credential() ?: return CounterSyncResult.AuthRequired
            val uid = credential.uid
            if (identity.currentUid() != uid) return CounterSyncResult.AccountChanged
            var result = reader.read(credential)
            if (result == CounterReadResult.Unauthorized) {
                credential = identity.credential(true) ?: return CounterSyncResult.AuthRequired
                if (credential.uid != uid || identity.currentUid() != uid) return CounterSyncResult.AccountChanged
                result = reader.read(credential)
            }
            if (identity.currentUid() != uid) return CounterSyncResult.AccountChanged
            return when (result) {
                is CounterReadResult.Found -> {
                    if (result.snapshot.firebaseUid != uid) return CounterSyncResult.AccountChanged
                    try { store.record(result.snapshot) }
                    catch (e: CancellationException) { throw e }
                    catch (_: IllegalArgumentException) { return CounterSyncResult.AccountChanged }
                    catch (_: Exception) { return CounterSyncResult.StorageUnavailable }
                    CounterSyncResult.Conflict
                }
                CounterReadResult.Unauthorized -> CounterSyncResult.AuthRequired
                CounterReadResult.Forbidden -> CounterSyncResult.Forbidden
                CounterReadResult.RetryLater -> CounterSyncResult.RetryLater
                CounterReadResult.Missing, CounterReadResult.InvalidResponse -> CounterSyncResult.InvalidResponse
            }
        } catch (e: CancellationException) { throw e
        } catch (_: Exception) { return CounterSyncResult.RetryLater }
    }

    suspend fun resolve(localRevision: Long, serverVersion: Long, keepLocal: Boolean) {
        val uid = requireNotNull(identity.currentUid()) { "auth_required" }
        store.resolve(uid, localRevision, serverVersion, keepLocal)
    }
}
