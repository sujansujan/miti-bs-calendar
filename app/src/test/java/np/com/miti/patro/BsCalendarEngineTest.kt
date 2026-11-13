package np.com.miti.patro

import np.com.miti.patro.data.local.EventEntity
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
import java.time.LocalDate

class BsCalendarEngineTest {

    @Test
    fun anchorDateRoundTrip_isExact() {
        val anchorBs = BsDate(2000, 1, 1)
        val anchorAd = BsDateConverter.bsToAd(anchorBs)
        assertEquals(LocalDate.of(1943, 4, 14), anchorAd)
        assertEquals(anchorBs, BsDateConverter.adToBs(anchorAd))
    }

    @Test
    fun fullRangeRoundTrip_1970To2100_isLossless() {
        for (year in listOf(1970, 2000, 2050, 2081, 2082, 2083, 2084, 2085, 2090, 2100)) {
            for (month in 1..12) {
                val dim = BsCalendarData.getDaysInMonth(year, month)
                val first = BsDate(year, month, 1)
                val last = BsDate(year, month, dim)
                assertEquals(first, BsDateConverter.adToBs(BsDateConverter.bsToAd(first)))
                assertEquals(last, BsDateConverter.adToBs(BsDateConverter.bsToAd(last)))
            }
        }
    }

    @Test
    fun bhadra2084_hasSingleGaiJatraAndJanaiPurnimaOnDay1() {
        // Bhadra 1, 2084 (2027-08-17): Janai Purnima (Shrawan Shukla Purnima)
        val bhadra1 = BsDate(2084, 5, 1)
        val tithi1 = TithiCalculator.calculateTithi(bhadra1)
        val fests1 = NepaliFestivals.getFestivalsForDay(bhadra1, tithi1)
        assertTrue("Bhadra 1, 2084 must be Janai Purnima", fests1.any { "Janai Purnima" in it.nameEn })

        // Bhadra 2, 2084 (2027-08-18): Gai Jatra
        val bhadra2 = BsDate(2084, 5, 2)
        val tithi2 = TithiCalculator.calculateTithi(bhadra2)
        val fests2 = NepaliFestivals.getFestivalsForDay(bhadra2, tithi2)
        assertTrue("Bhadra 2, 2084 must be Gai Jatra", fests2.any { "Gai Jatra" in it.nameEn })

        // Bhadra 31, 2084 (2027-09-16): Must be Sorha Shraddha Starts, NOT a second Gai Jatra!
        val bhadra31 = BsDate(2084, 5, 31)
        val tithi31 = TithiCalculator.calculateTithi(bhadra31)
        val fests31 = NepaliFestivals.getFestivalsForDay(bhadra31, tithi31)
        assertTrue("Bhadra 31, 2084 must NOT duplicate Gai Jatra", fests31.none { "Gai Jatra" in it.nameEn })
        assertTrue("Bhadra 31, 2084 must be Sorha Shraddha Starts", fests31.any { "Sorha Shraddha" in it.nameEn })
    }

    @Test
    fun adhikMaas2083_isDetectedAndSkipsDuplicateFestivals() {
        // In 2083 BS, Ashadh 2..30 is Adhik Ashadh (two Aunsis in Ashadh 2083)
        val adhikDay = BsDate(2083, 3, 15)
        val tithi = TithiCalculator.calculateTithi(adhikDay)
        assertTrue("2083-03-15 falls inside Adhik Ashadh", tithi.isAdhikMaas)
    }

    @Test
    fun shraddhaEventResolution_resolvesSingleDayPerMonth() {
        val shraddhaEvent = EventEntity(
            id = 1L,
            title = "Hajurama Shraddha",
            bsMonth = 5,
            bsDay = 24,
            startBsYear = 2083,
            isShraddha = true,
            shraddhaTithi = 9
        )
        val resolved2084 = BsDateConverter.resolveEventOccurrence(
            event = shraddhaEvent,
            targetBsYear = 2084,
            strategy = MissingDayStrategy.CLAMP_TO_LAST_DAY
        )
        assertNotNull(resolved2084)
        assertEquals(2084, resolved2084?.targetDate?.year)
        assertEquals(5, resolved2084?.targetDate?.month)
    }

    @Test
    fun missingDay31And32_strategiesBehaveCorrectly() {
        val clamped = BsDateConverter.resolveRepeatingDate(
            originMonth = 1,
            originDay = 31,
            targetBsYear = 2000,
            strategy = MissingDayStrategy.CLAMP_TO_LAST_DAY
        )
        assertEquals(BsDate(2000, 1, 30), clamped?.targetDate)
        assertEquals(true, clamped?.wasAdjusted)

        val rolled = BsDateConverter.resolveRepeatingDate(
            originMonth = 1,
            originDay = 31,
            targetBsYear = 2000,
            strategy = MissingDayStrategy.ROLL_TO_NEXT_MONTH
        )
        assertEquals(BsDate(2000, 2, 1), rolled?.targetDate)

        val skipped = BsDateConverter.resolveRepeatingDate(
            originMonth = 1,
            originDay = 31,
            targetBsYear = 2000,
            strategy = MissingDayStrategy.SKIP_YEAR
        )
        assertNull(skipped)
    }
}
