package np.com.miti.patro.domain.engine

import np.com.miti.patro.domain.model.BsDate
import np.com.miti.patro.domain.model.Paksha
import np.com.miti.patro.domain.model.TithiInfo
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sin

object TithiCalculator {

    private val TITHI_NAMES_EN = arrayOf(
        "Pratipada", "Dwitiya", "Tritiya", "Chaturthi", "Panchami",
        "Shasthi", "Saptami", "Ashtami", "Nawami", "Dashami",
        "Ekadashi", "Dwadashi", "Trayodashi", "Chaturdashi", "Purnima",
        "Pratipada", "Dwitiya", "Tritiya", "Chaturthi", "Panchami",
        "Shasthi", "Saptami", "Ashtami", "Nawami", "Dashami",
        "Ekadashi", "Dwadashi", "Trayodashi", "Chaturdashi", "Aunsi"
    )

    private val TITHI_SHORT_EN = arrayOf(
        "Prat", "Dwit", "Trit", "Chat", "Panc",
        "Shas", "Sapt", "Asht", "Nawa", "Dash",
        "Ekad", "Dwad", "Tray", "Chat", "Purn",
        "Prat", "Dwit", "Trit", "Chat", "Panc",
        "Shas", "Sapt", "Asht", "Nawa", "Dash",
        "Ekad", "Dwad", "Tray", "Chat", "Auns"
    )

    private val TITHI_NAMES_NE = arrayOf(
        "प्रतिपदा", "द्वितीया", "तृतीया", "चतुर्थी", "पञ्चमी",
        "षष्ठी", "सप्तमी", "अष्टमी", "नवमी", "दशमी",
        "एकादशी", "द्वादशी", "त्रयोदशी", "चतुर्दशी", "पूर्णिमा",
        "प्रतिपदा", "द्वितीया", "तृतीया", "चतुर्थी", "पञ्चमी",
        "षष्ठी", "सप्तमी", "अष्टमी", "नवमी", "दशमी",
        "एकादशी", "द्वादशी", "त्रयोदशी", "चतुर्दशी", "औंसी"
    )

    private val TITHI_SHORT_NE = arrayOf(
        "प्रति", "द्वि", "तृती", "चतु", "पञ्च",
        "षष्ठी", "सप्त", "अष्ट", "नवमी", "दशमी",
        "एका", "द्वा", "त्रयो", "चतु", "पूर्णिमा",
        "प्रति", "द्वि", "तृती", "चतु", "पञ्च",
        "षष्ठी", "सप्त", "अष्ट", "नवमी", "दशमी",
        "एका", "द्वा", "त्रयो", "चतु", "औंसी"
    )

    private val PANCHANG_OVERRIDES: Map<String, Int> = mapOf(
        "2083-01-01" to 29,
        "2083-01-02" to 30,
        "2083-01-18" to 15,
        "2083-05-12" to 15,
        "2083-05-13" to 16,
        "2083-05-19" to 23,
        "2083-05-29" to 3,
        "2083-06-25" to 1,
        "2083-06-31" to 7,
        "2083-07-01" to 8,
        "2083-07-02" to 8,
        "2083-07-03" to 9,
        "2083-07-04" to 10,
        "2083-07-05" to 11,
        "2083-07-08" to 14,
        "2083-07-09" to 15,
        "2083-07-21" to 28,
        "2083-07-22" to 30,
        "2083-07-23" to 30,
        "2083-07-24" to 1,
        "2083-07-25" to 2,
        "2083-07-29" to 6,
        "2083-09-09" to 15,
        "2083-10-28" to 5,
        "2083-11-22" to 29,
        "2083-12-07" to 15,
        "2083-12-23" to 30
    )

    private class LunarTerm(
        val d: Int,
        val m: Int,
        val mp: Int,
        val f: Int,
        val coeff: Int
    )

    private val LUNAR_TERMS = arrayOf(
        LunarTerm(0, 0, 1, 0, 6288774),
        LunarTerm(2, 0, -1, 0, 1274027),
        LunarTerm(2, 0, 0, 0, 658314),
        LunarTerm(0, 0, 2, 0, 213618),
        LunarTerm(0, 1, 0, 0, -185116),
        LunarTerm(0, 0, 0, 2, -114332),
        LunarTerm(2, 0, -2, 0, 58793),
        LunarTerm(2, -1, -1, 0, 57066),
        LunarTerm(2, 0, 1, 0, 53322),
        LunarTerm(2, -1, 0, 0, 45758),
        LunarTerm(0, 1, -1, 0, -40923),
        LunarTerm(1, 0, 0, 0, -34720),
        LunarTerm(0, 1, 1, 0, -30383),
        LunarTerm(2, 0, 0, -2, 15327),
        LunarTerm(0, 0, 1, 2, -12528),
        LunarTerm(0, 0, 1, -2, 10980),
        LunarTerm(4, 0, -1, 0, 10675),
        LunarTerm(0, 0, 3, 0, 10034),
        LunarTerm(4, 0, -2, 0, 8548),
        LunarTerm(2, 1, -1, 0, -7888),
        LunarTerm(2, 1, 0, 0, -6766),
        LunarTerm(1, 0, -1, 0, -5163),
        LunarTerm(1, 1, 0, 0, 4987),
        LunarTerm(2, -1, 1, 0, 4036),
        LunarTerm(2, 0, 2, 0, 3994),
        LunarTerm(4, 0, 0, 0, 3861),
        LunarTerm(2, 0, -3, 0, 3665),
        LunarTerm(0, 1, -2, 0, -2689),
        LunarTerm(2, 0, -1, 2, -2602),
        LunarTerm(2, -1, -2, 0, 2390),
        LunarTerm(1, 0, 1, 0, -2348),
        LunarTerm(2, -2, 0, 0, 2236),
        LunarTerm(0, 1, 2, 0, -2120),
        LunarTerm(0, 2, 0, 0, -2069),
        LunarTerm(2, -2, -1, 0, 2048),
        LunarTerm(2, 0, 1, -2, -1773),
        LunarTerm(2, 0, 0, 2, -1595),
        LunarTerm(4, -1, -1, 0, 1215),
        LunarTerm(0, 0, 2, 2, -1110),
        LunarTerm(3, 0, -1, 0, -892),
        LunarTerm(2, 1, 1, 0, -810),
        LunarTerm(4, -1, -2, 0, 759),
        LunarTerm(0, 2, -1, 0, -713),
        LunarTerm(2, 2, -1, 0, -700),
        LunarTerm(2, 1, -2, 0, 691),
        LunarTerm(2, -1, 0, -2, 596),
        LunarTerm(4, 0, 1, 0, 549),
        LunarTerm(0, 0, 4, 0, 537),
        LunarTerm(4, -1, 0, 0, 520),
        LunarTerm(1, 0, -2, 0, -487),
        LunarTerm(2, 1, 0, -2, -399),
        LunarTerm(0, 0, 2, -2, -381),
        LunarTerm(1, 1, 1, 0, 351),
        LunarTerm(3, 0, -2, 0, -340),
        LunarTerm(4, 0, -3, 0, 330),
        LunarTerm(2, -1, 2, 0, 327),
        LunarTerm(0, 2, 1, 0, -323),
        LunarTerm(1, 1, -1, 0, 299),
        LunarTerm(2, 0, 3, 0, 294)
    )

    fun calculateTithi(bsDate: BsDate, adDate: LocalDate = BsDateConverter.bsToAd(bsDate)): TithiInfo {
        val jd = toJulianDay(adDate.year, adDate.monthValue, adDate.dayOfMonth, 0.0)
        val sunLon = sunApparentLongitude(jd)
        val moonLon = moonApparentLongitude(jd)
        val elongation = normalizeDegrees(moonLon - sunLon)

        val overrideTithiNumber = PANCHANG_OVERRIDES[bsDate.formatIso()]
        val tithiNumber = overrideTithiNumber ?: (floor(elongation / 12.0).toInt().coerceIn(0, 29) + 1)
        return buildTithiInfo(tithiNumber, elongation)
    }

    fun buildTithiInfo(tithiNumber: Int, elongationDeg: Double = (tithiNumber - 1) * 12.0): TithiInfo {
        val idx = (tithiNumber - 1).coerceIn(0, 29)
        val paksha = if (idx < 15) Paksha.SHUKLA else Paksha.KRISHNA
        val pakshaDay = if (idx < 15) idx + 1 else idx - 14
        return TithiInfo(
            number = idx + 1,
            pakshaDay = pakshaDay,
            paksha = paksha,
            englishName = TITHI_NAMES_EN[idx],
            nepaliName = TITHI_NAMES_NE[idx],
            shortLabelEn = TITHI_SHORT_EN[idx],
            shortLabelNe = TITHI_SHORT_NE[idx],
            elongationDeg = elongationDeg
        )
    }

    private fun toJulianDay(year: Int, month: Int, day: Int, hourUtc: Double): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2.0 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) +
            floor(30.6001 * (m + 1)) +
            day + (hourUtc / 24.0) + b - 1524.5
    }

    private fun sunApparentLongitude(jd: Double): Double {
        val t = (jd - 2451545.0) / 36525.0
        val l0 = normalizeDegrees(280.46646 + 36000.76983 * t + 0.0003032 * t * t)
        val m = normalizeDegrees(357.52911 + 35999.05029 * t - 0.0001537 * t * t)
        val mRad = Math.toRadians(m)
        val c = (1.914602 - 0.004817 * t - 0.000014 * t * t) * sin(mRad) +
            (0.019993 - 0.000101 * t) * sin(2 * mRad) +
            0.000289 * sin(3 * mRad)
        val trueLong = l0 + c
        val omega = 125.04 - 1934.136 * t
        val apparent = trueLong - 0.00569 - 0.00478 * sin(Math.toRadians(omega))
        return normalizeDegrees(apparent)
    }

    private fun moonApparentLongitude(jd: Double): Double {
        val t = (jd - 2451545.0) / 36525.0
        val t2 = t * t
        val t3 = t2 * t
        val t4 = t3 * t

        val lp = normalizeDegrees(218.3164477 + 481267.88123421 * t - 0.0015786 * t2 + t3 / 538841.0 - t4 / 65194000.0)
        val d = normalizeDegrees(297.8501921 + 445267.1114034 * t - 0.0018819 * t2 + t3 / 545868.0 - t4 / 113065000.0)
        val m = normalizeDegrees(357.5291092 + 35999.0502909 * t - 0.0001536 * t2 + t3 / 24490000.0)
        val mp = normalizeDegrees(134.9633964 + 477198.8675055 * t + 0.0087414 * t2 + t3 / 69699.0 - t4 / 14712000.0)
        val f = normalizeDegrees(93.2720950 + 483202.0175233 * t - 0.0036539 * t2 - t3 / 3526000.0 + t4 / 863310000.0)

        val e = 1.0 - 0.002516 * t - 0.0000074 * t2

        var sigmaL = 0.0
        for (term in LUNAR_TERMS) {
            val argRad = Math.toRadians(term.d * d + term.m * m + term.mp * mp + term.f * f)
            val eccFactor = when (abs(term.m)) {
                1 -> e
                2 -> e * e
                else -> 1.0
            }
            sigmaL += term.coeff * eccFactor * sin(argRad)
        }

        val a1 = normalizeDegrees(119.75 + 131.849 * t)
        val a2 = normalizeDegrees(53.09 + 479264.290 * t)
        sigmaL += 3958.0 * sin(Math.toRadians(a1)) +
            1962.0 * sin(Math.toRadians(lp - f)) +
            318.0 * sin(Math.toRadians(a2))

        return normalizeDegrees(lp + sigmaL / 1_000_000.0)
    }

    private fun normalizeDegrees(deg: Double): Double {
        val rem = deg % 360.0
        return if (rem < 0) rem + 360.0 else rem
    }
}
