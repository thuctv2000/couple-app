package dev.coupleapp.lovecounter.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

class CalculateLoveDaysTest {
    private val calculate = CalculateLoveDays()

    @Test fun startingDateIsDayOne() {
        val date = LocalDate(2026, 10, 5)
        assertEquals(LoveDays.Count(1), calculate(date, date))
    }

    @Test fun leapDayCountsAsACalendarDate() {
        assertEquals(LoveDays.Count(3), calculate(LocalDate(2024, 2, 28), LocalDate(2024, 3, 1)))
    }

    @Test fun crossingYearCountsCorrectly() {
        assertEquals(LoveDays.Count(2), calculate(LocalDate(2025, 12, 31), LocalDate(2026, 1, 1)))
    }

    @Test fun futureStartDateIsRejected() {
        assertEquals(LoveDays.FutureStartDate, calculate(LocalDate(2026, 10, 6), LocalDate(2026, 10, 5)))
    }

    @Test fun savedTimeZoneDeterminesCalendarDay() {
        val count = CountLoveDaysAtInstant()
        val start = LocalDate(2026, 10, 5)
        val now = Instant.parse("2026-10-05T17:30:00Z")
        assertEquals(LoveDays.Count(2), count(start, now, TimeZone.of("Asia/Ho_Chi_Minh")))
        assertEquals(LoveDays.Count(1), count(start, now, TimeZone.UTC))
    }

    @Test fun daylightSavingDoesNotChangeCalendarCounting() {
        val count = CountLoveDaysAtInstant()
        val zone = TimeZone.of("America/New_York")
        val start = LocalDate(2026, 3, 7)
        assertEquals(LoveDays.Count(3), count(start, Instant.parse("2026-03-09T04:00:00Z"), zone))
    }
}
