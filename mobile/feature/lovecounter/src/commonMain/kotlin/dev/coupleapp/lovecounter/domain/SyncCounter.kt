package dev.coupleapp.lovecounter.domain

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Intentionally not a data class: token must never appear in generated toString().
class CounterCredential(val uid: String, val idToken: String)

interface CounterIdentity {
    /** Native adapter restores/creates Anonymous; an unavailable session returns null. */
    suspend fun credential(forceRefresh: Boolean = false): CounterCredential?
    fun currentUid(): String?
}

interface CounterRemote {
    suspend fun apply(credential: CounterCredential, operation: PreparedCounterOperation): CounterRemoteResult
}

sealed interface CounterRemoteResult {
    data class Accepted(val operationId: String, val serverVersion: Long, val payload: CounterDraft) : CounterRemoteResult
    data object Unauthorized : CounterRemoteResult
    data object Forbidden : CounterRemoteResult
    data object Conflict : CounterRemoteResult
    data object InvalidOperation : CounterRemoteResult
    data object RetryLater : CounterRemoteResult
    data object InvalidResponse : CounterRemoteResult
}

sealed interface CounterSyncResult {
    data class Drained(val acknowledged: Int) : CounterSyncResult
    data class MorePending(val acknowledged: Int) : CounterSyncResult
    data object AuthRequired : CounterSyncResult
    data object AccountChanged : CounterSyncResult
    data object Conflict : CounterSyncResult
    data object Forbidden : CounterSyncResult
    data object InvalidOperation : CounterSyncResult
    data object InvalidResponse : CounterSyncResult
    data object RetryLater : CounterSyncResult
    data object StorageUnavailable : CounterSyncResult
}

/** One instance per local store. Hosts schedule bounded attempts with backoff. */
class SyncCounter(
    private val queue: CounterSyncQueue,
    private val identity: CounterIdentity,
    private val remote: CounterRemote,
) {
    private val mutex = Mutex()

    suspend fun run(maxOperations: Int = 50): CounterSyncResult = mutex.withLock {
        require(maxOperations > 0)
        var credential = try { identity.credential() } catch (e: CancellationException) { throw e
        } catch (_: Exception) { return@withLock CounterSyncResult.RetryLater }
            ?: return@withLock CounterSyncResult.AuthRequired
        val uid = credential.uid
        if (uid.isBlank() || identity.currentUid() != uid) return@withLock CounterSyncResult.AccountChanged
        try { queue.bindFirebaseUid(uid) } catch (e: CancellationException) { throw e
        } catch (_: IllegalArgumentException) { return@withLock CounterSyncResult.AccountChanged
        } catch (_: Exception) { return@withLock CounterSyncResult.StorageUnavailable }
        var acknowledged = 0
        while (acknowledged < maxOperations) {
            if (identity.currentUid() != uid) return@withLock CounterSyncResult.AccountChanged
            val operation = try { queue.prepareNext(uid) } catch (e: CancellationException) { throw e
            } catch (_: CounterConflictPending) { return@withLock CounterSyncResult.Conflict
            } catch (_: IllegalArgumentException) { return@withLock CounterSyncResult.AccountChanged
            } catch (_: Exception) { return@withLock CounterSyncResult.StorageUnavailable }
                ?: return@withLock CounterSyncResult.Drained(acknowledged)
            if (operation.firebaseUid != uid) return@withLock CounterSyncResult.AccountChanged
            var response = send(credential, operation)
            if (response == CounterRemoteResult.Unauthorized) {
                credential = try { identity.credential(forceRefresh = true) } catch (e: CancellationException) { throw e
                } catch (_: Exception) { return@withLock CounterSyncResult.RetryLater }
                    ?: return@withLock CounterSyncResult.AuthRequired
                if (credential.uid != uid || identity.currentUid() != uid) return@withLock CounterSyncResult.AccountChanged
                response = send(credential, operation)
            }
            if (identity.currentUid() != uid) return@withLock CounterSyncResult.AccountChanged
            when (response) {
                is CounterRemoteResult.Accepted -> {
                    if (response.operationId != operation.operationId || operation.baseServerVersion == Long.MAX_VALUE ||
                        response.serverVersion != operation.baseServerVersion + 1 || response.payload != operation.payload) {
                        return@withLock CounterSyncResult.InvalidResponse
                    }
                    try { queue.acknowledge(uid, operation.operationId, response.serverVersion) }
                    catch (e: CancellationException) { throw e }
                    catch (_: Exception) { return@withLock CounterSyncResult.StorageUnavailable }
                    acknowledged++
                }
                CounterRemoteResult.Unauthorized -> return@withLock CounterSyncResult.AuthRequired
                CounterRemoteResult.Forbidden -> return@withLock CounterSyncResult.Forbidden
                CounterRemoteResult.Conflict -> return@withLock CounterSyncResult.Conflict
                CounterRemoteResult.InvalidOperation -> return@withLock CounterSyncResult.InvalidOperation
                CounterRemoteResult.RetryLater -> return@withLock CounterSyncResult.RetryLater
                CounterRemoteResult.InvalidResponse -> return@withLock CounterSyncResult.InvalidResponse
            }
        }
        CounterSyncResult.MorePending(acknowledged)
    }

    private suspend fun send(credential: CounterCredential, operation: PreparedCounterOperation) =
        try { remote.apply(credential, operation) } catch (e: CancellationException) { throw e
        } catch (_: Exception) { CounterRemoteResult.RetryLater }
}
