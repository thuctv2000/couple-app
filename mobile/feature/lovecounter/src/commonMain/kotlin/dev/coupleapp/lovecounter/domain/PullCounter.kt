package dev.coupleapp.lovecounter.domain

import kotlinx.coroutines.CancellationException

interface CounterSnapshotStore {
    /** Check ownership, pending edits and version in the same transaction as replacement. */
    suspend fun apply(snapshot: CounterSnapshot): CounterSnapshotApplication
}

enum class CounterSnapshotApplication { Applied, Unchanged, LocalChanges, Conflict, Stale }

class PullCounter(
    private val identity: CounterIdentity,
    private val queue: CounterSyncQueue,
    private val reader: CounterSnapshotReader,
    private val store: CounterSnapshotStore,
) {
    /** Null means no existing identity or no server copy; local data is untouched. */
    suspend fun run(): CounterSyncResult? {
        val uid = identity.currentUid() ?: return null
        try {
            var credential = identity.credential() ?: return CounterSyncResult.AuthRequired
            if (credential.uid != uid || identity.currentUid() != uid) return CounterSyncResult.AccountChanged
            try { queue.bindFirebaseUid(uid) }
            catch (e: CancellationException) { throw e }
            catch (_: IllegalArgumentException) { return CounterSyncResult.AccountChanged }
            catch (_: Exception) { return CounterSyncResult.StorageUnavailable }
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
                    val applied = try { store.apply(result.snapshot) }
                    catch (e: CancellationException) { throw e }
                    catch (_: IllegalArgumentException) { return CounterSyncResult.AccountChanged }
                    catch (_: Exception) { return CounterSyncResult.StorageUnavailable }
                    when (applied) {
                        CounterSnapshotApplication.Applied, CounterSnapshotApplication.Unchanged -> CounterSyncResult.Drained(0)
                        CounterSnapshotApplication.LocalChanges -> CounterSyncResult.MorePending(0)
                        CounterSnapshotApplication.Conflict -> CounterSyncResult.Conflict
                        CounterSnapshotApplication.Stale -> CounterSyncResult.InvalidResponse
                    }
                }
                CounterReadResult.Missing -> null
                CounterReadResult.Unauthorized -> CounterSyncResult.AuthRequired
                CounterReadResult.Forbidden -> CounterSyncResult.Forbidden
                CounterReadResult.RetryLater -> CounterSyncResult.RetryLater
                CounterReadResult.InvalidResponse -> CounterSyncResult.InvalidResponse
            }
        } catch (e: CancellationException) { throw e
        } catch (_: Exception) { return CounterSyncResult.RetryLater }
    }
}
