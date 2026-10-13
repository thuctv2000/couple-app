package dev.coupleapp.lovecounter.domain

data class CounterSnapshot(val firebaseUid: String, val serverVersion: Long, val payload: CounterDraft)

interface CounterSnapshotReader {
    suspend fun read(credential: CounterCredential): CounterReadResult
}

sealed interface CounterReadResult {
    data class Found(val snapshot: CounterSnapshot) : CounterReadResult
    data object Missing : CounterReadResult
    data object Unauthorized : CounterReadResult
    data object Forbidden : CounterReadResult
    data object RetryLater : CounterReadResult
    data object InvalidResponse : CounterReadResult
}
