package np.com.miti.patro

import np.com.miti.patro.domain.engine.BsCalendarData
import np.com.miti.patro.domain.engine.BsDateConverter
import np.com.miti.patro.domain.engine.NepaliFestivals
import np.com.miti.patro.domain.engine.TithiCalculator
import np.com.miti.patro.domain.model.BsDate
import np.com.miti.patro.domain.model.MissingDayStrategy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class BsCalendarTest {

    @Test
    fun anchorDateAndKnownNewYearsMatchOfficialPatro() {
        // Base anchor: 2000 Baisakh 1 = 1943-04-14
        assertEquals(LocalDate.of(1943, 4, 14), BsDateConverter.bsToAd(2000, 1, 1))
        assertEquals(BsDate(2000, 1, 1), BsDateConverter.adToBs(LocalDate.of(1943, 4, 14)))

        // Recent & upcoming BS New Years
        assertEquals(LocalDate.of(2023, 4, 14), BsDateConverter.bsToAd(2080, 1, 1))
        assertEquals(LocalDate.of(2024, 4, 13), BsDateConverter.bsToAd(2081, 1, 1))
        assertEquals(LocalDate.of(2025, 4, 14), BsDateConverter.bsToAd(2082, 1, 1))
        assertEquals(LocalDate.of(2026, 4, 14), BsDateConverter.bsToAd(2083, 1, 1))
        assertEquals(LocalDate.of(2027, 4, 14), BsDateConverter.bsToAd(2084, 1, 1))
    }

    @Test
    fun verified2083DatesAndWeekdaysMatchPanchang() {
        // 2083 Kartik 9 (Today in reference metadata) = 2026-10-26 (Monday)
        val kartik9 = BsDate(2083, 7, 9)
        val adKartik9 = BsDateConverter.bsToAd(kartik9)
        assertEquals(LocalDate.of(2026, 10, 26), adKartik9)
        assertEquals(DayOfWeek.MONDAY, adKartik9.dayOfWeek)
        assertEquals(kartik9, BsDateConverter.adToBs(adKartik9))

        // 2083 Kartik 4 (Vijaya Dashami) = 2026-10-21 (Wednesday)
        assertEquals(LocalDate.of(2026, 10, 21), BsDateConverter.bsToAd(2083, 7, 4))

        // 2083 Kartik 22 (Laxmi Puja) = 2026-11-08 (Sunday)
        assertEquals(LocalDate.of(2026, 11, 8), BsDateConverter.bsToAd(2083, 7, 22))

        // 2083 Magh 1 (Maghe Sankranti) = 2027-01-15 (Friday)
        assertEquals(LocalDate.of(2027, 1, 15), BsDateConverter.bsToAd(2083, 10, 1))
    }

    @Test
    fun roundTripConversionAcrossMultipleYearsIsLossless() {
        var cursor = LocalDate.of(2020, 1, 1)
        val end = LocalDate.of(2030, 12, 31)
        while (!cursor.isAfter(end)) {
            val bs = BsDateConverter.adToBs(cursor)
            val roundTripAd = BsDateConverter.bsToAd(bs)
            assertEquals("Mismatch on $cursor -> $bs", cursor, roundTripAd)
            cursor = cursor.plusDays(1)
        }
    }

    @Test
    fun missingDayStrategiesHandle32DayMonthVariations() {
        // In 2080 BS, Jestha (month 2) has 32 days.
        // In 2083 BS, Jestha (month 2) has 31 days.
        assertEquals(32, BsCalendarData.getDaysInMonth(2080, 2))
        assertEquals(31, BsCalendarData.getDaysInMonth(2083, 2))

        // 1. Clamp strategy: Jestha 32 clamps to Jestha 31 in 2083 BS
        val clamped = BsDateConverter.resolveAnnualOccurrence(
            eventMonth = 2,
            eventDay = 32,
            targetBsYear = 2083,
            strategy = MissingDayStrategy.CLAMP_TO_LAST_DAY
        )
        assertNotNull(clamped)
        assertEquals(BsDate(2083, 2, 31), clamped!!.targetDate)
        assertTrue(clamped.isClampedOrRolled)

        // 2. Roll strategy: Jestha 32 rolls to Ashadh 1 in 2083 BS
        val rolled = BsDateConverter.resolveAnnualOccurrence(
            eventMonth = 2,
            eventDay = 32,
            targetBsYear = 2083,
            strategy = MissingDayStrategy.ROLL_TO_NEXT_MONTH
        )
        assertNotNull(rolled)
        assertEquals(BsDate(2083, 3, 1), rolled!!.targetDate)
        assertTrue(rolled.isClampedOrRolled)

        // 3. Skip strategy: Jestha 32 returns null in 2083 BS, but succeeds in 2080 BS
        val skipped = BsDateConverter.resolveAnnualOccurrence(
            eventMonth = 2,
            eventDay = 32,
            targetBsYear = 2083,
            strategy = MissingDayStrategy.SKIP_YEAR
        )
        assertNull(skipped)

        val exactIn2080 = BsDateConverter.resolveAnnualOccurrence(
            eventMonth = 2,
            eventDay = 32,
            targetBsYear = 2080,
            strategy = MissingDayStrategy.SKIP_YEAR
        )
        assertNotNull(exactIn2080)
        assertEquals(BsDate(2080, 2, 32), exactIn2080!!.targetDate)
    }

    @Test
    fun tithiAndFestivalCalculationsMatchKnownFullAndNewMoons() {
        // Buddha Jayanti 2083 (2083-01-18 = 2026-05-01) is Purnima
        val buddhaJayantiDate = BsDate(2083, 1, 18)
        val tithiBuddha = TithiCalculator.calculateTithi(buddhaJayantiDate)
        assertTrue(tithiBuddha.isPurnima)
        assertEquals("Purnima", tithiBuddha.englishName)

        // 2083 Kartik 9 (2026-10-26) is Purnima
        val kartik9 = BsDate(2083, 7, 9)
        val tithiKartik9 = TithiCalculator.calculateTithi(kartik9)
        assertTrue(tithiKartik9.isPurnima)

        // Laxmi Puja 2083 (2083-07-22 = 2026-11-08) is Aunsi
        val laxmiPujaDate = BsDate(2083, 7, 22)
        val tithiLaxmi = TithiCalculator.calculateTithi(laxmiPujaDate)
        assertTrue(tithiLaxmi.isAunsi)
        assertEquals("Aunsi", tithiLaxmi.englishName)

        val festivals = NepaliFestivals.getFestivalsForDay(laxmiPujaDate, tithiLaxmi)
        assertTrue(festivals.any { it.nameEn.contains("Laxmi Puja") })
    }
}
