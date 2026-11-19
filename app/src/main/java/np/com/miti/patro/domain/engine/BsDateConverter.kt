package np.com.miti.patro.domain.engine

import np.com.miti.patro.data.local.EventEntity
import np.com.miti.patro.domain.model.BsDate
import np.com.miti.patro.domain.model.MissingDayStrategy
import np.com.miti.patro.domain.model.ResolvedBsOccurrence
import java.time.LocalDate
import java.time.temporal.ChronoUnit

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
     * derived from the event's origin BS date.
     */
    fun getShraddhaLunarMonth(event: EventEntity): Int {
        val safeYear = event.startBsYear.coerceIn(BsCalendarData.MIN_YEAR, BsCalendarData.MAX_YEAR)
        val safeMonth = event.bsMonth.coerceIn(1, 12)
        val maxDay = BsCalendarData.getDaysInMonth(safeYear, safeMonth)
        val originBs = BsDate(
            year = safeYear,
            month = safeMonth,
            day = event.bsDay.coerceIn(1, maxDay)
        )
        val originAd = bsToAd(originBs)
        return TithiCalculator.getLunationDetails(originAd).shuddhaLunarMonth
    }

    /**
     * Resolves an annual BS event OR an annual Lunar Shraddha Tithi event for [targetBsYear].
     * For Shraddha events, returns the first matching occurrence in [targetBsYear].
     * Use [resolveEventOccurrencesInBsYear] when a lunar month (such as Chaitra Amanta)
     * occurs twice within a single 365-day BS year or 0 times in an Adhik Maas year.
     */
    fun resolveEventOccurrence(
        event: EventEntity,
        targetBsYear: Int,
        strategy: MissingDayStrategy
    ): ResolvedBsOccurrence? {
        return resolveEventOccurrencesInBsYear(event, targetBsYear, strategy).firstOrNull()
    }

    /**
     * Resolves all occurrences of an annual BS event or Shraddha Tithi event inside [targetBsYear].
     *
     * For Shraddha events:
     * 1. Identifies all distinct Shuddha lunations matching `targetLunarMonth` (including Kshaya Maas)
     *    that overlap [targetBsYear].
     * 2. Evaluates each lunation across its full ~30-day span using Aparahna-vyapini priority
     *    (4th fifth of daytime in Kathmandu), then Udaya/Kshaya fallback within that same lunation.
     * 3. Returns occurrences falling strictly inside [targetBsYear], ordered chronologically.
     */
    fun resolveEventOccurrencesInBsYear(
        event: EventEntity,
        targetBsYear: Int,
        strategy: MissingDayStrategy
    ): List<ResolvedBsOccurrence> {
        if (targetBsYear !in BsCalendarData.MIN_YEAR..BsCalendarData.MAX_YEAR) return emptyList()

        if (event.isShraddha && event.shraddhaTithi in 1..30) {
            val targetLunarMonth = getShraddhaLunarMonth(event)
            return resolveShraddhaOccurrences(
                targetLunarMonth = targetLunarMonth,
                targetTithi = event.shraddhaTithi,
                targetBsYear = targetBsYear
            )
        }

        return listOfNotNull(
            resolveRepeatingDate(
                originMonth = event.bsMonth,
                originDay = event.bsDay,
                targetBsYear = targetBsYear,
                strategy = strategy
            )
        )
    }

    private fun resolveShraddhaOccurrences(
        targetLunarMonth: Int,
        targetTithi: Int,
        targetBsYear: Int
    ): List<ResolvedBsOccurrence> {
        val candidateMonths = when (targetLunarMonth) {
            12 -> listOf(1, 11, 12)
            1 -> listOf(1, 2, 12)
            else -> listOf(targetLunarMonth - 1, targetLunarMonth, (targetLunarMonth % 12) + 1)
                .distinct()
                .sorted()
        }

        val lunationEndDates = LinkedHashMap<Long, LocalDate>()
        for (m in candidateMonths) {
            val dim = BsCalendarData.getDaysInMonth(targetBsYear, m)
            val firstAd = bsToAd(targetBsYear, m, 1)
            for (d in 1..dim) {
                val ad = firstAd.plusDays((d - 1).toLong())
                val lun = TithiCalculator.getLunationDetails(ad)
                if (!lun.isAdhikMaas && (lun.shuddhaLunarMonth == targetLunarMonth || lun.kshayaLunarMonth == targetLunarMonth)) {
                    lunationEndDates.putIfAbsent(lun.endAunsiEpochDay, LocalDate.ofEpochDay(lun.endAunsiEpochDay))
                }
                val nextLun = TithiCalculator.getLunationDetails(ad.plusDays(1))
                if (!nextLun.isAdhikMaas && (nextLun.shuddhaLunarMonth == targetLunarMonth || nextLun.kshayaLunarMonth == targetLunarMonth)) {
                    lunationEndDates.putIfAbsent(nextLun.endAunsiEpochDay, LocalDate.ofEpochDay(nextLun.endAunsiEpochDay))
                }
            }
        }

        val results = mutableListOf<ResolvedBsOccurrence>()
        for (endEpochDay in lunationEndDates.keys.sorted()) {
            val endAd = lunationEndDates[endEpochDay] ?: continue
            var bestAd: LocalDate? = null
            var bestScore = -1
            var bestIsSunrise = false
            var bestIsKshaya = false

            for (off in -31..0) {
                val ad = endAd.plusDays(off.toLong())
                val tInfo = TithiCalculator.calculateTithiAtSunrise(ad)
                val curOrd = TithiCalculator.getLunationDetails(ad).endAunsiEpochDay
                val nextOrd = TithiCalculator.getLunationDetails(ad.plusDays(1)).endAunsiEpochDay
                val (apStart, apMid, apEnd) = TithiCalculator.kathmanduAparahnaWindowNst(ad)

                fun tithiMatchesAt(nstHour: Double): Boolean {
                    val tn = TithiCalculator.rawTithiAtNstHour(ad, nstHour)
                    if (tn != targetTithi) return false
                    return if (tInfo.number in 29..30 && tn in 1..2) {
                        nextOrd == endEpochDay
                    } else {
                        curOrd == endEpochDay
                    }
                }

                val apStartM = tithiMatchesAt(apStart)
                val apMidM = tithiMatchesAt(apMid)
                val apEndM = tithiMatchesAt(apEnd)
                val srM = (curOrd == endEpochDay && tInfo.number == targetTithi && !tInfo.isVriddhiSecondDay)
                val ksM = (tInfo.kshayaTithiNumber == targetTithi &&
                    (if (tInfo.number == 30 && tInfo.kshayaTithiNumber == 1) nextOrd == endEpochDay else curOrd == endEpochDay))

                val score = when {
                    apStartM && apMidM && apEndM -> if (srM) 6 else 5
                    apMidM -> if (apStartM || apEndM) 4 else 3
                    apStartM || apEndM -> 2
                    srM || ksM -> 1
                    else -> 0
                }

                if (score > bestScore) {
                    bestScore = score
                    bestAd = ad
                    bestIsSunrise = srM
                    bestIsKshaya = ksM
                }
            }

            if (bestAd != null && bestScore > 0) {
                val bs = adToBs(bestAd)
                if (bs.year == targetBsYear && bsToAd(bs) == bestAd) {
                    val wasAdj = (bestScore == 1 && !bestIsSunrise)
                    val note = if (bestScore == 1 && bestIsKshaya) "Tithi Kshaya on ${bs.formatIso()}" else null
                    results.add(
                        ResolvedBsOccurrence(
                            targetDate = bs,
                            wasAdjusted = wasAdj,
                            adjustmentNote = note
                        )
                    )
                }
            }
        }

        return results.sortedBy { it.targetDate }
    }

    /**
     * Finds a valid non-Adhik BS anchor date whose [getShraddhaLunarMonth] equals [targetAmantaMonth],
     * preferring the resolved occurrence of [targetTithi] in or near [preferredBsYear].
     */
    fun findAnchorDateForShraddha(
        targetAmantaMonth: Int,
        targetTithi: Int,
        preferredBsYear: Int
    ): BsDate {
        val safeMonth = targetAmantaMonth.coerceIn(1, 12)
        val safeTithi = targetTithi.coerceIn(1, 30)
        val safeYear = preferredBsYear.coerceIn(BsCalendarData.MIN_YEAR, BsCalendarData.MAX_YEAR)
        val searchYears = listOf(safeYear, safeYear + 1, safeYear - 1)
            .filter { it in BsCalendarData.MIN_YEAR..BsCalendarData.MAX_YEAR }
            .distinct()

        for (y in searchYears) {
            val occurrences = resolveShraddhaOccurrences(safeMonth, safeTithi, y)
            for (occ in occurrences) {
                val candidate = occ.targetDate
                val candAd = bsToAd(candidate)
                val lun = TithiCalculator.getLunationDetails(candAd)
                if (!lun.isAdhikMaas && lun.shuddhaLunarMonth == safeMonth) {
                    return candidate
                }
                val nextAd = candAd.plusDays(1)
                val nextLun = TithiCalculator.getLunationDetails(nextAd)
                if (!nextLun.isAdhikMaas && nextLun.shuddhaLunarMonth == safeMonth) {
                    val nextBs = adToBs(nextAd)
                    if (bsToAd(nextBs) == nextAd) {
                        return nextBs
                    }
                }
            }
        }

        val fallbackMonths = listOf(safeMonth, (safeMonth % 12) + 1)
        for (y in searchYears) {
            for (m in fallbackMonths) {
                val dim = BsCalendarData.getDaysInMonth(y, m)
                val firstAd = bsToAd(y, m, 1)
                for (d in 1..dim) {
                    val lun = TithiCalculator.getLunationDetails(firstAd.plusDays((d - 1).toLong()))
                    if (!lun.isAdhikMaas && lun.shuddhaLunarMonth == safeMonth) {
                        return BsDate(y, m, d)
                    }
                }
            }
        }

        return BsDate(safeYear, safeMonth, 1)
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
