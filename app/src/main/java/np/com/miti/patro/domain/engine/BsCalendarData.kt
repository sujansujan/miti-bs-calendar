package np.com.miti.patro.domain.engine

import java.time.LocalDate

/**
 * Authoritative Bikram Sambat month-length table covering BS 1970 to BS 2100 (AD 1913 to 2044).
 *
 * Each entry maps a BS year to a 12-element IntArray of exact day counts for:
 * [Baisakh, Jestha, Ashadh, Shrawan, Bhadra, Ashwin, Kartik, Mangsir, Poush, Magh, Falgun, Chaitra].
 *
 * Base anchor: BS 2000-01-01 (1 Baisakh 2000) == AD 1943-04-14 (Wednesday).
 * Verified against the Nepal Panchanga Nirnayak Samiti published calendars and Hamro Patro / Ashesh's Patro.
 */
object BsCalendarData {

    const val MIN_YEAR = 1970
    const val MAX_YEAR = 2100

    val ANCHOR_BS_YEAR = 2000
    val ANCHOR_AD_DATE: LocalDate = LocalDate.of(1943, 4, 14)

    val MONTH_NAMES_EN = arrayOf(
        "Baisakh", "Jestha", "Ashadh", "Shrawan", "Bhadra", "Ashwin",
        "Kartik", "Mangsir", "Poush", "Magh", "Falgun", "Chaitra"
    )

    val MONTH_NAMES_SHORT_EN = arrayOf(
        "Bai", "Jes", "Ash", "Shr", "Bha", "Asw",
        "Kar", "Man", "Pou", "Mag", "Fal", "Cha"
    )

    val MONTH_NAMES_NE = arrayOf(
        "बैशाख", "जेठ", "असार", "श्रावण", "भदौ", "आश्विन",
        "कार्तिक", "मंसिर", "पौष", "माघ", "फाल्गुन", "चैत्र"
    )

    val WEEKDAY_SHORT_EN = arrayOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
    val WEEKDAY_FULL_EN = arrayOf(
        "Aaitabar (Sun)", "Sombar (Mon)", "Mangalbar (Tue)",
        "Budhabar (Wed)", "Bihibar (Thu)", "Shukrabar (Fri)", "Shanibar (Sat)"
    )
    val WEEKDAY_SHORT_NE = arrayOf("आइत", "सोम", "मंगल", "बुध", "बिहि", "शुक्र", "शनि")
    val WEEKDAY_FULL_NE = arrayOf(
        "आइतबार", "सोमबार", "मंगलबार", "बुधबार", "बिहीबार", "शुक्रबार", "शनिबार"
    )

    val AD_MONTH_SHORT_EN = arrayOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )
    val AD_MONTH_SHORT_NE = arrayOf(
        "जन", "फेब्रु", "मार्च", "अप्रिल", "मे", "जुन",
        "जुलाई", "अगस्ट", "सेप्टे", "अक्टो", "नोभे", "डिसे"
    )

    fun getAdMonthName(month1Based: Int, useDevanagari: Boolean): String {
        val idx = (month1Based - 1).coerceIn(0, 11)
        return if (useDevanagari) AD_MONTH_SHORT_NE[idx] else AD_MONTH_SHORT_EN[idx]
    }

    fun formatAdDate(adDate: LocalDate, useDevanagari: Boolean): String {
        val y = formatNumber(adDate.year, useDevanagari)
        val m = getAdMonthName(adDate.monthValue, useDevanagari)
        val d = formatNumber(adDate.dayOfMonth, useDevanagari, padWidth = 2)
        return "$y $m $d"
    }

    fun formatTime24h(hour: Int, minute: Int, useDevanagari: Boolean): String {
        val h = formatNumber(hour, useDevanagari, padWidth = 2)
        val m = formatNumber(minute, useDevanagari, padWidth = 2)
        return "$h:$m"
    }

    private val DAYS_PER_MONTH: Map<Int, IntArray> = mapOf(
        1970 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        1971 to intArrayOf(31, 31, 32, 31, 32, 30, 30, 29, 30, 29, 30, 30),
        1972 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        1973 to intArrayOf(30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        1974 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        1975 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        1976 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        1977 to intArrayOf(30, 32, 31, 32, 31, 31, 29, 30, 29, 30, 29, 31),
        1978 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        1979 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        1980 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        1981 to intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
        1982 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        1983 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        1984 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        1985 to intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
        1986 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        1987 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        1988 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        1989 to intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
        1990 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        1991 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
        1992 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        1993 to intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
        1994 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        1995 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
        1996 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        1997 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        1998 to intArrayOf(31, 31, 32, 31, 32, 30, 30, 29, 30, 29, 30, 30),
        1999 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2000 to intArrayOf(30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2001 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2002 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2003 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2004 to intArrayOf(30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2005 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2006 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2007 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2008 to intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 29, 31),
        2009 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2010 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2011 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2012 to intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
        2013 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2014 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2015 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2016 to intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
        2017 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2018 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2019 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2020 to intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
        2021 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2022 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
        2023 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2024 to intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
        2025 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2026 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2027 to intArrayOf(30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2028 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2029 to intArrayOf(31, 31, 32, 31, 32, 30, 30, 29, 30, 29, 30, 30),
        2030 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2031 to intArrayOf(30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2032 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2033 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2034 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2035 to intArrayOf(30, 32, 31, 32, 31, 31, 29, 30, 30, 29, 29, 31),
        2036 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2037 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2038 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2039 to intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
        2040 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2041 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2042 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2043 to intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
        2044 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2045 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2046 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2047 to intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
        2048 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2049 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
        2050 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2051 to intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
        2052 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2053 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
        2054 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2055 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2056 to intArrayOf(31, 31, 32, 31, 32, 30, 30, 29, 30, 29, 30, 30),
        2057 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2058 to intArrayOf(30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2059 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2060 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2061 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2062 to intArrayOf(30, 32, 31, 32, 31, 31, 29, 30, 29, 30, 29, 31),
        2063 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2064 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2065 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2066 to intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 29, 31),
        2067 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2068 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2069 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2070 to intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
        2071 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2072 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2073 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2074 to intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
        2075 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2076 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
        2077 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2078 to intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
        2079 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2080 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
        2081 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2082 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2083 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2084 to intArrayOf(31, 31, 32, 31, 31, 30, 30, 30, 29, 30, 30, 30),
        2085 to intArrayOf(31, 32, 31, 32, 30, 31, 30, 30, 29, 30, 30, 30),
        2086 to intArrayOf(30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 30, 30),
        2087 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 30, 29, 30, 30, 30),
        2088 to intArrayOf(30, 31, 32, 32, 30, 31, 30, 30, 29, 30, 30, 30),
        2089 to intArrayOf(30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 30, 30),
        2090 to intArrayOf(30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 30, 30),
        2091 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2092 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2093 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2094 to intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
        2095 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2096 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2097 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2098 to intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
        2099 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2100 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30)
    )

    private val DEFAULT_FALLBACK_MONTHS = intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30)

    fun getDaysInMonth(bsYear: Int, bsMonth: Int): Int {
        require(bsMonth in 1..12) { "BS month must be in 1..12, got $bsMonth" }
        val yearArray = DAYS_PER_MONTH[bsYear] ?: DEFAULT_FALLBACK_MONTHS
        return yearArray[bsMonth - 1]
    }

    fun getDaysInYear(bsYear: Int): Int {
        val yearArray = DAYS_PER_MONTH[bsYear] ?: DEFAULT_FALLBACK_MONTHS
        return yearArray.sum()
    }

    fun getMonthName(bsMonth: Int, useDevanagari: Boolean): String {
        val idx = (bsMonth - 1).coerceIn(0, 11)
        return if (useDevanagari) MONTH_NAMES_NE[idx] else MONTH_NAMES_EN[idx]
    }

    fun getShortMonthName(bsMonth: Int, useDevanagari: Boolean): String {
        val idx = (bsMonth - 1).coerceIn(0, 11)
        return if (useDevanagari) MONTH_NAMES_NE[idx] else MONTH_NAMES_SHORT_EN[idx]
    }

    fun formatNumber(value: Int, useDevanagari: Boolean, padWidth: Int = 0): String {
        val raw = if (padWidth > 0) "%0${padWidth}d".format(value) else value.toString()
        if (!useDevanagari) return raw
        val sb = StringBuilder(raw.length)
        for (ch in raw) {
            if (ch in '0'..'9') {
                sb.append(('०'.code + (ch.code - '0'.code)).toChar())
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun daysInMonth(bsYear: Int, bsMonth: Int): Int = getDaysInMonth(bsYear, bsMonth)

    fun monthName(bsMonth: Int, useDevanagari: Boolean): String = getMonthName(bsMonth, useDevanagari)

    fun formatNumberPadded(value: Int, useDevanagari: Boolean): String =
        formatNumber(value, useDevanagari, padWidth = 2)

    fun weekdayShort(dowIndex0Sun: Int, useDevanagari: Boolean): String {
        val idx = dowIndex0Sun.coerceIn(0, 6)
        return if (useDevanagari) WEEKDAY_SHORT_NE[idx] else WEEKDAY_SHORT_EN[idx]
    }

    fun weekdayFull(dowIndex0Sun: Int, useDevanagari: Boolean): String {
        val idx = dowIndex0Sun.coerceIn(0, 6)
        return if (useDevanagari) WEEKDAY_FULL_NE[idx] else WEEKDAY_FULL_EN[idx]
    }
}
