package np.com.miti.patro.domain.engine

import np.com.miti.patro.domain.model.BsDate
import np.com.miti.patro.domain.model.MissingDayStrategy
import np.com.miti.patro.domain.model.ResolvedBsOccurrence
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Bidirectional Bikram Sambat (BS) <-> Gregorian (AD) converter and annual
 * event recurrence resolver.
 */
object BsDateConverter {

    /**
     * Returns 0 for Sunday, 1 for Monday, ..., 6 for Saturday.
     */
    fun dayOfWeekIndex(adDate: LocalDate): Int = adDate.dayOfWeek.value % 7

    fun bsToAd(bsDate: BsDate): LocalDate = bsToAd(bsDate.year, bsDate.month, bsDate.day)

    fun bsToAd(bsYear: Int, bsMonth: Int, bsDay: Int): LocalDate {
        require(bsMonth in 1..12) { "BS month must be between 1 and 12" }
        val maxDay = BsCalendarData.getDaysInMonth(bsYear, bsMonth)
        val clampedDay = bsDay.coerceIn(1, maxDay)

        var totalDays = 0L
        if (bsYear >= BsCalendarData.ANCHOR_BS_YEAR) {
            for (y in BsCalendarData.ANCHOR_BS_YEAR until bsYear) {
                totalDays += BsCalendarData.getDaysInYear(y)
            }
            for (m in 1 until bsMonth) {
                totalDays += BsCalendarData.getDaysInMonth(bsYear, m)
            }
            totalDays += (clampedDay - 1)
        } else {
            for (y in bsYear until BsCalendarData.ANCHOR_BS_YEAR) {
                totalDays -= BsCalendarData.getDaysInYear(y)
            }
            for (m in 1 until bsMonth) {
                totalDays += BsCalendarData.getDaysInMonth(bsYear, m)
            }
            totalDays += (clampedDay - 1)
        }

        return BsCalendarData.ANCHOR_AD_DATE.plusDays(totalDays)
    }

    fun adToBs(adDate: LocalDate): BsDate {
        var daysDiff = ChronoUnit.DAYS.between(BsCalendarData.ANCHOR_AD_DATE, adDate)
        var bsYear = BsCalendarData.ANCHOR_BS_YEAR

        if (daysDiff >= 0) {
            while (bsYear <= BsCalendarData.MAX_YEAR) {
                val daysInYear = BsCalendarData.getDaysInYear(bsYear)
                if (daysDiff < daysInYear) break
                daysDiff -= daysInYear
                bsYear++
            }
        } else {
            while (daysDiff < 0 && bsYear > BsCalendarData.MIN_YEAR) {
                bsYear--
                daysDiff += BsCalendarData.getDaysInYear(bsYear)
            }
        }

        var bsMonth = 1
        while (bsMonth <= 12) {
            val daysInMonth = BsCalendarData.getDaysInMonth(bsYear, bsMonth)
            if (daysDiff < daysInMonth) break
            daysDiff -= daysInMonth
            bsMonth++
        }

        val bsDay = (daysDiff.toInt() + 1).coerceAtLeast(1)
        return BsDate(bsYear, bsMonth.coerceIn(1, 12), bsDay)
    }

    fun resolveAnnualOccurrence(
        eventMonth: Int,
        eventDay: Int,
        targetBsYear: Int,
        strategy: MissingDayStrategy = MissingDayStrategy.CLAMP_TO_LAST_DAY
    ): ResolvedBsOccurrence? {
        if (targetBsYear !in BsCalendarData.MIN_YEAR..BsCalendarData.MAX_YEAR) return null
        val daysInTargetMonth = BsCalendarData.getDaysInMonth(targetBsYear, eventMonth)

        if (eventDay <= daysInTargetMonth) {
            return ResolvedBsOccurrence(
                targetDate = BsDate(targetBsYear, eventMonth, eventDay),
                originalDay = eventDay,
                isClampedOrRolled = false
            )
        }

        return when (strategy) {
            MissingDayStrategy.CLAMP_TO_LAST_DAY -> {
                ResolvedBsOccurrence(
                    targetDate = BsDate(targetBsYear, eventMonth, daysInTargetMonth),
                    originalDay = eventDay,
                    isClampedOrRolled = true
                )
            }
            MissingDayStrategy.ROLL_TO_NEXT_MONTH -> {
                val overflowDays = eventDay - daysInTargetMonth
                val nextMonth = if (eventMonth == 12) 1 else eventMonth + 1
                val nextYear = if (eventMonth == 12) targetBsYear + 1 else targetBsYear
                if (nextYear > BsCalendarData.MAX_YEAR) null
                else ResolvedBsOccurrence(
                    targetDate = BsDate(nextYear, nextMonth, overflowDays),
                    originalDay = eventDay,
                    isClampedOrRolled = true
                )
            }
            MissingDayStrategy.SKIP_YEAR -> null
        }
    }
}
