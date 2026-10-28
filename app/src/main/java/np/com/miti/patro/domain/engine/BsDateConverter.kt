package np.com.miti.patro.domain.engine

import np.com.miti.patro.data.local.EventEntity
import np.com.miti.patro.domain.model.BsDate
import np.com.miti.patro.domain.model.MissingDayStrategy
import np.com.miti.patro.domain.model.ResolvedBsOccurrence
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * Bidirectional Bikram Sambat (BS) <-> Gregorian (AD) converter and annual
 * event recurrence resolver (supporting both solar Gatey and lunar Shraddha Tithi).
 */
object BsDateConverter {

    /**
     * Returns 0 for Sunday, 1 for Monday, ..., 6 for Saturday.
     */
    fun dayOfWeekIndex(adDate: LocalDate): Int = adDate.dayOfWeek.value % 7

    fun bsToAd(bsDate: BsDate): LocalDate = bsToAd(bsDate.year, bsDate.month, bsDate.day)

    /**
     * O(1) conversion from BS (year, month, day) to Gregorian LocalDate.
     */
    fun bsToAd(bsYear: Int, bsMonth: Int, bsDay: Int): LocalDate {
        require(bsMonth in 1..12) { "BS month must be between 1 and 12" }
        val safeYear = bsYear.coerceIn(BsCalendarData.MIN_YEAR, BsCalendarData.MAX_YEAR)
        val maxDay = BsCalendarData.getDaysInMonth(safeYear, bsMonth)
        val clampedDay = bsDay.coerceIn(1, maxDay)

        val yearIdx = safeYear - BsCalendarData.MIN_YEAR
        val totalDaysFrom1970 = BsCalendarData.YEAR_START_OFFSET_FROM_1970[yearIdx].toLong() +
            BsCalendarData.getDaysBeforeMonthInYear(safeYear, bsMonth).toLong() +
            (clampedDay - 1).toLong()

        return BsCalendarData.EPOCH_1970_AD_DATE.plusDays(totalDaysFrom1970)
    }

    /**
     * O(log N) binary-search conversion from Gregorian LocalDate to BS date.
     */
    fun adToBs(adDate: LocalDate): BsDate {
        val daysFrom1970 = ChronoUnit.DAYS.between(BsCalendarData.EPOCH_1970_AD_DATE, adDate).toInt()
        if (daysFrom1970 <= 0) {
            return BsDate(BsCalendarData.MIN_YEAR, 1, 1)
        }

        val offsets = BsCalendarData.YEAR_START_OFFSET_FROM_1970
        val searchIdx = offsets.binarySearch(daysFrom1970)
        val yearIdx = if (searchIdx >= 0) {
            searchIdx
        } else {
            (-searchIdx - 2).coerceIn(0, offsets.size - 1)
        }

        val bsYear = BsCalendarData.MIN_YEAR + yearIdx
        var dayInYear = daysFrom1970 - offsets[yearIdx]

        var bsMonth = 1
        while (bsMonth <= 12) {
            val dim = BsCalendarData.getDaysInMonth(bsYear, bsMonth)
            if (dayInYear < dim) break
            dayInYear -= dim
            bsMonth++
        }

        if (bsMonth > 12) {
            val lastDim = BsCalendarData.getDaysInMonth(bsYear, 12)
            return BsDate(bsYear, 12, lastDim)
        }

        val bsDay = (dayInYear + 1).coerceAtLeast(1)
        return BsDate(bsYear, bsMonth, bsDay)
    }

    /**
     * Resolves a Shraddha (Lunar Tithi) annual occurrence inside [bsMonth] of [targetBsYear].
     * Scans all days in [bsMonth] for the exact sunrise Tithi [shraddhaTithi] (1..30),
     * falling back to the closest lunar elongation day if the Tithi is kshaya (skipped at sunrise).
     */
    fun resolveShraddhaOccurrence(
        bsMonth: Int,
        shraddhaTithi: Int,
        targetBsYear: Int
    ): ResolvedBsOccurrence? {
        if (targetBsYear !in BsCalendarData.MIN_YEAR..BsCalendarData.MAX_YEAR) return null
        val targetTithi = shraddhaTithi.coerceIn(1, 30)
        val daysInMonth = BsCalendarData.getDaysInMonth(targetBsYear, bsMonth)
        val firstDayAd = bsToAd(targetBsYear, bsMonth, 1)

        var bestDay = 1
        var bestAngleDiff = Double.MAX_VALUE
        val targetCenterAngle = (targetTithi - 1) * 12.0 + 6.0

        for (day in 1..daysInMonth) {
            val bsDate = BsDate(targetBsYear, bsMonth, day)
            val adDate = firstDayAd.plusDays((day - 1).toLong())
            val tithiInfo = TithiCalculator.calculateTithi(bsDate, adDate)
            if (tithiInfo.number == targetTithi) {
                return ResolvedBsOccurrence(
                    targetDate = bsDate,
                    originalDay = day,
                    isClampedOrRolled = false
                )
            }
            val rawDiff = abs(tithiInfo.elongationDeg - targetCenterAngle)
            val circularDiff = minOf(rawDiff, 360.0 - rawDiff)
            if (circularDiff < bestAngleDiff) {
                bestAngleDiff = circularDiff
                bestDay = day
            }
        }

        return ResolvedBsOccurrence(
            targetDate = BsDate(targetBsYear, bsMonth, bestDay),
            originalDay = bestDay,
            isClampedOrRolled = true
        )
    }

    /**
     * Unified resolver for any EventEntity (either standard BS Gatey or Lunar Tithi Shraddha).
     */
    fun resolveEventOccurrence(
        event: EventEntity,
        targetBsYear: Int,
        strategy: MissingDayStrategy = MissingDayStrategy.CLAMP_TO_LAST_DAY
    ): ResolvedBsOccurrence? {
        return if (event.isShraddha && event.shraddhaTithi in 1..30) {
            resolveShraddhaOccurrence(
                bsMonth = event.bsMonth,
                shraddhaTithi = event.shraddhaTithi,
                targetBsYear = targetBsYear
            )
        } else {
            resolveAnnualOccurrence(
                eventMonth = event.bsMonth,
                eventDay = event.bsDay,
                targetBsYear = targetBsYear,
                strategy = strategy
            )
        }
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
