package np.com.miti.patro.domain.engine

import np.com.miti.patro.domain.model.BsDate
import np.com.miti.patro.domain.model.FestivalInfo
import np.com.miti.patro.domain.model.TithiInfo
import java.time.LocalDate

/**
 * Offline Nepali National & Cultural Festival Engine (BS 1970..2100).
 *
 * Combines:
 * 1. Fixed Solar BS Festivals (e.g., Baisakh 1 New Year, Asar 15 Paddy Day, Maghe Sankranti).
 * 2. Fixed Gregorian (AD) Holidays (e.g., March 8 Women's Day, May 1 Labour Day).
 * 3. Pre-verified Nepal Panchanga Nirnayak Samiti table for BS 2083.
 * 4. True Amanta Lunar Month (Chandra Maas) + Tithi Kshaya / Vriddhi + Adhik Maas (मलमास)
 *    + Pradosh / Nishita Kala astronomical rules for all other years (1970..2100 BS).
 */
object NepaliFestivals {

    private val FIXED_BS_FESTIVALS: Map<Pair<Int, Int>, List<FestivalInfo>> = mapOf(
        (1 to 1) to listOf(
            FestivalInfo("Nepali New Year / Biska Jatra", "नयाँ वर्ष / बिस्का जात्रा", isPublicHoliday = true, isLunar = false)
        ),
        (1 to 11) to listOf(
            FestivalInfo("Loktantra Diwas", "लोकतन्त्र दिवस", isPublicHoliday = false, isLunar = false)
        ),
        (2 to 15) to listOf(
            FestivalInfo("Ganatantra Diwas (Republic Day)", "गणतन्त्र दिवस", isPublicHoliday = true, isLunar = false)
        ),
        (3 to 15) to listOf(
            FestivalInfo("National Paddy Day / Dahi Chiura", "धान दिवस / दही चिउरा खाने दिन", isPublicHoliday = false, isLunar = false)
        ),
        (3 to 29) to listOf(
            FestivalInfo("Bhanu Jayanti", "भानु जयन्ती", isPublicHoliday = false, isLunar = false)
        ),
        (4 to 1) to listOf(
            FestivalInfo("Shrawan Sankranti / Luto Fyakne", "साउन संक्रान्ति / लुतो फ्याँक्ने", isPublicHoliday = false, isLunar = false)
        ),
        (4 to 15) to listOf(
            FestivalInfo("Kheer Khane Din", "खीर खाने दिन", isPublicHoliday = false, isLunar = false)
        ),
        (6 to 1) to listOf(
            FestivalInfo("Bishwakarma Puja", "विश्वकर्मा पूजा", isPublicHoliday = false, isLunar = false)
        ),
        (6 to 3) to listOf(
            FestivalInfo("Constitution Day (Sambidhan Diwas)", "संविधान दिवस", isPublicHoliday = true, isLunar = false)
        ),
        (9 to 15) to listOf(
            FestivalInfo("Tamu Lhosar", "तमु ल्होसार", isPublicHoliday = true, isLunar = false)
        ),
        (9 to 27) to listOf(
            FestivalInfo("Prithvi Jayanti / National Unity Day", "पृथ्वी जयन्ती / राष्ट्रिय एकता दिवस", isPublicHoliday = true, isLunar = false)
        ),
        (10 to 1) to listOf(
            FestivalInfo("Maghe Sankranti / Maghi Parva", "माघे संक्रान्ति / माघी पर्व", isPublicHoliday = true, isLunar = false)
        ),
        (10 to 16) to listOf(
            FestivalInfo("Sahid Diwas (Martyrs' Day)", "शहीद दिवस", isPublicHoliday = false, isLunar = false)
        ),
        (11 to 7) to listOf(
            FestivalInfo("Prajatantra Diwas (Democracy Day)", "प्रजातन्त्र दिवस", isPublicHoliday = true, isLunar = false)
        )
    )

    private val FIXED_AD_FESTIVALS: Map<Pair<Int, Int>, List<FestivalInfo>> = mapOf(
        (3 to 8) to listOf(
            FestivalInfo("International Women's Day", "अन्तर्राष्ट्रिय नारी दिवस", isPublicHoliday = true, isLunar = false)
        ),
        (5 to 1) to listOf(
            FestivalInfo("International Labour Day", "अन्तर्राष्ट्रिय श्रमिक दिवस", isPublicHoliday = true, isLunar = false)
        )
    )

    private val VERIFIED_YEAR_FESTIVALS: Map<String, List<FestivalInfo>> = mapOf(
        "2083-01-04" to listOf(FestivalInfo("Mata Tirtha Aunsi (Mother's Day)", "मातातीर्थ औंसी", isPublicHoliday = false, isLunar = true)),
        "2083-01-07" to listOf(FestivalInfo("Akshaya Tritiya", "अक्षय तृतीया", isPublicHoliday = false, isLunar = true)),
        "2083-01-18" to listOf(FestivalInfo("Buddha Jayanti / Ubhauli Parva", "बुद्ध जयन्ती / उभौली पर्व", isPublicHoliday = true, isLunar = true)),
        "2083-02-03" to listOf(FestivalInfo("Adhik Maas (Malamas) Starts", "अधिक मास (मलमास) सुरु", isPublicHoliday = false, isLunar = true)),
        "2083-03-06" to listOf(FestivalInfo("Kumar Shasthi / Sithi Nakha", "कुमार षष्ठी / सिथि नखः", isPublicHoliday = false, isLunar = true)),
        "2083-04-09" to listOf(FestivalInfo("Harishayani Ekadashi (Tulasi Ropne)", "हरिशयनी एकादशी", isPublicHoliday = false, isLunar = true)),
        "2083-04-13" to listOf(FestivalInfo("Guru Purnima", "गुरु पूर्णिमा", isPublicHoliday = false, isLunar = true)),
        "2083-05-01" to listOf(FestivalInfo("Nag Panchami", "नाग पञ्चमी", isPublicHoliday = false, isLunar = true)),
        "2083-05-12" to listOf(FestivalInfo("Janai Purnima / Raksha Bandhan", "जनै पूर्णिमा / रक्षा बन्धन", isPublicHoliday = true, isLunar = true)),
        "2083-05-13" to listOf(FestivalInfo("Gai Jatra", "गाईजात्रा", isPublicHoliday = true, isLunar = true)),
        "2083-05-19" to listOf(FestivalInfo("Shree Krishna Janmashtami / Gaura Parva", "श्रीकृष्ण जन्माष्टमी / गौरा पर्व", isPublicHoliday = true, isLunar = true)),
        "2083-05-26" to listOf(FestivalInfo("Kushe Aunsi (Father's Day)", "कुशे औंसी (बुबाको मुख हेर्ने दिन)", isPublicHoliday = false, isLunar = true)),
        "2083-05-29" to listOf(FestivalInfo("Haritalika Teej", "हरितालिका तीज", isPublicHoliday = true, isLunar = true)),
        "2083-05-31" to listOf(FestivalInfo("Rishi Panchami", "ऋषि पञ्चमी", isPublicHoliday = false, isLunar = true)),
        "2083-06-09" to listOf(FestivalInfo("Indra Jatra", "इन्द्रजात्रा", isPublicHoliday = true, isLunar = true)),
        "2083-06-11" to listOf(FestivalInfo("Sorha Shraddha Starts (Pitri Paksha)", "सोह्र श्राद्ध सुरु", isPublicHoliday = false, isLunar = true)),
        "2083-06-18" to listOf(FestivalInfo("Jitiya Parva", "जितिया पर्व", isPublicHoliday = false, isLunar = true)),
        "2083-06-25" to listOf(FestivalInfo("Ghatasthapana (Dashain Starts)", "घटस्थापना", isPublicHoliday = true, isLunar = true)),
        "2083-06-31" to listOf(FestivalInfo("Fulpati (Dashain)", "फूलपाती", isPublicHoliday = true, isLunar = true)),
        "2083-07-01" to listOf(FestivalInfo("Maha Ashtami / Kaal Ratri", "महाष्टमी / कालरात्री", isPublicHoliday = true, isLunar = true)),
        "2083-07-02" to listOf(FestivalInfo("Kuchhi Bhwaya / Dashain Holiday", "कुछी भ्वय / दशैं बिदा", isPublicHoliday = true, isLunar = true)),
        "2083-07-03" to listOf(FestivalInfo("Maha Nawami", "महानवमी", isPublicHoliday = true, isLunar = true)),
        "2083-07-04" to listOf(FestivalInfo("Vijaya Dashami (Dashain Tika)", "विजया दशमी", isPublicHoliday = true, isLunar = true)),
        "2083-07-05" to listOf(FestivalInfo("Papankusha Ekadashi / Dashain Holiday", "पापांकुशा एकादशी / दशैं बिदा", isPublicHoliday = true, isLunar = true)),
        "2083-07-06" to listOf(FestivalInfo("Dashain Holiday (Dwadashi)", "दशैं बिदा (द्वादशी)", isPublicHoliday = true, isLunar = true)),
        "2083-07-08" to listOf(FestivalInfo("Kojagrat Purnima", "कोजाग्रत पूर्णिमा", isPublicHoliday = false, isLunar = true)),
        "2083-07-21" to listOf(FestivalInfo("Kaag Tihar", "काग तिहार", isPublicHoliday = false, isLunar = true)),
        "2083-07-22" to listOf(FestivalInfo("Kukur Tihar / Laxmi Puja", "कुकुर तिहार / लक्ष्मी पूजा", isPublicHoliday = true, isLunar = true)),
        "2083-07-23" to listOf(FestivalInfo("Gai Tihar", "गाई तिहार", isPublicHoliday = true, isLunar = true)),
        "2083-07-24" to listOf(FestivalInfo("Mha Puja / Gobardhan Puja / Nepal Sambat 1147", "म्हः पूजा / गोवर्द्धन पूजा / ने.सं. ११४७", isPublicHoliday = true, isLunar = true)),
        "2083-07-25" to listOf(FestivalInfo("Bhai Tika / Kija Puja", "भाइटीका / किजा पूजा", isPublicHoliday = true, isLunar = true)),
        "2083-07-26" to listOf(FestivalInfo("Tihar Holiday", "तिहार बिदा", isPublicHoliday = true, isLunar = true)),
        "2083-07-29" to listOf(FestivalInfo("Chhath Parva", "छठ पर्व", isPublicHoliday = true, isLunar = true)),
        "2083-08-04" to listOf(FestivalInfo("Haribodhini Ekadashi (Tulasi Bibaha)", "हरिबोधिनी एकादशी", isPublicHoliday = false, isLunar = true)),
        "2083-08-08" to listOf(FestivalInfo("Sakimana Punhi / Kartik Snan Ends", "सकिमना पुन्हि", isPublicHoliday = false, isLunar = true)),
        "2083-08-28" to listOf(FestivalInfo("Vivaha Panchami", "विवाह पञ्चमी", isPublicHoliday = false, isLunar = true)),
        "2083-09-09" to listOf(FestivalInfo("Yomari Punhi / Udhauli Parva / Dhanya Purnima", "य:मरि पुन्हि / उधौली पर्व / धान्य पूर्णिमा", isPublicHoliday = true, isLunar = true)),
        "2083-10-24" to listOf(FestivalInfo("Sonam Lhosar", "सोनाम ल्होसार", isPublicHoliday = true, isLunar = true)),
        "2083-10-28" to listOf(FestivalInfo("Basanta Panchami / Saraswati Puja", "वसन्त पञ्चमी / सरस्वती पूजा", isPublicHoliday = false, isLunar = true)),
        "2083-11-22" to listOf(FestivalInfo("Maha Shivaratri / Army Day", "महाशिवरात्री / सेना दिवस", isPublicHoliday = true, isLunar = true)),
        "2083-11-25" to listOf(FestivalInfo("Gyalpo Lhosar", "ग्याल्पो ल्होसार", isPublicHoliday = true, isLunar = true)),
        "2083-12-07" to listOf(FestivalInfo("Fagu Purnima (Holi - Hills)", "फागु पूर्णिमा (पहाड)", isPublicHoliday = true, isLunar = true)),
        "2083-12-08" to listOf(FestivalInfo("Fagu Purnima (Holi - Terai)", "फागु पूर्णिमा (तराई)", isPublicHoliday = true, isLunar = true)),
        "2083-12-23" to listOf(FestivalInfo("Ghode Jatra", "घोडेजात्रा", isPublicHoliday = true, isLunar = true))
    )

    fun getFestivalsForDay(bsDate: BsDate, tithi: TithiInfo): List<FestivalInfo> {
        val result = mutableListOf<FestivalInfo>()
        FIXED_BS_FESTIVALS[bsDate.month to bsDate.day]?.let { result.addAll(it) }

        val adDate = BsDateConverter.bsToAd(bsDate)
        FIXED_AD_FESTIVALS[adDate.monthValue to adDate.dayOfMonth]?.forEach { f ->
            if (result.none { it.nameEn == f.nameEn }) result.add(f)
        }

        if (bsDate.year == 2083) {
            val isoKey = bsDate.formatIso()
            VERIFIED_YEAR_FESTIVALS[isoKey]?.forEach { f ->
                if (result.none { it.nameEn == f.nameEn }) result.add(f)
            }
            return result
        }

        val lunarRuleFestivals = evaluateTrueLunarFestivalRules(adDate, tithi)
        for (f in lunarRuleFestivals) {
            if (result.none { it.nameEn == f.nameEn }) result.add(f)
        }
        return result
    }

    /**
     * Evaluates lunar festivals using the true Amanta Lunar Month (tithi.shuddhaLunarMonth),
     * including any Kshaya Tithi that fell on this solar day (even across an Adhik Maas boundary),
     * Kshaya Maas merged months, and Pradosh/Nishita Kala timing for evening/midnight festivals.
     */
    private fun evaluateTrueLunarFestivalRules(adDate: LocalDate, tithi: TithiInfo): List<FestivalInfo> {
        val list = mutableListOf<FestivalInfo>()

        if ((tithi.isAdhikMaas && tithi.number == 1 && !tithi.isVriddhiSecondDay) ||
            (!tithi.isAdhikMaas && tithi.kshayaTithiNumber == 1 && tithi.kshayaTithiIsAdhikMaas)
        ) {
            list.add(FestivalInfo("Adhik Maas (Malamas) Starts", "अधिक मास (मलमास) सुरु", isPublicHoliday = false, isLunar = true))
        }

        val candidateTithis = mutableListOf<Pair<Int, Int>>()
        if (!tithi.isAdhikMaas && !tithi.isVriddhiSecondDay) {
            candidateTithis.add(tithi.shuddhaLunarMonth to tithi.number)
            tithi.kshayaLunarMonth?.let { kMonth ->
                candidateTithis.add(kMonth to tithi.number)
            }
        }
        tithi.kshayaTithiNumber?.let { kTithi ->
            if (!tithi.kshayaTithiIsAdhikMaas) {
                val kLm = tithi.kshayaTithiLunarMonth ?: tithi.shuddhaLunarMonth
                candidateTithis.add(kLm to kTithi)
                if (kTithi != 1) {
                    tithi.kshayaLunarMonth?.let { kMonth ->
                        candidateTithis.add(kMonth to kTithi)
                    }
                }
            }
        }

        for ((lunarMonth, tNum) in candidateTithis) {
            when (lunarMonth) {
                12 -> {
                    // Chaitra Amanta (ends with Mata Tirtha Aunsi)
                    if (tNum == 8) list.add(FestivalInfo("Chaite Dashain", "चैते दशैं", false, true))
                    if (tNum == 9) list.add(FestivalInfo("Ram Nawami", "राम नवमी", true, true))
                    if (tNum == 30) list.add(FestivalInfo("Mata Tirtha Aunsi (Mother's Day)", "मातातीर्थ औंसी", false, true))
                }
                1 -> {
                    // Baisakh Amanta
                    if (tNum == 3) list.add(FestivalInfo("Akshaya Tritiya", "अक्षय तृतीया", false, true))
                    if (tNum == 15) list.add(FestivalInfo("Buddha Jayanti / Ubhauli Parva", "बुद्ध जयन्ती / उभौली पर्व", true, true))
                }
                2 -> {
                    // Jestha Amanta
                    if (tNum == 6) list.add(FestivalInfo("Kumar Shasthi / Sithi Nakha", "कुमार षष्ठी / सिथि नखः", false, true))
                }
                3 -> {
                    // Ashadh Amanta
                    if (tNum == 11) list.add(FestivalInfo("Harishayani Ekadashi (Tulasi Ropne)", "हरिशयनी एकादशी", false, true))
                    if (tNum == 15) list.add(FestivalInfo("Guru Purnima", "गुरु पूर्णिमा", false, true))
                }
                4 -> {
                    // Shrawan Amanta (Shrawan Shukla + Bhadra Krishna)
                    if (tNum == 5) list.add(FestivalInfo("Nag Panchami", "नाग पञ्चमी", false, true))
                    if (tNum == 15) list.add(FestivalInfo("Janai Purnima / Raksha Bandhan", "जनै पूर्णिमा / रक्षा बन्धन", true, true))
                    if (tNum == 16) list.add(FestivalInfo("Gai Jatra", "गाईजात्रा", true, true))
                    if (tNum == 30) list.add(FestivalInfo("Kushe Aunsi (Father's Day)", "कुशे औंसी (बुबाको मुख हेर्ने दिन)", false, true))
                }
                5 -> {
                    // Bhadra Amanta (Bhadra Shukla + Ashwin Krishna / Sorha Shraddha)
                    if (tNum == 3) list.add(FestivalInfo("Haritalika Teej", "हरितालिका तीज", true, true))
                    if (tNum == 5) list.add(FestivalInfo("Rishi Panchami", "ऋषि पञ्चमी", false, true))
                    if (tNum == 14) list.add(FestivalInfo("Indra Jatra", "इन्द्रजात्रा", true, true))
                    if (tNum == 16) list.add(FestivalInfo("Sorha Shraddha Starts (Pitri Paksha)", "सोह्र श्राद्ध सुरु", false, true))
                    if (tNum == 23) list.add(FestivalInfo("Jitiya Parva", "जितिया पर्व", false, true))
                }
                6 -> {
                    // Ashwin Amanta (Dashain in Shukla Paksha + Kaag Tihar in late Krishna Paksha)
                    if (tNum == 1) list.add(FestivalInfo("Ghatasthapana (Dashain Starts)", "घटस्थापना", true, true))
                    if (tNum == 7) list.add(FestivalInfo("Fulpati (Dashain)", "फूलपाती", true, true))
                    if (tNum == 8) list.add(FestivalInfo("Maha Ashtami / Kaal Ratri", "महाष्टमी / कालरात्री", true, true))
                    if (tNum == 9) list.add(FestivalInfo("Maha Nawami", "महानवमी", true, true))
                    if (tNum == 10) list.add(FestivalInfo("Vijaya Dashami (Dashain Tika)", "विजया दशमी", true, true))
                    if (tNum == 11) list.add(FestivalInfo("Papankusha Ekadashi / Dashain Holiday", "पापांकुशा एकादशी / दशैं बिदा", true, true))
                    if (tNum == 28) list.add(FestivalInfo("Kaag Tihar", "काग तिहार", false, true))
                }
                7 -> {
                    // Kartik Amanta (Gobardhan/Mha Puja, Bhai Tika, Chhath, Haribodhini Ekadashi, Sakimana Punhi)
                    if (tNum == 1) list.add(FestivalInfo("Gobardhan Puja / Mha Puja", "गोवर्द्धन पूजा / म्हः पूजा", true, true))
                    if (tNum == 2) list.add(FestivalInfo("Bhai Tika / Kija Puja", "भाइटीका / किजा पूजा", true, true))
                    if (tNum == 6) list.add(FestivalInfo("Chhath Parva", "छठ पर्व", true, true))
                    if (tNum == 11) list.add(FestivalInfo("Haribodhini Ekadashi (Tulasi Bibaha)", "हरिबोधिनी एकादशी", false, true))
                    if (tNum == 15) list.add(FestivalInfo("Sakimana Punhi / Kartik Snan Ends", "सकिमना पुन्हि", false, true))
                }
                8 -> {
                    // Mangsir Amanta
                    if (tNum == 5) list.add(FestivalInfo("Vivaha Panchami", "विवाह पञ्चमी", false, true))
                    if (tNum == 15) list.add(FestivalInfo("Yomari Punhi / Udhauli Parva", "य:मरि पुन्हि / उधौली पर्व", true, true))
                }
                10 -> {
                    // Magh Amanta
                    if (tNum == 1) list.add(FestivalInfo("Sonam Lhosar", "सोनाम ल्होसार", true, true))
                    if (tNum == 5) list.add(FestivalInfo("Basanta Panchami / Saraswati Puja", "वसन्त पञ्चमी / सरस्वती पूजा", false, true))
                }
                11 -> {
                    // Falgun Amanta
                    if (tNum == 1) list.add(FestivalInfo("Gyalpo Lhosar", "ग्याल्पो ल्होसार", true, true))
                    if (tNum == 30) list.add(FestivalInfo("Ghode Jatra", "घोडेजात्रा", true, true))
                }
            }
        }

        // Nishita (midnight) & Pradosh (evening) festivals
        if (matchesKala(adDate, 4, 23, useNishita = true)) {
            list.add(FestivalInfo("Shree Krishna Janmashtami", "श्रीकृष्ण जन्माष्टमी", true, true))
        }
        if (matchesKala(adDate, 6, 15, useNishita = true)) {
            list.add(FestivalInfo("Kojagrat Purnima", "कोजाग्रत पूर्णिमा", false, true))
        }

        val isKukur = (6 to 29) in candidateTithis
        val isLaxmi = matchesKala(adDate, 6, 30, useNishita = false)
        val isGai = (6 to 30) in candidateTithis
        when {
            isKukur && isLaxmi && isGai ->
                list.add(FestivalInfo("Kukur Tihar / Laxmi Puja / Gai Tihar", "कुकुर तिहार / लक्ष्मी पूजा / गाई तिहार", true, true))
            isKukur && isLaxmi ->
                list.add(FestivalInfo("Kukur Tihar / Laxmi Puja", "कुकुर तिहार / लक्ष्मी पूजा", true, true))
            isLaxmi && isGai ->
                list.add(FestivalInfo("Laxmi Puja / Gai Tihar", "लक्ष्मी पूजा / गाई तिहार", true, true))
            isKukur ->
                list.add(FestivalInfo("Kukur Tihar", "कुकुर तिहार", false, true))
            isLaxmi ->
                list.add(FestivalInfo("Laxmi Puja", "लक्ष्मी पूजा", true, true))
            isGai ->
                list.add(FestivalInfo("Gai Tihar", "गाई तिहार", true, true))
        }

        if (matchesKala(adDate, 10, 29, useNishita = true)) {
            list.add(FestivalInfo("Maha Shivaratri", "महाशिवरात्री", true, true))
        }
        if (matchesKala(adDate, 11, 15, useNishita = false)) {
            list.add(FestivalInfo("Fagu Purnima (Holi - Hills)", "फागु पूर्णिमा (पहाड)", true, true))
        }
        if (matchesKala(adDate.minusDays(1), 11, 15, useNishita = false)) {
            list.add(FestivalInfo("Fagu Purnima (Holi - Terai)", "फागु पूर्णिमा (तराई)", true, true))
        }

        return list
    }

    private fun matchesKala(
        adDate: LocalDate,
        targetLunarMonth: Int,
        targetTithi: Int,
        useNishita: Boolean
    ): Boolean {
        val lun = TithiCalculator.getLunationDetails(adDate)
        if (lun.isAdhikMaas || (lun.shuddhaLunarMonth != targetLunarMonth && lun.kshayaLunarMonth != targetLunarMonth)) {
            return false
        }
        val ktNow = if (useNishita) TithiCalculator.nishitaTithi(adDate) else TithiCalculator.pradoshTithi(adDate)
        val ktPrev = if (useNishita) {
            TithiCalculator.nishitaTithi(adDate.minusDays(1))
        } else {
            TithiCalculator.pradoshTithi(adDate.minusDays(1))
        }
        val prevT = if (targetTithi == 1) 30 else targetTithi - 1
        val nextT = if (targetTithi == 30) 1 else targetTithi + 1
        return (ktNow == targetTithi && ktPrev != targetTithi) || (ktPrev == prevT && ktNow == nextT)
    }
}
