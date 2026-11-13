package np.com.miti.patro.domain.engine

import np.com.miti.patro.domain.model.BsDate
import np.com.miti.patro.domain.model.Paksha
import np.com.miti.patro.domain.model.TithiInfo
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.floor
import kotlin.math.sin

/**
 * Offline Astronomical Lunar Tithi & Amanta Lunar Month (Chandra Maas) Calculator.
 *
 * - Evaluates Sun and Moon ecliptic longitudes via Jean Meeus's Chapter 25 & 47 equations.
 * - Sunrise Tithi (Udaya Tithi) is evaluated at 05:45 NST (00:00 UTC).
 * - Aparahna Tithi (used for Shraddha) is evaluated at 14:00 NST (08:15 UTC).
 * - Detects Tithi Kshaya (when 2 Tithis fall on the same solar day, i.e. tomorrow's sunrise jumps by +2)
 *   and Vriddhi / Adhika Tithi (when 1 Tithi spans 2 consecutive sunrises).
 * - Determines the true Amanta Lunar Month (shuddhaLunarMonth 1..12) and Intercalary Leap Month
 *   (isAdhikMaas / मलमास) by comparing the BS solar months of the preceding and ending Aunsis.
 */
object TithiCalculator {

    private val TITHI_CACHE = ConcurrentHashMap<Long, TithiInfo>(512)
    private val RAW_SUNRISE_CACHE = ConcurrentHashMap<Long, Int>(1024)
    private val LUNATION_CACHE = ConcurrentHashMap<Long, Pair<Int, Boolean>>(1024)

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
        "Ekad", "Dwad", "Tray", "Chat", "Purn",
        "Prat", "Dwit", "Trit", "Chat", "Panc",
        "Shas", "Sapt", "Asht", "Nawa", "Dash",
        "Ekad", "Dwad", "Tray", "Chat", "Auns"
    )

    private val SHORT_LABELS_NE = arrayOf(
        "प्रति", "द्वि", "तृती", "चतु", "पञ्च",
        "षष्ठी", "सप्त", "अष्ट", "नवमी", "दशमी",
        "एका", "द्वा", "त्रयो", "चतु", "पूर्णिमा",
        "प्रति", "द्वि", "तृती", "चतु", "पञ्च",
        "षष्ठी", "सप्त", "अष्ट", "नवमी", "दशमी",
        "एका", "द्वा", "त्रयो", "चतु", "औंसी"
    )

    fun calculateTithi(bsDate: BsDate, adDate: LocalDate = BsDateConverter.bsToAd(bsDate)): TithiInfo {
        return calculateTithiAtSunrise(adDate)
    }

    fun calculateTithiAtSunrise(adDate: LocalDate): TithiInfo {
        val epochDay = adDate.toEpochDay()
        TITHI_CACHE[epochDay]?.let { return it }

        val tPrev = rawTithiAtSunrise(adDate.minusDays(1))
        val tSun = rawTithiAtSunrise(adDate)
        val tNext = rawTithiAtSunrise(adDate.plusDays(1))
        val tAparahna = rawTithiAtNstHour(adDate, nstHour = 14.0)

        val isVriddhiSecond = (tSun == tPrev)
        val diffNext = (tNext - tSun + 30) % 30
        val kshayaNumber = if (diffNext == 2) (tSun % 30) + 1 else null

        val (shuddhaMonth, isAdhik) = getLunationInfo(adDate)

        val computed = buildTithiInfo(
            tithiNumber = tSun,
            kshayaTithiNumber = kshayaNumber,
            isVriddhiSecondDay = isVriddhiSecond,
            aparahnaTithiNumber = tAparahna,
            shuddhaLunarMonth = shuddhaMonth,
            isAdhikMaas = isAdhik
        )
        TITHI_CACHE[epochDay] = computed
        return computed
    }

    fun calculateTithiAtAparahna(adDate: LocalDate): Int {
        return rawTithiAtNstHour(adDate, nstHour = 14.0)
    }

    /**
     * Determines the true Amanta Lunar Month (1=Baisakh..12=Chaitra) and whether the lunation
     * containing [adDate] is an intercalary Adhik Maas (मलमास, two Aunsis inside the same BS solar month).
     */
    fun getLunationInfo(adDate: LocalDate): Pair<Int, Boolean> {
        val epochDay = adDate.toEpochDay()
        LUNATION_CACHE[epochDay]?.let { return it }

        // 1. Walk backward from yesterday to find the preceding Aunsi (end of previous lunation)
        var curBack = adDate.minusDays(1)
        for (step in 0 until 35) {
            val ts = rawTithiAtSunrise(curBack)
            val tn = rawTithiAtSunrise(curBack.plusDays(1))
            if ((ts == 29 || ts == 30) && (tn == 1 || tn == 2)) {
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
            if ((ts == 29 || ts == 30) && (tn == 1 || tn == 2)) {
                break
            }
            curFwd = curFwd.plusDays(1)
        }
        val endAunsiAd = curFwd

        val prevBsMonth = BsDateConverter.adToBs(prevAunsiAd).month
        val endBsMonth = BsDateConverter.adToBs(endAunsiAd).month

        val isAdhikMaas = (prevBsMonth == endBsMonth)
        val shuddhaLunarMonth = ((endBsMonth - 2 + 12) % 12) + 1
        val result = shuddhaLunarMonth to isAdhikMaas

        // Cache for all days in this lunation (prevAunsiAd + 1 .. endAunsiAd)
        var fill = prevAunsiAd.plusDays(1)
        while (!fill.isAfter(endAunsiAd)) {
            LUNATION_CACHE[fill.toEpochDay()] = result
            fill = fill.plusDays(1)
        }
        return result
    }

    fun rawTithiAtSunrise(adDate: LocalDate): Int {
        val epochDay = adDate.toEpochDay()
        RAW_SUNRISE_CACHE[epochDay]?.let { return it }
        val t = rawTithiAtNstHour(adDate, nstHour = 5.75)
        RAW_SUNRISE_CACHE[epochDay] = t
        return t
    }

    private fun rawTithiAtNstHour(adDate: LocalDate, nstHour: Double): Int {
        val utcHours = nstHour - 5.75
        val j2000Epoch = LocalDate.of(2000, 1, 1)
        val daysSinceJ2000 = ChronoUnit.DAYS.between(j2000Epoch, adDate).toDouble() + (utcHours - 12.0) / 24.0
        val t = daysSinceJ2000 / 36525.0

        val l0 = normalizeDegrees(280.46646 + 36000.76983 * t + 0.0003032 * t * t)
        val mSun = normalizeDegrees(357.52911 + 35999.05029 * t - 0.0001537 * t * t)
        val mSunRad = Math.toRadians(mSun)

        val cSun = (1.914602 - 0.004817 * t - 0.000014 * t * t) * sin(mSunRad) +
            (0.019993 - 0.000101 * t) * sin(2 * mSunRad) +
            0.000289 * sin(3 * mSunRad)

        val sunTrueLong = normalizeDegrees(l0 + cSun)

        val lPrime = normalizeDegrees(218.3165 + 481267.8813 * t)
        val d = normalizeDegrees(297.8502 + 445267.1115 * t - 0.0016300 * t * t)
        val m = normalizeDegrees(357.5291 + 35999.0503 * t - 0.0001536 * t * t)
        val mPrime = normalizeDegrees(134.9634 + 477198.8676 * t + 0.0089970 * t * t)
        val f = normalizeDegrees(93.2721 + 483202.0175 * t - 0.0034029 * t * t)

        val dRad = Math.toRadians(d)
        val mRad = Math.toRadians(m)
        val mPrimeRad = Math.toRadians(mPrime)
        val fRad = Math.toRadians(f)

        val moonLongCorrection =
            6.288774 * sin(mPrimeRad) +
            1.274027 * sin(2 * dRad - mPrimeRad) +
            0.658314 * sin(2 * dRad) +
            0.213618 * sin(2 * mPrimeRad) -
            0.185596 * sin(mRad) -
            0.114336 * sin(2 * fRad) +
            0.058793 * sin(2 * dRad - 2 * mPrimeRad) +
            0.057066 * sin(2 * dRad - mRad - mPrimeRad) +
            0.053322 * sin(2 * dRad + mPrimeRad) +
            0.045758 * sin(2 * dRad - mRad) -
            0.040923 * sin(mRad - mPrimeRad) -
            0.034720 * sin(dRad) -
            0.030383 * sin(mRad + mPrimeRad) +
            0.015327 * sin(2 * dRad - 2 * fRad)

        val moonTrueLong = normalizeDegrees(lPrime + moonLongCorrection)
        val elongation = normalizeDegrees(moonTrueLong - sunTrueLong)
        return (floor(elongation / 12.0).toInt() + 1).coerceIn(1, 30)
    }

    fun buildTithiInfo(
        tithiNumber: Int,
        kshayaTithiNumber: Int? = null,
        isVriddhiSecondDay: Boolean = false,
        aparahnaTithiNumber: Int = tithiNumber,
        shuddhaLunarMonth: Int = 1,
        isAdhikMaas: Boolean = false
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
            in 1..7 -> "◐"
            in 8..14 -> "◕"
            in 16..22 -> "◑"
            else -> "◔"
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
            isAdhikMaas = isAdhikMaas
        )
    }

    private fun normalizeDegrees(deg: Double): Double {
        var r = deg % 360.0
        if (r < 0) r += 360.0
        return r
    }
}
