package dev.coupleapp.lovecounter.domain

import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class CountLoveDaysAtInstant {
    private val calculate = CalculateLoveDays()

    operator fun invoke(startDate: LocalDate, now: Instant, timeZone: TimeZone): LoveDays =
        calculate(startDate, now.toLocalDateTime(timeZone).date)
}
