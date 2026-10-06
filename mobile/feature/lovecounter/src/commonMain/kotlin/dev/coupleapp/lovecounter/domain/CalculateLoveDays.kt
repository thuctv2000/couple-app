package dev.coupleapp.lovecounter.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

/** Counts calendar dates inclusively: the starting date is day one. */
class CalculateLoveDays {
    operator fun invoke(startDate: LocalDate, today: LocalDate): LoveDays {
        if (startDate > today) return LoveDays.FutureStartDate
        return LoveDays.Count(startDate.daysUntil(today) + 1)
    }
}

sealed interface LoveDays {
    data class Count(val days: Int) : LoveDays
    data object FutureStartDate : LoveDays
}
