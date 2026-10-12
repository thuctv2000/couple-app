package dev.coupleapp.lovecounter.domain

import kotlin.random.Random

/** Six foreground retry delays, capped with jitter; permanent errors never retry. */
class CounterRetryPolicy(private val jitter: () -> Double = { Random.nextDouble() }) {
    fun delayMillis(result: CounterSyncResult, failedAttempts: Int): Long? {
        require(failedAttempts >= 1)
        if (result != CounterSyncResult.RetryLater || failedAttempts > 6) return null
        val base = 2_000L shl (failedAttempts - 1)
        return (base * (0.5 + jitter().coerceIn(0.0, 1.0))).toLong().coerceIn(1_000L, 60_000L)
    }
}
