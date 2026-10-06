package dev.coupleapp.shared

import kotlin.test.Test
import kotlin.test.assertEquals

class LoveCounterFacadeTest {
    private val facade = LoveCounterFacade()

    @Test fun malformedDateReturnsAnErrorAcrossTheSwiftBoundary() {
        assertEquals("invalid_date", facade.calculate("not-a-date", "UTC").errorCode)
    }

    @Test fun invalidLeapDateIsRejected() {
        assertEquals("invalid_date", facade.calculate("2025-02-29", "UTC").errorCode)
    }

    @Test fun unknownTimeZoneIsRejected() {
        assertEquals("invalid_time_zone", facade.calculate("2020-01-01", "Invalid/Zone").errorCode)
    }
}
