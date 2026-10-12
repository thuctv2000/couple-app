package dev.coupleapp.shared

import dev.coupleapp.lovecounter.domain.CounterCredential
import dev.coupleapp.lovecounter.domain.CounterIdentity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Callback surface for Swift; tokens are never part of state shown to the UI. */
class NativeCounterCredential(val uid: String, val idToken: String)

interface NativeCounterIdentity {
    fun currentUid(): String?
    /** Called on main; completion must be delivered once on main. */
    fun requestCredential(forceRefresh: Boolean, completion: (NativeCounterCredential?, String?) -> Unit)
}

internal class NativeIdentityAdapter(private val native: NativeCounterIdentity) : CounterIdentity {
    override fun currentUid() = native.currentUid()
    override suspend fun credential(forceRefresh: Boolean): CounterCredential? = suspendCancellableCoroutine { continuation ->
        native.requestCredential(forceRefresh) { result, error ->
            if (continuation.isActive) {
                if (error != null) continuation.resumeWithException(IllegalStateException("native_auth_unavailable"))
                else continuation.resume(result?.let { CounterCredential(it.uid, it.idToken) })
            }
        }
    }
}
