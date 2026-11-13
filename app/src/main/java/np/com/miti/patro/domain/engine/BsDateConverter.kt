package np.com.miti.patro.domain.engine

import np.com.miti.patro.data.local.EventEntity
import np.com.miti.patro.domain.model.BsDate
import np.com.miti.patro.domain.model.MissingDayStrategy
import np.com.miti.patro.domain.model.ResolvedBsOccurrence
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * Bidirectional O(1) converter between Bikram Sambat (BS) and Gregorian (AD) dates,
 * plus annual repeating BS and Lunar Shraddha Tithi event resolution.
 */
object BsDateConverter {

    fun bsToAd(bsDate: BsDate): LocalDate = bsToAd(bsDate.year, bsDate.month, bsDate.day)

    fun bsToAd(bsYear: Int, bsMonth: Int, bsDay: Int): LocalDate {
        val clampedYear = bsYear.coerceIn(BsCalendarData.MIN_YEAR, BsCalendarData.MAX_YEAR)
        val clampedMonth = bsMonth.coerceIn(1, 12)
        val maxDay = BsCalendarData.getDaysInMonth(clampedYear, clampedMonth)
        val clampedDay = bsDay.coerceIn(1, maxDay)

        val yearIdx = clampedYear - BsCalendarData.MIN_YEAR
        val daysOffset = BsCalendarData.YEAR_START_DAYS_FROM_MIN[yearIdx] +
            BsCalendarData.getDaysBeforeMonthInYear(clampedYear, clampedMonth) +
            (clampedDay - 1)
        return BsCalendarData.MIN_AD_DATE.plusDays(daysOffset.toLong())
    }

    fun adToBs(adDate: LocalDate): BsDate {
        val totalDays = ChronoUnit.DAYS.between(BsCalendarData.MIN_AD_DATE, adDate).toInt()
        if (totalDays <= 0) {
            return BsDate(BsCalendarData.MIN_YEAR, 1, 1)
        }
        val maxTotalDays = BsCalendarData.YEAR_START_DAYS_FROM_MIN[BsCalendarData.YEAR_COUNT]
        if (totalDays >= maxTotalDays) {
            val lastDay = BsCalendarData.getDaysInMonth(BsCalendarData.MAX_YEAR, 12)
            return BsDate(BsCalendarData.MAX_YEAR, 12, lastDay)
        }

        var low = 0
        var high = BsCalendarData.YEAR_COUNT - 1
        while (low < high) {
            val mid = (low + high + 1) ushr 1
            if (BsCalendarData.YEAR_START_DAYS_FROM_MIN[mid] <= totalDays) {
                low = mid
            } else {
                high = mid - 1
            }
        }

        val currentYear = BsCalendarData.MIN_YEAR + low
        val dayInYear = totalDays - BsCalendarData.YEAR_START_DAYS_FROM_MIN[low]

        var currentMonth = 1
        for (m in 12 downTo 1) {
            if (BsCalendarData.getDaysBeforeMonthInYear(currentYear, m) <= dayInYear) {
                currentMonth = m
                break
            }
        }
        val currentDay = dayInYear - BsCalendarData.getDaysBeforeMonthInYear(currentYear, currentMonth) + 1
        return BsDate(currentYear, currentMonth, currentDay)
    }

    fun dayOfWeekIndex(adDate: LocalDate): Int {
        return adDate.dayOfWeek.value % 7
    }

    fun todayBs(): BsDate = adToBs(LocalDate.now())

    /**
     * Resolves an annual BS event OR an annual Lunar Shraddha Tithi event for [targetBsYear]:
     * - If [event.isShraddha] is true, evaluates Aparahna (14:00 NST), Sunrise (05:45 NST), and
     *   Kshaya Tithi across the target BS month. If a 31/32-day solar month contains two occurrences
     *   of the same Tithi (Day 1-2 and Day 30-32), it picks the single occurrence closest to [event.bsDay]
     *   in a non-Adhik lunation so a Shraddha never triggers twice in the same month.
     * - Otherwise, resolves by solar BS month and day using [strategy].
     */
    fun resolveEventOccurrence(
        event: EventEntity,
        targetBsYear: Int,
        strategy: MissingDayStrategy
    ): ResolvedBsOccurrence? {
        if (event.isShraddha && event.shraddhaTithi in 1..30) {
            val targetTithi = event.shraddhaTithi
            val daysInTarget = BsCalendarData.getDaysInMonth(targetBsYear, event.bsMonth)
            val firstAd = bsToAd(targetBsYear, event.bsMonth, 1)

            val exactCandidates = mutableListOf<Int>()
            for (d in 1..daysInTarget) {
                val ad = firstAd.plusDays((d - 1).toLong())
                val tInfo = TithiCalculator.calculateTithiAtSunrise(ad)
                // Prefer Aparahna Tithi (traditional Shraddha rule), or Sunrise / Kshaya Tithi
                val matchesAparahna = (tInfo.aparahnaTithiNumber == targetTithi)
                val matchesSunrise = (tInfo.number == targetTithi && !tInfo.isVriddhiSecondDay)
                val matchesKshaya = (tInfo.kshayaTithiNumber == targetTithi)
                if ((matchesAparahna || matchesSunrise || matchesKshaya) && !tInfo.isAdhikMaas) {
                    // Avoid consecutive-day duplicate if yesterday already matched
                    if (exactCandidates.isEmpty() || exactCandidates.last() != d - 1) {
                        exactCandidates.add(d)
                    }
                }
            }

            if (exactCandidates.isNotEmpty()) {
                val bestDay = exactCandidates.minByOrNull { abs(it - event.bsDay) } ?: exactCandidates.first()
                return ResolvedBsOccurrence(
                    targetDate = BsDate(targetBsYear, event.bsMonth, bestDay),
                    wasAdjusted = false
                )
            }

            // Fallback if the month was Adhik or Tithi was at boundary: pick closest Aparahna Tithi
            val fallbackDay = (1..daysInTarget).minByOrNull { d ->
                val ad = firstAd.plusDays((d - 1).toLong())
                val tAp = TithiCalculator.calculateTithiAtAparahna(ad)
                val diff = abs(tAp - targetTithi)
                minOf(diff, 30 - diff) * 100 + abs(d - event.bsDay)
            } ?: 1

            return ResolvedBsOccurrence(
                targetDate = BsDate(targetBsYear, event.bsMonth, fallbackDay),
                wasAdjusted = true,
                adjustmentNote = "Tithi Kshaya fallback to Day $fallbackDay"
            )
        }

        return resolveRepeatingDate(
            originMonth = event.bsMonth,
            originDay = event.bsDay,
            targetBsYear = targetBsYear,
            strategy = strategy
        )
    }

    fun resolveRepeatingDate(
        originMonth: Int,
        originDay: Int,
        targetBsYear: Int,
        strategy: MissingDayStrategy
    ): ResolvedBsOccurrence? {
        if (targetBsYear !in BsCalendarData.MIN_YEAR..BsCalendarData.MAX_YEAR) return null
        val daysInTargetMonth = BsCalendarData.getDaysInMonth(targetBsYear, originMonth)

        if (originDay <= daysInTargetMonth) {
            return ResolvedBsOccurrence(
                targetDate = BsDate(targetBsYear, originMonth, originDay),
                wasAdjusted = false
            )
        }

        return when (strategy) {
            MissingDayStrategy.CLAMP_TO_LAST_DAY -> ResolvedBsOccurrence(
                targetDate = BsDate(targetBsYear, originMonth, daysInTargetMonth),
                wasAdjusted = true,
                adjustmentNote = "Day $originDay clamped to $daysInTargetMonth"
            )

            MissingDayStrategy.ROLL_TO_NEXT_MONTH -> {
                val nextMonth = if (originMonth == 12) 1 else originMonth + 1
                val nextYear = if (originMonth == 12) targetBsYear + 1 else targetBsYear
                if (nextYear > BsCalendarData.MAX_YEAR) return null
                ResolvedBsOccurrence(
                    targetDate = BsDate(nextYear, nextMonth, 1),
                    wasAdjusted = true,
                    adjustmentNote = "Day $originDay rolled over to 1 ${BsCalendarData.getMonthName(nextMonth, false)}"
                )
            }

            MissingDayStrategy.SKIP_YEAR -> null
        }
    }
}
