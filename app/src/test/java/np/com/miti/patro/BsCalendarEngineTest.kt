package np.com.miti.patro

import np.com.miti.patro.data.local.EventEntity
import np.com.miti.patro.domain.engine.BsCalendarData
import np.com.miti.patro.domain.engine.BsDateConverter
import np.com.miti.patro.domain.engine.NepaliFestivals
import np.com.miti.patro.domain.engine.TithiCalculator
import np.com.miti.patro.domain.model.BsDate
import np.com.miti.patro.domain.model.MissingDayStrategy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

        // Verify across 2083..2088 that it always falls 5..7 days before Janai Purnima and on Aparahna Tithi 9
        for (year in 2083..2088) {
            val occ = BsDateConverter.resolveEventOccurrence(
                event = shraddhaEvent,
                targetBsYear = year,
                strategy = MissingDayStrategy.CLAMP_TO_LAST_DAY
            )
            assertNotNull("Shraddha must resolve in $year", occ)
            val shraddhaAd = BsDateConverter.bsToAd(occ!!.targetDate)
            val tInfo = TithiCalculator.calculateTithi(occ.targetDate, shraddhaAd)
            assertEquals("Resolved day in $year must have Aparahna Shukla Nawami (9)", 9, tInfo.aparahnaTithiNumber)

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

    @Test
    fun adhikMaas_2080_2083_2085_areAccurateAndNotSwapped() {
        // BS 2080 Adhik Shrawan
        val adhik2080 = TithiCalculator.calculateTithi(BsDate(2080, 4, 15))
        assertEquals(4, adhik2080.shuddhaLunarMonth)
        assertTrue("2080-04-15 must be Adhik Shrawan", adhik2080.isAdhikMaas)

        // BS 2083 Adhik Jestha (2083-02-03..2083-03-01) vs Shuddha Jestha (2083-03-06 Sithi Nakha)
        val adhik2083 = TithiCalculator.calculateTithi(BsDate(2083, 2, 15))
        assertEquals(2, adhik2083.shuddhaLunarMonth)
        assertTrue("2083-02-15 must be Adhik Jestha", adhik2083.isAdhikMaas)

        val shuddha2083 = TithiCalculator.calculateTithi(BsDate(2083, 3, 6))
        assertEquals(2, shuddha2083.shuddhaLunarMonth)
        assertFalse("2083-03-06 must be Shuddha Jestha", shuddha2083.isAdhikMaas)

        // BS 2085-2086 Adhik Chaitra (2085-12-03..2086-01-01) vs Shuddha Chaitra (2086-01-05)
        val adhik2085 = TithiCalculator.calculateTithi(BsDate(2085, 12, 15))
        assertEquals(12, adhik2085.shuddhaLunarMonth)
        assertTrue("2085-12-15 must be Adhik Chaitra", adhik2085.isAdhikMaas)

        val adhik2086End = TithiCalculator.calculateTithi(BsDate(2086, 1, 1))
        assertEquals(12, adhik2086End.shuddhaLunarMonth)
        assertTrue("2086-01-01 must still be Adhik Chaitra", adhik2086End.isAdhikMaas)

        val shuddha2086 = TithiCalculator.calculateTithi(BsDate(2086, 1, 5))
        assertEquals(12, shuddha2086.shuddhaLunarMonth)
        assertFalse("2086-01-05 must be Shuddha Chaitra", shuddha2086.isAdhikMaas)
    }

    @Test
    fun boundaryYears1970And2100_noFalseAdhikMaas() {
        for (d in listOf(BsDate(1970, 1, 1), BsDate(1970, 1, 15), BsDate(2100, 12, 30))) {
            val t = TithiCalculator.calculateTithi(d)
            assertFalse("$d must not be flagged as Adhik Maas", t.isAdhikMaas)
        }
    }

    @Test
    fun allShuddhaLunarMonthsPresent_includingKshayaMaasYears() {
        for (year in listOf(2001, 2020, 2039, 2058)) {
            val seenMonths = mutableSetOf<Int>()
            for (m in 1..12) {
                val dim = BsCalendarData.getDaysInMonth(year, m)
                val firstAd = BsDateConverter.bsToAd(year, m, 1)
                for (d in 1..dim) {
                    val lun = TithiCalculator.getLunationDetails(firstAd.plusDays((d - 1).toLong()))
                    if (!lun.isAdhikMaas) {
                        seenMonths.add(lun.shuddhaLunarMonth)
                        lun.kshayaLunarMonth?.let { seenMonths.add(it) }
                    }
                }
            }
            assertEquals("All 12 lunar months must occur in BS $year", (1..12).toSet(), seenMonths)
        }

        // BS 2039 must not drop Ghatasthapana across the Adhik Bhadra/Ashwin boundary
        var ghatasthapanaFound = false
        for (m in 6..7) {
            val dim = BsCalendarData.getDaysInMonth(2039, m)
            for (d in 1..dim) {
                val bs = BsDate(2039, m, d)
                val t = TithiCalculator.calculateTithi(bs)
                if (NepaliFestivals.getFestivalsForDay(bs, t).any { "Ghatasthapana" in it.nameEn }) {
                    ghatasthapanaFound = true
                }
            }
        }
        assertTrue("Ghatasthapana must occur in BS 2039", ghatasthapanaFound)
    }

    @Test
    fun bs2083VerifiedFestivals_andGregorianFixedHolidays_areAccurate() {
        fun namesAt(y: Int, m: Int, d: Int): List<String> {
            val bs = BsDate(y, m, d)
            return NepaliFestivals.getFestivalsForDay(bs, TithiCalculator.calculateTithi(bs)).map { it.nameEn }
        }

        assertTrue(namesAt(2083, 1, 4).any { "Mata Tirtha Aunsi" in it })
        assertTrue(namesAt(2083, 1, 7).any { "Akshaya Tritiya" in it })
        assertTrue(namesAt(2083, 2, 3).any { "Adhik Maas" in it })
        assertTrue(namesAt(2083, 3, 6).any { "Sithi Nakha" in it })
        assertTrue(namesAt(2083, 4, 9).any { "Harishayani" in it })
        assertTrue(namesAt(2083, 4, 13).any { "Guru Purnima" in it })
        assertTrue(namesAt(2083, 5, 1).any { "Nag Panchami" in it })
        assertTrue(namesAt(2083, 8, 5).any { "Haribodhini" in it })
        assertTrue(namesAt(2083, 8, 28).any { "Vivaha Panchami" in it })

        // May 1, 2024 falls on Baisakh 19, 2081 BS (NOT Baisakh 18)
        assertTrue(namesAt(2081, 1, 19).any { "Labour Day" in it })
        assertTrue(namesAt(2081, 1, 18).none { "Labour Day" in it })

        // March 8, 2028 falls on Falgun 25, 2084 BS
        assertTrue(namesAt(2084, 11, 25).any { "Women's Day" in it })
    }

    @Test
    fun pradoshAndNishitaFestivals_matchPanchanga() {
        fun namesAt(y: Int, m: Int, d: Int): List<String> {
            val bs = BsDate(y, m, d)
            return NepaliFestivals.getFestivalsForDay(bs, TithiCalculator.calculateTithi(bs)).map { it.nameEn }
        }

        // BS 2081 evening/midnight festivals
        assertTrue(namesAt(2081, 5, 10).any { "Janmashtami" in it })
        assertTrue(namesAt(2081, 7, 15).any { "Laxmi Puja" in it })
        assertTrue(namesAt(2081, 11, 14).any { "Maha Shivaratri" in it })
        assertTrue(namesAt(2081, 11, 29).any { "Holi - Hills" in it })
        assertTrue(namesAt(2081, 12, 1).any { "Holi - Terai" in it })

        // BS 2084 evening/midnight festivals
        assertTrue(namesAt(2084, 5, 8).any { "Janmashtami" in it })
        assertTrue(namesAt(2084, 11, 11).any { "Maha Shivaratri" in it })
    }

    @Test
    fun chaitraAmantaShraddha_multiOccurrenceAndZeroBrokenGaps() {
        val chaitraAshtami = EventEntity(
            id = 2L,
            title = "Chaitra Shukla Ashtami Shraddha",
            bsMonth = 12,
            bsDay = 11,
            startBsYear = 2082,
            isShraddha = true,
            shraddhaTithi = 8
        )

        val occ2083 = BsDateConverter.resolveEventOccurrencesInBsYear(
            chaitraAshtami, 2083, MissingDayStrategy.CLAMP_TO_LAST_DAY
        )
        assertTrue("Chaitra Amanta cannot occur in Adhik BS 2083", occ2083.isEmpty())

        val occ2084 = BsDateConverter.resolveEventOccurrencesInBsYear(
            chaitraAshtami, 2084, MissingDayStrategy.CLAMP_TO_LAST_DAY
        )
        assertEquals(2, occ2084.size)
        assertEquals(BsDate(2084, 1, 1), occ2084[0].targetDate)
        assertEquals(BsDate(2084, 12, 20), occ2084[1].targetDate)

        val allAdDates = mutableListOf<LocalDate>()
        for (y in 2082..2091) {
            for (occ in BsDateConverter.resolveEventOccurrencesInBsYear(chaitraAshtami, y, MissingDayStrategy.CLAMP_TO_LAST_DAY)) {
                allAdDates.add(BsDateConverter.bsToAd(occ.targetDate))
            }
        }
        assertEquals(9, allAdDates.size)
        for (i in 1 until allAdDates.size) {
            val gap = ChronoUnit.DAYS.between(allAdDates[i - 1], allAdDates[i])
            assertTrue("Lunar year gap $gap out of bounds [350..390]", gap in 350..390)
        }

        // Chaitra Shukla Pratipada (lm=12, t=1) at end of BS 2039
        val chaitraPratipada = EventEntity(
            id = 3L,
            title = "Chaitra Pratipada Shraddha",
            bsMonth = 12,
            bsDay = 13,
            startBsYear = 2038,
            isShraddha = true,
            shraddhaTithi = 1
        )
        val occ2039 = BsDateConverter.resolveEventOccurrencesInBsYear(
            chaitraPratipada, 2039, MissingDayStrategy.CLAMP_TO_LAST_DAY
        )
        assertEquals(listOf(BsDate(2039, 12, 30)), occ2039.map { it.targetDate })
    }

    @Test
    fun jesthaShraddhaIn2083_andPurnimantaHelpers() {
        val jesthaShasthi = EventEntity(
            id = 4L,
            title = "Jestha Shukla Shasthi Shraddha",
            bsMonth = 2,
            bsDay = 18,
            startBsYear = 2082,
            isShraddha = true,
            shraddhaTithi = 6
        )
        val occ2083 = BsDateConverter.resolveEventOccurrence(
            jesthaShasthi, 2083, MissingDayStrategy.CLAMP_TO_LAST_DAY
        )
        assertEquals(BsDate(2083, 3, 6), occ2083?.targetDate)

        for (amanta in 1..12) {
            for (tithi in 1..30) {
                val purnimanta = TithiCalculator.amantaToPurnimantaMonth(amanta, tithi)
                val roundTrip = TithiCalculator.purnimantaToAmantaMonth(purnimanta, tithi)
                assertEquals("Round-trip failed for amanta=$amanta tithi=$tithi", amanta, roundTrip)
            }
        }

        for (amanta in 1..12) {
            for (tithi in listOf(1, 15, 16, 30)) {
                val anchor = BsDateConverter.findAnchorDateForShraddha(amanta, tithi, 2083)
                val probe = EventEntity(
                    title = "Probe",
                    bsMonth = anchor.month,
                    bsDay = anchor.day,
                    startBsYear = anchor.year,
                    isShraddha = true,
                    shraddhaTithi = tithi
                )
                assertEquals(
                    "Anchor $anchor for (amanta=$amanta, tithi=$tithi) resolved wrong lunar month",
                    amanta,
                    BsDateConverter.getShraddhaLunarMonth(probe)
                )
            }
        }
    }
}
