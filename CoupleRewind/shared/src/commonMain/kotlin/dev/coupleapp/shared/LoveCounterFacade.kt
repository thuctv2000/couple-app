package dev.coupleapp.shared

import dev.coupleapp.lovecounter.domain.CountLoveDaysAtInstant
import dev.coupleapp.lovecounter.domain.LoveDays
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

/** Small Swift-facing boundary; Kotlin SDK/domain types stay inside shared code. */
class LoveCounterFacade {
    private val count = CountLoveDaysAtInstant()

    fun calculate(startDateIso: String, timeZoneId: String): DayCountResult {
        val date = try {
            LocalDate.parse(startDateIso)
        } catch (_: IllegalArgumentException) {
            return DayCountResult(0, "invalid_date")
        }
        val zone = try {
            TimeZone.of(timeZoneId)
        } catch (_: IllegalArgumentException) {
            return DayCountResult(0, "invalid_time_zone")
        }
        return when (val result = count(date, Clock.System.now(), zone)) {
            is LoveDays.Count -> DayCountResult(result.days, null)
            LoveDays.FutureStartDate -> DayCountResult(0, "future_date")
        }
    }
}

class DayCountResult(val days: Int, val errorCode: String?)
