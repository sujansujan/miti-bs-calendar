package np.com.miti.patro

import np.com.miti.patro.domain.engine.BsCalendarData
import np.com.miti.patro.domain.engine.BsDateConverter
import np.com.miti.patro.domain.engine.TithiCalculator
import np.com.miti.patro.domain.model.BsDate
import np.com.miti.patro.domain.model.EventColor
import np.com.miti.patro.domain.model.MissingDayStrategy
import np.com.miti.patro.domain.model.Paksha
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BsCalendarTest {

    @Test
    fun epochRoundTrip_1970Baisakh1_is1913April13() {
        val epochBs = BsDate(1970, 1, 1)
        val ad = BsDateConverter.bsToAd(epochBs)
        assertEquals(LocalDate.of(1913, 4, 13), ad)
        assertEquals(epochBs, BsDateConverter.adToBs(ad))
    }

    @Test
    fun knownHistoricalAndModernDates_convertAccurately() {
        // 2072 Baisakh 12 BS = 2015-04-25 AD (Gorkha Earthquake day)
        val eqBs = BsDate(2072, 1, 12)
        val eqAd = LocalDate.of(2015, 4, 25)
        assertEquals(eqAd, BsDateConverter.bsToAd(eqBs))
        assertEquals(eqBs, BsDateConverter.adToBs(eqAd))

        // 2081 Baisakh 1 BS = 2024-04-13 AD (Nepali New Year 2081)
        val ny2081Bs = BsDate(2081, 1, 1)
        val ny2081Ad = LocalDate.of(2024, 4, 13)
        assertEquals(ny2081Ad, BsDateConverter.bsToAd(ny2081Bs))
        assertEquals(ny2081Bs, BsDateConverter.adToBs(ny2081Ad))

        // 2082 Baisakh 1 BS = 2025-04-14 AD (Nepali New Year 2082)
        val ny2082Bs = BsDate(2082, 1, 1)
        val ny2082Ad = LocalDate.of(2025, 4, 14)
        assertEquals(ny2082Ad, BsDateConverter.bsToAd(ny2082Bs))
        assertEquals(ny2082Bs, BsDateConverter.adToBs(ny2082Ad))
    }

    @Test
    fun fullTableRoundTrip_everyYearStartAndEndMatches() {
        for (year in BsCalendarData.MIN_YEAR..BsCalendarData.MAX_YEAR) {
            val firstDay = BsDate(year, 1, 1)
            val adFirst = BsDateConverter.bsToAd(firstDay)
            assertEquals("Failed start of year $year", firstDay, BsDateConverter.adToBs(adFirst))

            val chaitraLen = BsCalendarData.daysInMonth(year, 12)
            val lastDay = BsDate(year, 12, chaitraLen)
            val adLast = BsDateConverter.bsToAd(lastDay)
            assertEquals("Failed end of year $year", lastDay, BsDateConverter.adToBs(adLast))
        }
    }

    @Test
    fun annualBsRepeat_convertsToTrueShiftingAdDatesAcrossFutureYears() {
        // An event on Kartik 10 (month 7, day 10) in 2082, 2083, 2084 BS
        // must resolve to each year's true BS->AD date, not a fixed Gregorian day-of-month
        val occ2082 = BsDateConverter.resolveAnnualOccurrence(eventMonth = 7, eventDay = 10, targetBsYear = 2082, strategy = MissingDayStrategy.CLAMP_TO_LAST_DAY)!!
        val occ2083 = BsDateConverter.resolveAnnualOccurrence(eventMonth = 7, eventDay = 10, targetBsYear = 2083, strategy = MissingDayStrategy.CLAMP_TO_LAST_DAY)!!
        val occ2084 = BsDateConverter.resolveAnnualOccurrence(eventMonth = 7, eventDay = 10, targetBsYear = 2084, strategy = MissingDayStrategy.CLAMP_TO_LAST_DAY)!!

        val ad2082 = BsDateConverter.bsToAd(occ2082.resolvedDate)
        val ad2083 = BsDateConverter.bsToAd(occ2083.resolvedDate)
        val ad2084 = BsDateConverter.bsToAd(occ2084.resolvedDate)

        assertEquals(BsDate(2082, 7, 10), BsDateConverter.adToBs(ad2082))
        assertEquals(BsDate(2083, 7, 10), BsDateConverter.adToBs(ad2083))
        assertEquals(BsDate(2084, 7, 10), BsDateConverter.adToBs(ad2084))
        // Across leap/non-leap BS years, the AD day-of-month shifts rather than staying locked
        assertTrue(ad2082.dayOfMonth != ad2084.dayOfMonth || ad2082.dayOfMonth != ad2083.dayOfMonth)
    }

    @Test
    fun missingDayStrategies_handleVariableMonthLengthsCorrectly() {
        // In 2081 BS, Baisakh (month 1) has 31 days. Suppose an event is created on Baisakh 32.
        val clampRes = BsDateConverter.resolveAnnualOccurrence(
            targetBsYear = 2081,
            eventMonth = 1,
            eventDay = 32,
            strategy = MissingDayStrategy.LAST_DAY_OF_MONTH
        )
        assertNotNull(clampRes)
        assertEquals(BsDate(2081, 1, 31), clampRes!!.resolvedDate)
        assertTrue(clampRes.wasAdjusted)

        val rollRes = BsDateConverter.resolveAnnualOccurrence(
            targetBsYear = 2081,
            eventMonth = 1,
            eventDay = 32,
            strategy = MissingDayStrategy.NEXT_MONTH_FIRST_DAY
        )
        assertNotNull(rollRes)
        assertEquals(BsDate(2081, 2, 1), rollRes!!.resolvedDate)
        assertTrue(rollRes.wasAdjusted)

        val skipRes = BsDateConverter.resolveAnnualOccurrence(
            targetBsYear = 2081,
            eventMonth = 1,
            eventDay = 32,
            strategy = MissingDayStrategy.SKIP_YEAR
        )
        assertNull(skipRes)
    }

    @Test
    fun eventColor_fromKeyResovesAllPaletteSwatches() {
        for (c in EventColor.values()) {
            assertEquals(c, EventColor.fromKey(c.key))
        }
        assertEquals(EventColor.GREEN, EventColor.fromKey("UNKNOWN_COLOR"))
    }

    @Test
    fun tithiCalculator_identifiesFullMoonAndNewMoonAccurately() {
        val buddhaJayanti2024 = LocalDate.of(2024, 5, 23)
        val purnimaTithi = TithiCalculator.calculateTithiAtSunrise(buddhaJayanti2024)
        assertEquals(Paksha.SHUKLA, purnimaTithi.paksha)
        assertTrue(
            "Expected Purnima (15) or Chaturdashi (14) at sunrise, got ${purnimaTithi.number}",
            purnimaTithi.number in 14..15
        )

        val laxmiPuja2024 = LocalDate.of(2024, 11, 1)
        val aunsiTithi = TithiCalculator.calculateTithiAtSunrise(laxmiPuja2024)
        assertEquals(Paksha.KRISHNA, aunsiTithi.paksha)
        assertTrue(
            "Expected Aunsi (30) or Chaturdashi (29) at sunrise, got ${aunsiTithi.number}",
            aunsiTithi.number in 29..30
        )
    }
}
