package np.com.miti.patro.domain.engine

import np.com.miti.patro.domain.model.BsDate
import np.com.miti.patro.domain.model.Paksha
import np.com.miti.patro.domain.model.TithiInfo
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

/**
 * Offline Astronomical Lunar Tithi & Amanta Lunar Month (Chandra Maas) Calculator.
 *
 * - Evaluates Sun and Moon ecliptic longitudes via Jean Meeus's Chapter 25 & 47 equations
 *   using Terrestrial Time (TT = UT + ΔT).
 * - Sunrise Tithi (Udaya Tithi), Aparahna window, Pradosh, and Nishita are evaluated using
 *   seasonal astronomical sunrise/sunset for Kathmandu (27.7172°N, 85.3240°E, UTC+05:45).
 * - Detects Tithi Kshaya (when 2 Tithis fall on the same solar day, i.e. tomorrow's sunrise jumps by +2)
 *   and Vriddhi / Adhika Tithi (when 1 Tithi spans 2 consecutive sunrises).
 * - Determines the true Amanta Lunar Month (shuddhaLunarMonth 1..12), Intercalary Adhik Maas
 *   (isAdhikMaas / मलमास), and Kshaya Maas by evaluating the sidereal (Lahiri) solar Rashi
 *   at the exact preceding and ending New Moon conjunction instants.
 */
object TithiCalculator {

    data class LunationDetails(
        val shuddhaLunarMonth: Int,
        val isAdhikMaas: Boolean,
        val kshayaLunarMonth: Int?,
        val endAunsiEpochDay: Long
    )

    data class TithiTimeSpan(
        val startAdDate: LocalDate,
        val startHour: Int,
        val startMinute: Int,
        val endAdDate: LocalDate,
        val endHour: Int,
        val endMinute: Int
    ) {
        fun formatDisplay(anchorAdDate: LocalDate, useDevanagari: Boolean): String {
            fun formatPoint(pointDate: LocalDate, hour: Int, minute: Int): String {
                val timeStr = BsCalendarData.formatTime24h(hour, minute, useDevanagari)
                return if (pointDate == anchorAdDate) {
                    timeStr
                } else {
                    val pointBs = BsDateConverter.adToBs(pointDate)
                    val mShort = BsCalendarData.getShortMonthName(pointBs.month, useDevanagari)
                    val dStr = BsCalendarData.formatNumber(pointBs.day, useDevanagari)
                    "$mShort $dStr $timeStr"
                }
            }
            val startStr = formatPoint(startAdDate, startHour, startMinute)
            val endStr = formatPoint(endAdDate, endHour, endMinute)
            return "$startStr -> $endStr"
        }
    }

    private val TITHI_CACHE = ConcurrentHashMap<Long, TithiInfo>(512)
    private val TITHI_SPAN_CACHE = ConcurrentHashMap<Long, TithiTimeSpan>(256)
    private val RAW_SUNRISE_CACHE = ConcurrentHashMap<Long, Int>(1024)
    private val LUNATION_DETAILS_CACHE = ConcurrentHashMap<Long, LunationDetails>(1024)
    private val SUNRISE_SUNSET_CACHE = ConcurrentHashMap<Long, Pair<Double, Double>>(1024)

    private val J2000_EPOCH: LocalDate = LocalDate.of(2000, 1, 1)
    private const val KTM_LAT_DEG = 27.7172
    private const val KTM_LON_DEG = 85.3240

    private val SHUKLA_NAMES_EN = arrayOf(
        "Pratipada", "Dwitiya", "Tritiya", "Chaturthi", "Panchami",
        "Shasthi", "Saptami", "Ashtami", "Nawami", "Dashami",
        "Ekadashi", "Dwadashi", "Trayodashi", "Chaturdashi", "Purnima"
    )

    private val KRISHNA_NAMES_EN = arrayOf(
        "Pratipada", "Dwitiya", "Tritiya", "Chaturthi", "Panchami",
        "Shasthi", "Saptami", "Ashtami", "Nawami", "Dashami",
        "Ekadashi", "Dwadashi", "Trayodashi", "Chaturdashi", "Aunsi"
    )

    private val SHUKLA_NAMES_NE = arrayOf(
        "प्रतिपदा", "द्वितीया", "तृतीया", "चतुर्थी", "पञ्चमी",
        "षष्ठी", "सप्तमी", "अष्टमी", "नवमी", "दशमी",
        "एकादशी", "द्वादशी", "त्रयोदशी", "चतुर्दशी", "पूर्णिमा"
    )

    private val KRISHNA_NAMES_NE = arrayOf(
        "प्रतिपदा", "द्वितीया", "तृतीया", "चतुर्थी", "पञ्चमी",
        "षष्ठी", "सप्तमी", "अष्टमी", "नवमी", "दशमी",
        "एकादशी", "द्वादशी", "त्रयोदशी", "चतुर्दशी", "औंसी"
    )

    private val SHORT_LABELS_EN = arrayOf(
        "Prat", "Dwit", "Trit", "Chat", "Panc",
        "Shas", "Sapt", "Asht", "Nawa", "Dash",
        "Ekad", "Dwad", "Tray", "Chtd", "Purn",
        "Prat", "Dwit", "Trit", "Chat", "Panc",
        "Shas", "Sapt", "Asht", "Nawa", "Dash",
        "Ekad", "Dwad", "Tray", "Chtd", "Auns"
    )

    private val SHORT_LABELS_NE = arrayOf(
        "प्रति", "द्वि", "तृती", "चतुर्थी", "पञ्च",
        "षष्ठी", "सप्त", "अष्ट", "नवमी", "दशमी",
        "एका", "द्वा", "त्रयो", "चतुर्द", "पूर्णिमा",
        "प्रति", "द्वि", "तृती", "चतुर्थी", "पञ्च",
        "षष्ठी", "सप्त", "अष्ट", "नवमी", "दशमी",
        "एका", "द्वा", "त्रयो", "चतुर्द", "औंसी"
    )

    fun amantaToPurnimantaMonth(amantaMonth: Int, tithiNumber: Int): Int {
        val m = amantaMonth.coerceIn(1, 12)
        return if (tithiNumber <= 15) m else (m % 12) + 1
    }

    fun purnimantaToAmantaMonth(purnimantaMonth: Int, tithiNumber: Int): Int {
        val m = purnimantaMonth.coerceIn(1, 12)
        return if (tithiNumber <= 15) m else ((m - 2 + 12) % 12) + 1
    }

    fun calculateTithi(bsDate: BsDate, adDate: LocalDate = BsDateConverter.bsToAd(bsDate)): TithiInfo {
        return calculateTithiAtSunrise(adDate)
    }

    fun calculateTithiAtSunrise(adDate: LocalDate): TithiInfo {
        val epochDay = adDate.toEpochDay()
        TITHI_CACHE[epochDay]?.let { return it }

        val tPrev = rawTithiAtSunrise(adDate.minusDays(1))
        val tSun = rawTithiAtSunrise(adDate)
        val tNext = rawTithiAtSunrise(adDate.plusDays(1))
        val tAparahna = calculateTithiAtAparahna(adDate)
        val tPradosh = pradoshTithi(adDate)
        val tNishita = nishitaTithi(adDate)

        val isVriddhiSecond = (tSun == tPrev)
        val diffNext = (tNext - tSun + 30) % 30
        val kshayaNumber = if (diffNext == 2) (tSun % 30) + 1 else null

        val lun = getLunationDetails(adDate)
        val nextLun = if (tSun >= 29) getLunationDetails(adDate.plusDays(1)) else lun

        val (kshayaLm, kshayaAdhik) = when {
            kshayaNumber == null -> null to false
            kshayaNumber == 1 && tSun == 30 -> nextLun.shuddhaLunarMonth to nextLun.isAdhikMaas
            else -> lun.shuddhaLunarMonth to lun.isAdhikMaas
        }

        val (apLm, apAdhik) = if (tSun in 29..30 && tAparahna in 1..2) {
            nextLun.shuddhaLunarMonth to nextLun.isAdhikMaas
        } else {
            lun.shuddhaLunarMonth to lun.isAdhikMaas
        }

        val computed = buildTithiInfo(
            tithiNumber = tSun,
            kshayaTithiNumber = kshayaNumber,
            isVriddhiSecondDay = isVriddhiSecond,
            aparahnaTithiNumber = tAparahna,
            shuddhaLunarMonth = lun.shuddhaLunarMonth,
            isAdhikMaas = lun.isAdhikMaas,
            kshayaTithiLunarMonth = kshayaLm,
            kshayaTithiIsAdhikMaas = kshayaAdhik,
            aparahnaLunarMonth = apLm,
            aparahnaIsAdhikMaas = apAdhik,
            pradoshTithiNumber = tPradosh,
            nishitaTithiNumber = tNishita,
            kshayaLunarMonth = lun.kshayaLunarMonth
        )
        TITHI_CACHE[epochDay] = computed
        return computed
    }

    fun calculateTithiAtAparahna(adDate: LocalDate): Int {
        val (_, apMid, _) = kathmanduAparahnaWindowNst(adDate)
        return rawTithiAtNstHour(adDate, apMid)
    }

    /**
     * Returns (sunriseNstHour, sunsetNstHour) for Kathmandu (27.7172°N, 85.3240°E, UTC+05:45)
     * using NOAA solar geometry equations.
     */
    fun kathmanduSunriseSunsetNst(adDate: LocalDate): Pair<Double, Double> {
        val epochDay = adDate.toEpochDay()
        SUNRISE_SUNSET_CACHE[epochDay]?.let { return it }

        val daysUt = ChronoUnit.DAYS.between(J2000_EPOCH, adDate).toDouble() + (6.25 - 12.0) / 24.0
        val t = daysUt / 36525.0

        val l0 = normalizeDegrees(280.46646 + 36000.76983 * t + 0.0003032 * t * t)
        val mRad = Math.toRadians(normalizeDegrees(357.52911 + 35999.05029 * t - 0.0001537 * t * t))
        val e = 0.016708634 - 0.000042037 * t
        val epsRad = Math.toRadians(23.439291 - 0.0130042 * t)
        val tanHalfEps = tan(epsRad / 2.0)
        val y = tanHalfEps * tanHalfEps
        val l0Rad = Math.toRadians(l0)

        val eqTime = 4.0 * Math.toDegrees(
            y * sin(2.0 * l0Rad) -
                2.0 * e * sin(mRad) +
                4.0 * e * y * sin(mRad) * cos(2.0 * l0Rad) -
                0.5 * y * y * sin(4.0 * l0Rad) -
                1.25 * e * e * sin(2.0 * mRad)
        )

        val c = (1.914602 - 0.004817 * t) * sin(mRad) +
            0.019993 * sin(2.0 * mRad) +
            0.000289 * sin(3.0 * mRad)
        val sunTrueLongRad = Math.toRadians(normalizeDegrees(l0 + c))
        val decl = asin(sin(epsRad) * sin(sunTrueLongRad))

        val latRad = Math.toRadians(KTM_LAT_DEG)
        val cosHa = ((cos(Math.toRadians(90.833)) / (cos(latRad) * cos(decl))) -
            tan(latRad) * tan(decl)).coerceIn(-1.0, 1.0)
        val ha = Math.toDegrees(acos(cosHa))

        val sunriseNst = (720.0 - 4.0 * (KTM_LON_DEG + ha) - eqTime + 345.0) / 60.0
        val sunsetNst = (720.0 - 4.0 * (KTM_LON_DEG - ha) - eqTime + 345.0) / 60.0
        val result = sunriseNst to sunsetNst
        SUNRISE_SUNSET_CACHE[epochDay] = result
        return result
    }

    /**
     * Returns (start, midpoint, end) NST hours of the Aparahna Muhurta (4th fifth of daytime)
     * in Kathmandu.
     */
    fun kathmanduAparahnaWindowNst(adDate: LocalDate): Triple<Double, Double, Double> {
        val (sr, ss) = kathmanduSunriseSunsetNst(adDate)
        val din = ss - sr
        return Triple(sr + 0.6 * din, sr + 0.7 * din, sr + 0.8 * din)
    }

    fun pradoshTithi(adDate: LocalDate): Int {
        val (_, sunset) = kathmanduSunriseSunsetNst(adDate)
        return rawTithiAtNstHour(adDate, sunset + 0.5)
    }

    fun nishitaTithi(adDate: LocalDate): Int {
        val (_, sunsetToday) = kathmanduSunriseSunsetNst(adDate)
        val (sunriseTomorrow, _) = kathmanduSunriseSunsetNst(adDate.plusDays(1))
        return rawTithiAtNstHour(adDate, 0.5 * (sunsetToday + 24.0 + sunriseTomorrow))
    }

    fun getLunationInfo(adDate: LocalDate): Pair<Int, Boolean> {
        val details = getLunationDetails(adDate)
        return details.shuddhaLunarMonth to details.isAdhikMaas
    }

    /**
     * Determines the true Amanta Lunar Month (1=Baisakh..12=Chaitra), Adhik Maas status,
     * and Kshaya Maas (if two solar Sankrantis fall within the lunation) by evaluating the
     * sidereal solar Rashi at the exact preceding and ending New Moon conjunction instants.
     */
    fun getLunationDetails(adDate: LocalDate): LunationDetails {
        val epochDay = adDate.toEpochDay()
        LUNATION_DETAILS_CACHE[epochDay]?.let { return it }

        // 1. Walk backward from yesterday to find the preceding Aunsi (end of previous lunation)
        var curBack = adDate.minusDays(1)
        for (step in 0 until 35) {
            val ts = rawTithiAtSunrise(curBack)
            val tn = rawTithiAtSunrise(curBack.plusDays(1))
            if (ts in 29..30 && tn in 1..2) {
                break
            }
            curBack = curBack.minusDays(1)
        }
        val prevAunsiAd = curBack

        // 2. Walk forward from today to find the ending Aunsi of the current lunation
        var curFwd = adDate
        for (step in 0 until 35) {
            val ts = rawTithiAtSunrise(curFwd)
            val tn = rawTithiAtSunrise(curFwd.plusDays(1))
            if (ts in 29..30 && tn in 1..2) {
                break
            }
            curFwd = curFwd.plusDays(1)
        }
        val endAunsiAd = curFwd

        val r1 = exactNewMoonRashi(prevAunsiAd)
        val r2 = exactNewMoonRashi(endAunsiAd)
        val rDiff = (r2 - r1 + 12) % 12

        val result = when (rDiff) {
            0 -> LunationDetails(
                shuddhaLunarMonth = r1,
                isAdhikMaas = true,
                kshayaLunarMonth = null,
                endAunsiEpochDay = endAunsiAd.toEpochDay()
            )
            2 -> LunationDetails(
                shuddhaLunarMonth = r1,
                isAdhikMaas = false,
                kshayaLunarMonth = (r1 % 12) + 1,
                endAunsiEpochDay = endAunsiAd.toEpochDay()
            )
            else -> LunationDetails(
                shuddhaLunarMonth = r1,
                isAdhikMaas = false,
                kshayaLunarMonth = null,
                endAunsiEpochDay = endAunsiAd.toEpochDay()
            )
        }

        // Cache for all days in this lunation (prevAunsiAd + 1 .. endAunsiAd)
        var fill = prevAunsiAd.plusDays(1)
        while (!fill.isAfter(endAunsiAd)) {
            LUNATION_DETAILS_CACHE[fill.toEpochDay()] = result
            fill = fill.plusDays(1)
        }
        return result
    }

    private const val PATRO_TRIGATI_HOURS = 1.2 // 3 Ghatis (72 min) post-sunrise threshold per Dharmasindhu / Nepali Patro

    private fun exactNewMoonRashi(aunsiAd: LocalDate): Int {
        var lo = kathmanduSunriseSunsetNst(aunsiAd).first + PATRO_TRIGATI_HOURS
        var hi = 24.0 + kathmanduSunriseSunsetNst(aunsiAd.plusDays(1)).first + PATRO_TRIGATI_HOURS
        for (i in 0 until 22) {
            val mid = 0.5 * (lo + hi)
            val (sunLong, moonLong, _) = sunMoonSiderealAtNst(aunsiAd, mid)
            val elong = normalizeDegrees(moonLong - sunLong)
            if (elong > 180.0) {
                lo = mid
            } else {
                hi = mid
            }
        }
        val (_, _, sunSidereal) = sunMoonSiderealAtNst(aunsiAd, 0.5 * (lo + hi))
        return (floor(sunSidereal / 30.0).toInt() % 12) + 1
    }

    /**
     * Effective Patro Tithi for the solar day in Kathmandu, applying the traditional
     * 3-Ghati (72 min) Tri-Ghati sunrise prevalence rule so brief dawn Tithis expiring
     * within ~1 hour of sunrise do not shift the day's Tithi or create false Vriddhis.
     */
    fun rawTithiAtSunrise(adDate: LocalDate): Int {
        val epochDay = adDate.toEpochDay()
        RAW_SUNRISE_CACHE[epochDay]?.let { return it }
        val (sunriseNst, _) = kathmanduSunriseSunsetNst(adDate)
        val t = rawTithiAtNstHour(adDate, sunriseNst + PATRO_TRIGATI_HOURS)
        RAW_SUNRISE_CACHE[epochDay] = t
        return t
    }

    fun rawTithiAtNstHour(adDate: LocalDate, nstHour: Double): Int {
        val (sunTrueLong, moonTrueLong, _) = sunMoonSiderealAtNst(adDate, nstHour)
        val elongation = normalizeDegrees(moonTrueLong - sunTrueLong)
        return (floor(elongation / 12.0).toInt() + 1).coerceIn(1, 30)
    }

    /**
     * Computes exact NST start and end instants (Kathmandu, UTC+05:45) for the given Tithi
     * active on [adDate] via binary search to sub-minute precision.
     */
    fun calculateTithiTimeSpan(adDate: LocalDate, tithiNumber: Int = rawTithiAtSunrise(adDate)): TithiTimeSpan {
        val cacheKey = adDate.toEpochDay() * 32L + tithiNumber.coerceIn(1, 30)
        TITHI_SPAN_CACHE[cacheKey]?.let { return it }

        val target = tithiNumber.coerceIn(1, 30)
        val (sunriseNst, _) = kathmanduSunriseSunsetNst(adDate)
        var anchorH = sunriseNst + PATRO_TRIGATI_HOURS
        if (rawTithiAtNstHour(adDate, anchorH) != target) {
            var found = false
            for (step in 0..24) {
                val fwd = anchorH + step * 1.0
                if (rawTithiAtNstHour(adDate, fwd) == target) {
                    anchorH = fwd
                    found = true
                    break
                }
                val bwd = anchorH - step * 1.0
                if (rawTithiAtNstHour(adDate, bwd) == target) {
                    anchorH = bwd
                    found = true
                    break
                }
            }
            if (!found) {
                return TithiTimeSpan(adDate, 0, 0, adDate, 23, 59)
            }
        }

        // Walk backward in 1-hour steps until leaving target Tithi, then bisect
        var backH = anchorH
        var guard = 0
        while (rawTithiAtNstHour(adDate, backH) == target && guard < 40) {
            backH -= 1.0
            guard++
        }
        var loStart = backH
        var hiStart = backH + 1.0
        for (i in 0 until 16) {
            val mid = 0.5 * (loStart + hiStart)
            if (rawTithiAtNstHour(adDate, mid) == target) hiStart = mid else loStart = mid
        }
        val startNstHours = 0.5 * (loStart + hiStart)

        // Walk forward in 1-hour steps until leaving target Tithi, then bisect
        var fwdH = anchorH
        guard = 0
        while (rawTithiAtNstHour(adDate, fwdH) == target && guard < 40) {
            fwdH += 1.0
            guard++
        }
        var loEnd = fwdH - 1.0
        var hiEnd = fwdH
        for (i in 0 until 16) {
            val mid = 0.5 * (loEnd + hiEnd)
            if (rawTithiAtNstHour(adDate, mid) == target) loEnd = mid else hiEnd = mid
        }
        val endNstHours = 0.5 * (loEnd + hiEnd)

        fun toDateTime(hoursFromMidnight: Double): Triple<LocalDate, Int, Int> {
            val totalMin = (hoursFromMidnight * 60.0).roundToInt()
            val dayOffset = Math.floorDiv(totalMin, 1440)
            val minOfDay = Math.floorMod(totalMin, 1440)
            return Triple(adDate.plusDays(dayOffset.toLong()), minOfDay / 60, minOfDay % 60)
        }

        val (sDate, sH, sM) = toDateTime(startNstHours)
        val (eDate, eH, eM) = toDateTime(endNstHours)
        val span = TithiTimeSpan(sDate, sH, sM, eDate, eH, eM)
        if (TITHI_SPAN_CACHE.size >= 256) {
            TITHI_SPAN_CACHE.clear()
        }
        TITHI_SPAN_CACHE[cacheKey] = span
        return span
    }

    private fun deltaTDays(yearDec: Double): Double {
        val dtSec = when {
            yearDec < 1941.0 -> {
                val u = yearDec - 1920.0
                21.20 + 0.84493 * u - 0.076100 * u * u + 0.0020936 * u * u * u
            }
            yearDec < 1961.0 -> {
                val u = yearDec - 1950.0
                29.07 + 0.407 * u - (u * u) / 233.0 + (u * u * u) / 2547.0
            }
            yearDec < 1986.0 -> {
                val u = yearDec - 1975.0
                45.45 + 1.067 * u - (u * u) / 260.0 - (u * u * u) / 718.0
            }
            yearDec < 2005.0 -> {
                val u = yearDec - 2000.0
                val u2 = u * u
                val u3 = u2 * u
                63.86 + 0.3345 * u - 0.060374 * u2 + 0.0017275 * u3 +
                    0.000651814 * u2 * u2 + 0.00002373599 * u2 * u3
            }
            yearDec < 2050.0 -> {
                val u = yearDec - 2000.0
                62.92 + 0.32217 * u + 0.005589 * u * u
            }
            else -> {
                val u = (yearDec - 1820.0) / 100.0
                -20.0 + 32.0 * u * u - 0.5628 * (2150.0 - yearDec)
            }
        }
        return dtSec / 86400.0
    }

    private fun sunMoonSiderealAtNst(adDate: LocalDate, nstHour: Double): Triple<Double, Double, Double> {
        val utcHours = nstHour - 5.75
        val daysUt = ChronoUnit.DAYS.between(J2000_EPOCH, adDate).toDouble() + (utcHours - 12.0) / 24.0
        val daysTt = daysUt + deltaTDays(2000.0 + daysUt / 365.25)
        val t = daysTt / 36525.0

        val l0 = normalizeDegrees(280.46646 + 36000.76983 * t + 0.0003032 * t * t)
        val mSun = normalizeDegrees(357.52911 + 35999.05029 * t - 0.0001537 * t * t)
        val mSunRad = Math.toRadians(mSun)

        val cSun = (1.914602 - 0.004817 * t - 0.000014 * t * t) * sin(mSunRad) +
            (0.019993 - 0.000101 * t) * sin(2 * mSunRad) +
            0.000289 * sin(3 * mSunRad)

        val omegaRad = Math.toRadians(125.04 - 1934.136 * t)
        val sunTrueLong = normalizeDegrees(l0 + cSun)
        val sunAppLong = normalizeDegrees(sunTrueLong - 0.00569 - 0.00478 * sin(omegaRad))

        val lPrime = normalizeDegrees(218.3164477 + 481267.88123421 * t - 0.0015786 * t * t)
        val d = normalizeDegrees(297.8501921 + 445267.1114034 * t - 0.0018819 * t * t)
        val m = normalizeDegrees(357.5291092 + 35999.0502909 * t - 0.0001536 * t * t)
        val mPrime = normalizeDegrees(134.9633964 + 477198.8675055 * t + 0.0087414 * t * t)
        val f = normalizeDegrees(93.2720950 + 483202.0175233 * t - 0.0036539 * t * t)

        val dRad = Math.toRadians(d)
        val mRad = Math.toRadians(m)
        val mPrimeRad = Math.toRadians(mPrime)
        val fRad = Math.toRadians(f)
        val eFactor = 1.0 - 0.002516 * t - 0.0000074 * t * t

        val moonLongCorrection =
            6.288774 * sin(mPrimeRad) +
            1.274027 * sin(2 * dRad - mPrimeRad) +
            0.658314 * sin(2 * dRad) +
            0.213618 * sin(2 * mPrimeRad) -
            0.185116 * eFactor * sin(mRad) -
            0.114332 * sin(2 * fRad) +
            0.058793 * sin(2 * dRad - 2 * mPrimeRad) +
            0.057066 * eFactor * sin(2 * dRad - mRad - mPrimeRad) +
            0.053322 * sin(2 * dRad + mPrimeRad) +
            0.045758 * eFactor * sin(2 * dRad - mRad) -
            0.040923 * eFactor * sin(mRad - mPrimeRad) -
            0.034720 * sin(dRad) -
            0.030383 * eFactor * sin(mRad + mPrimeRad) +
            0.015327 * sin(2 * dRad - 2 * fRad) -
            0.012528 * sin(mPrimeRad + 2 * fRad) +
            0.010980 * sin(mPrimeRad - 2 * fRad) +
            0.010675 * sin(4 * dRad - mPrimeRad) +
            0.010034 * sin(3 * mPrimeRad) +
            0.008548 * sin(4 * dRad - 2 * mPrimeRad) -
            0.007888 * eFactor * sin(2 * dRad + mRad - mPrimeRad)

        val nutationDeg = (-17.20 / 3600.0) * sin(omegaRad) -
            (1.32 / 3600.0) * sin(2.0 * Math.toRadians(l0))
        val moonAppLong = normalizeDegrees(lPrime + moonLongCorrection + nutationDeg)
        val ayanamsa = 23.853055 + (50.290966 / 3600.0) * (daysTt / 365.25)
        val sunSidereal = normalizeDegrees(sunTrueLong - ayanamsa)
        return Triple(sunAppLong, moonAppLong, sunSidereal)
    }

    fun buildTithiInfo(
        tithiNumber: Int,
        kshayaTithiNumber: Int? = null,
        isVriddhiSecondDay: Boolean = false,
        aparahnaTithiNumber: Int = tithiNumber,
        shuddhaLunarMonth: Int = 1,
        isAdhikMaas: Boolean = false,
        kshayaTithiLunarMonth: Int? = null,
        kshayaTithiIsAdhikMaas: Boolean = false,
        aparahnaLunarMonth: Int = shuddhaLunarMonth,
        aparahnaIsAdhikMaas: Boolean = isAdhikMaas,
        pradoshTithiNumber: Int = tithiNumber,
        nishitaTithiNumber: Int = tithiNumber,
        kshayaLunarMonth: Int? = null
    ): TithiInfo {
        val clamped = tithiNumber.coerceIn(1, 30)
        val isShukla = clamped <= 15
        val paksha = if (isShukla) Paksha.SHUKLA else Paksha.KRISHNA
        val pakshaDay = if (isShukla) clamped else clamped - 15

        val englishName = if (isShukla) SHUKLA_NAMES_EN[pakshaDay - 1] else KRISHNA_NAMES_EN[pakshaDay - 1]
        val nepaliName = if (isShukla) SHUKLA_NAMES_NE[pakshaDay - 1] else KRISHNA_NAMES_NE[pakshaDay - 1]
        val shortLabel = SHORT_LABELS_EN[clamped - 1]
        val shortLabelNe = SHORT_LABELS_NE[clamped - 1]

        val kClamped = kshayaTithiNumber?.coerceIn(1, 30)
        val kEn = kClamped?.let { if (it <= 15) SHUKLA_NAMES_EN[it - 1] else KRISHNA_NAMES_EN[it - 16] }
        val kNe = kClamped?.let { if (it <= 15) SHUKLA_NAMES_NE[it - 1] else KRISHNA_NAMES_NE[it - 16] }
        val kShortEn = kClamped?.let { SHORT_LABELS_EN[it - 1] }
        val kShortNe = kClamped?.let { SHORT_LABELS_NE[it - 1] }

        val phaseSymbol = when (clamped) {
            15 -> "○"
            30 -> "●"
            in 1..14 -> "◐"
            else -> "◑"
        }

        return TithiInfo(
            number = clamped,
            paksha = paksha,
            pakshaDay = pakshaDay,
            englishName = englishName,
            nepaliName = nepaliName,
            shortLabel = shortLabel,
            shortLabelNe = shortLabelNe,
            phaseSymbol = phaseSymbol,
            kshayaTithiNumber = kClamped,
            kshayaEnglishName = kEn,
            kshayaNepaliName = kNe,
            kshayaShortLabel = kShortEn,
            kshayaShortLabelNe = kShortNe,
            isVriddhiSecondDay = isVriddhiSecondDay,
            aparahnaTithiNumber = aparahnaTithiNumber,
            shuddhaLunarMonth = shuddhaLunarMonth,
            isAdhikMaas = isAdhikMaas,
            kshayaTithiLunarMonth = kshayaTithiLunarMonth,
            kshayaTithiIsAdhikMaas = kshayaTithiIsAdhikMaas,
            aparahnaLunarMonth = aparahnaLunarMonth,
            aparahnaIsAdhikMaas = aparahnaIsAdhikMaas,
            pradoshTithiNumber = pradoshTithiNumber,
            nishitaTithiNumber = nishitaTithiNumber,
            kshayaLunarMonth = kshayaLunarMonth
        )
    }

    private fun normalizeDegrees(deg: Double): Double {
        var r = deg % 360.0
        if (r < 0) r += 360.0
        return r
    }
}
