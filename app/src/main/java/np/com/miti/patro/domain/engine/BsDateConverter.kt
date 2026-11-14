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
 * plus annual repeating BS and True Lunar Month (Chandra Maas) Shraddha Tithi event resolution.
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
     * Returns the true Amanta Lunar Month (1=Baisakh..12=Chaitra) for a Shraddha event,
     * derived from the event's origin BS date (or its stored bsMonth if origin is already in that lunar month).
     *
     * Example: A Shraddha created on 2083-05-05 (Bhadra 5, 2083 = Shrawan Shukla Nawami, 7 days before Janai Purnima)
     * belongs to Amanta Lunar Month 4 (Shrawan Shukla). In 2084 BS, Shrawan Shukla Nawami falls on
     * 2084-04-25 (Shrawan 25, 2084 — 7 days before Janai Purnima on Bhadra 1), NOT Bhadra 23/24!
     */
    fun getShraddhaLunarMonth(event: EventEntity): Int {
        val originBs = BsDate(
            year = event.startBsYear.coerceIn(BsCalendarData.MIN_YEAR, BsCalendarData.MAX_YEAR),
            month = event.bsMonth.coerceIn(1, 12),
            day = event.bsDay.coerceIn(1, BsCalendarData.getDaysInMonth(event.startBsYear, event.bsMonth))
        )
        val originAd = bsToAd(originBs)
        val (shuddhaLunarMonth, _) = TithiCalculator.getLunationInfo(originAd)
        return shuddhaLunarMonth
    }

    /**
     * Resolves an annual BS event OR an annual Lunar Shraddha Tithi event for [targetBsYear]:
     * - If [event.isShraddha] is true:
     *   1. Determines the event's true Amanta Lunar Month (`targetLunarMonth`, e.g., Shrawan Shukla = 4).
     *   2. Scans the two solar BS months that can host that lunar month (`targetLunarMonth` and `targetLunarMonth + 1`).
     *   3. Ranks matching days in that true lunar month so a day where BOTH Sunrise and Aparahna match
     *      (score 4) or Sunrise Udaya Tithi matches (score 3) wins over a Tithi Kshaya day (score 2)
     *      or an afternoon-only boundary day (score 1) — ensuring a Nawami Shraddha never lands on
     *      an Ashtami cell when the next day is a full Nawami!
     * - Otherwise, resolves by solar BS month and day using [strategy].
     */
    fun resolveEventOccurrence(
        event: EventEntity,
        targetBsYear: Int,
        strategy: MissingDayStrategy
    ): ResolvedBsOccurrence? {
        if (targetBsYear !in BsCalendarData.MIN_YEAR..BsCalendarData.MAX_YEAR) return null

        if (event.isShraddha && event.shraddhaTithi in 1..30) {
            val targetTithi = event.shraddhaTithi
            val targetLunarMonth = getShraddhaLunarMonth(event)

            // A lunar month (e.g. Shrawan Amanta = 4) always spans parts of solar BS month M (4 = Shrawan)
            // and solar BS month M+1 (5 = Bhadra).
            val nextSolarMonth = (targetLunarMonth % 12) + 1
            val candidateMonths = if (targetLunarMonth == 12) {
                listOf(targetBsYear to 12, targetBsYear to 1)
            } else {
                listOf(targetBsYear to targetLunarMonth, targetBsYear to nextSolarMonth)
            }

            var bestDate: BsDate? = null
            var bestScore = -1

            for ((y, m) in candidateMonths) {
                val dim = BsCalendarData.getDaysInMonth(y, m)
                val firstAd = bsToAd(y, m, 1)
                for (d in 1..dim) {
                    val ad = firstAd.plusDays((d - 1).toLong())
                    val tInfo = TithiCalculator.calculateTithiAtSunrise(ad)
                    if (tInfo.isAdhikMaas) continue

                    val sunriseMatches = (tInfo.number == targetTithi && !tInfo.isVriddhiSecondDay && tInfo.shuddhaLunarMonth == targetLunarMonth)
                    val aparahnaMatches = (tInfo.aparahnaTithiNumber == targetTithi && tInfo.shuddhaLunarMonth == targetLunarMonth)
                    val kshayaLunarMonth = if (tInfo.kshayaTithiNumber == 1 && tInfo.number == 30) {
                        (tInfo.shuddhaLunarMonth % 12) + 1
                    } else {
                        tInfo.shuddhaLunarMonth
                    }
                    val kshayaMatches = (tInfo.kshayaTithiNumber == targetTithi && kshayaLunarMonth == targetLunarMonth)

                    val score = when {
                        sunriseMatches && aparahnaMatches -> 4 // Both Sunrise (Udaya) & Aparahna match
                        sunriseMatches -> 3                    // Sunrise (Udaya) matches
                        kshayaMatches -> 2                     // Tithi Kshaya (skipped between sunrises)
                        aparahnaMatches -> 1                   // Afternoon-only match
                        else -> 0
                    }

                    if (score > bestScore) {
                        bestScore = score
                        bestDate = BsDate(y, m, d)
                    }
                }
            }

            if (bestDate != null && bestScore > 0) {
                return ResolvedBsOccurrence(
                    targetDate = bestDate,
                    wasAdjusted = (bestScore == 2),
                    adjustmentNote = if (bestScore == 2) "Tithi Kshaya on ${bestDate.formatIso()}" else null
                )
            }

            // Fallback within the event's stored BS month if no exact lunation match was found
            val daysInStoredMonth = BsCalendarData.getDaysInMonth(targetBsYear, event.bsMonth)
            val firstAdStored = bsToAd(targetBsYear, event.bsMonth, 1)
            val fallbackDay = (1..daysInStoredMonth).minByOrNull { d ->
                val ad = firstAdStored.plusDays((d - 1).toLong())
                val tSun = TithiCalculator.rawTithiAtSunrise(ad)
                val diff = abs(tSun - targetTithi)
                minOf(diff, 30 - diff) * 100 + abs(d - event.bsDay)
            } ?: 1

            return ResolvedBsOccurrence(
                targetDate = BsDate(targetBsYear, event.bsMonth, fallbackDay),
                wasAdjusted = true
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
