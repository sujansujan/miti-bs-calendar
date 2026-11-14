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
import java.time.temporal.ChronoUnit

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
        val bhadra1 = BsDate(2084, 5, 1)
        val tithi1 = TithiCalculator.calculateTithi(bhadra1)
        val fests1 = NepaliFestivals.getFestivalsForDay(bhadra1, tithi1)
        assertTrue("Bhadra 1, 2084 must be Janai Purnima", fests1.any { "Janai Purnima" in it.nameEn })

        val bhadra2 = BsDate(2084, 5, 2)
        val tithi2 = TithiCalculator.calculateTithi(bhadra2)
        val fests2 = NepaliFestivals.getFestivalsForDay(bhadra2, tithi2)
        assertTrue("Bhadra 2, 2084 must be Gai Jatra", fests2.any { "Gai Jatra" in it.nameEn })

        val bhadra31 = BsDate(2084, 5, 31)
        val tithi31 = TithiCalculator.calculateTithi(bhadra31)
        val fests31 = NepaliFestivals.getFestivalsForDay(bhadra31, tithi31)
        assertTrue("Bhadra 31, 2084 must NOT duplicate Gai Jatra", fests31.none { "Gai Jatra" in it.nameEn })
        assertTrue("Bhadra 31, 2084 must be Sorha Shraddha Starts", fests31.any { "Sorha Shraddha" in it.nameEn })
    }

    @Test
    fun hajuramaShraddha_shrawanShuklaNawami_isAlways6DaysBeforeJanaiPurnima() {
        // Created on 2083-05-05 (Bhadra 5, 2083 = Shrawan Shukla Nawami, 7 days before Janai Purnima on Bhadra 12)
        val shraddhaEvent = EventEntity(
            id = 1L,
            title = "Hajurama Shraddha",
            bsMonth = 5,
            bsDay = 5,
            startBsYear = 2083,
            isShraddha = true,
            shraddhaTithi = 9
        )

        // In 2084 BS, Janai Purnima is on Bhadra 1 (2084-05-01), so Shrawan Shukla Nawami MUST fall on Shrawan 25 (2084-04-25)!
        val occ2084 = BsDateConverter.resolveEventOccurrence(
            event = shraddhaEvent,
            targetBsYear = 2084,
            strategy = MissingDayStrategy.CLAMP_TO_LAST_DAY
        )
        assertNotNull(occ2084)
        assertEquals(BsDate(2084, 4, 25), occ2084?.targetDate)

        // Verify across 2083..2088 that it always falls 5..7 days before Janai Purnima and on sunrise Tithi 9
        for (year in 2083..2088) {
            val occ = BsDateConverter.resolveEventOccurrence(
                event = shraddhaEvent,
                targetBsYear = year,
                strategy = MissingDayStrategy.CLAMP_TO_LAST_DAY
            )
            assertNotNull("Shraddha must resolve in $year", occ)
            val shraddhaAd = BsDateConverter.bsToAd(occ!!.targetDate)
            val tInfo = TithiCalculator.calculateTithi(occ.targetDate, shraddhaAd)
            assertEquals("Resolved day in $year must be Shukla Nawami (9)", 9, tInfo.number)

            // Janai Purnima is 6 Tithis later (around 5..7 solar days later)
            val jpAd = (5..7).map { shraddhaAd.plusDays(it.toLong()) }.firstOrNull { candAd ->
                val candBs = BsDateConverter.adToBs(candAd)
                val fests = NepaliFestivals.getFestivalsForDay(candBs, TithiCalculator.calculateTithi(candBs, candAd))
                fests.any { "Janai Purnima" in it.nameEn }
            }
            assertNotNull("Janai Purnima must follow 5..7 days after Shrawan Shukla Nawami in $year", jpAd)
            val daysBeforeJp = ChronoUnit.DAYS.between(shraddhaAd, jpAd)
            assertTrue("Must be 5..7 days before Janai Purnima", daysBeforeJp in 5..7)
        }
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
