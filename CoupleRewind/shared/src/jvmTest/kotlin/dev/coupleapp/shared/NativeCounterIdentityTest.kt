package dev.coupleapp.shared

import kotlinx.coroutines.*
import kotlin.test.*

class NativeCounterIdentityTest {
    @Test fun forwardsRefreshAndSurfacesNativeFailure() = runBlocking {
        var refresh = false
        var fail = false
        val native = object : NativeCounterIdentity {
            override fun currentUid() = "user-a"
            override fun requestCredential(forceRefresh: Boolean, completion: (NativeCounterCredential?, String?) -> Unit) {
                refresh = forceRefresh
                if (fail) completion(null, "network_unavailable")
                else completion(NativeCounterCredential("user-a", "test-token"), null)
            }
        }
        val adapter = NativeIdentityAdapter(native)
        assertEquals("user-a", adapter.currentUid())
        assertEquals("test-token", adapter.credential(true)?.idToken)
        assertTrue(refresh)
        fail = true
        assertFailsWith<IllegalStateException> { adapter.credential(false) }
        Unit
    }

    @Test fun lateNativeCallbackDoesNotResumeCancelledWork() = runBlocking {
        var callback: ((NativeCounterCredential?, String?) -> Unit)? = null
        var resumed = false
        val native = object : NativeCounterIdentity {
            override fun currentUid() = "user-a"
            override fun requestCredential(forceRefresh: Boolean, completion: (NativeCounterCredential?, String?) -> Unit) { callback = completion }
        }
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            NativeIdentityAdapter(native).credential(false)
            resumed = true
        }
        job.cancelAndJoin()
        assertNotNull(callback).invoke(NativeCounterCredential("user-a", "late-token"), null)
        assertFalse(resumed)
    }
}
