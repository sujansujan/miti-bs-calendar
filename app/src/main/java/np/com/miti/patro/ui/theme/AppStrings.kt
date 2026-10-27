package np.com.miti.patro.ui.theme

import np.com.miti.patro.domain.engine.BsCalendarData

/**
 * Complete bilingual string dictionary (English / Nepali Devanagari).
 * When Nepali is selected in Settings, 100% of UI headers, buttons, table labels,
 * dialogs, reminder descriptions, and sync messages are rendered in Nepali.
 */
object AppStrings {

    // Top Navigation Bar
    fun prevYearBtn(ne: Boolean) = if (ne) "[<<वर्ष]" else "[<<YR]"
    fun nextYearBtn(ne: Boolean) = if (ne) "[वर्ष>>]" else "[YR>>]"
    fun todayBtn(ne: Boolean) = if (ne) "[आज]" else "[TODAY]"
    fun addEventTopBtn(ne: Boolean) = if (ne) "[+कार्यक्रम]" else "[+EVENT]"
    fun settingsBtn(ne: Boolean) = if (ne) "[सेटिङ]" else "[CFG]"
    fun backBtn(ne: Boolean) = if (ne) "[< पात्रो]" else "[< CALENDAR]"

    // Selected Day Detail Box
    fun saturdayHolidayBadge(ne: Boolean) = if (ne) "[शनिबार सार्वजनिक बिदा]" else "[SATURDAY PUBLIC HOLIDAY]"
    fun publicHolidayTag(ne: Boolean) = if (ne) "[बिदा] *" else "[HOLIDAY] *"
    fun festivalTag(ne: Boolean) = if (ne) "[पर्व] *" else "[FESTIVAL] *"
    fun addRepeatingBsEventBtn(ne: Boolean) = if (ne) "[+ थप्नुहोस्]" else "[+ ADD]"

    // Repeating Events Section
    fun repeatingEventsHeader(ne: Boolean) =
        if (ne) "यस दिनका वार्षिक बि.सं. कार्यक्रमहरू:" else "REPEATING BS EVENTS ON THIS DAY:"
    fun noRepeatingEventsMsg(ne: Boolean) =
        if (ne) "यस मितिमा कुनै व्यक्तिगत वार्षिक कार्य छैन।" else "No repeating BS events on this date."
    fun clampedDayTag(ne: Boolean, origDayStr: String) =
        if (ne) " [मूल $origDayStr गते]" else " [~BS day $origDayStr]"
    fun personalHolidayTag(ne: Boolean) =
        if (ne) " [व्यक्तिगत बिदा]" else " [HOLIDAY]"
    fun alarmSummary(
        ne: Boolean,
        enabled: Boolean,
        timeStr: String,
        daysBefore: Int,
        startYearStr: String
    ): String {
        return if (ne) {
            if (enabled) {
                val offset = if (daysBefore == 0) "सोही दिन" else "${BsCalendarData.formatNumber(daysBefore, true)} दिन अघि"
                "अलार्म: $timeStr ($offset) • सुरु: वि.सं. $startYearStr"
            } else {
                "अलार्म: बन्द • सुरु: वि.सं. $startYearStr"
            }
        } else {
            if (enabled) {
                val offset = if (daysBefore == 0) "same day" else "${daysBefore}d before"
                "Alarm: $timeStr ($offset) • Since $startYearStr BS"
            } else {
                "Alarm: OFF • Since $startYearStr BS"
            }
        }
    }
    fun editBtn(ne: Boolean) = if (ne) "[सच्याउने]" else "[EDIT]"
    fun deleteBtn(ne: Boolean) = if (ne) "[हटाउने]" else "[DEL]"
    fun exportGCalBtn(ne: Boolean) = if (ne) "[+गुगल]" else "[+GCAL]"

    // Month Summary Box
    fun monthScheduleHeader(ne: Boolean, monthName: String, yearStr: String) =
        if (ne) "$monthName $yearStr का कार्य र चाडपर्वहरू" else "${monthName.uppercase()} $yearStr SCHEDULE (BS EVENTS & FESTIVALS)"
    fun noMonthScheduleMsg(ne: Boolean) =
        if (ne) "यस महिनामा कुनै प्रमुख कार्य वा पर्व सूचिकृत छैन।" else "No events or festivals recorded for this month."
    fun personalRepeatingSubheader(ne: Boolean) =
        if (ne) "व्यक्तिगत वार्षिक कार्यहरू:" else "REPEATING BS EVENTS:"
    fun festivalsSubheader(ne: Boolean) =
        if (ne) "प्रमुख चाडपर्व र सार्वजनिक बिदाहरू:" else "FESTIVALS & PUBLIC HOLIDAYS:"
    fun fromDaySuffix(ne: Boolean, origDayStr: String) =
        if (ne) " (मूल $origDayStr गतेबाट)" else " (from day $origDayStr)"

    // Event Editor Dialog
    fun addEventDialogHeader(ne: Boolean) =
        if (ne) "नयाँ वार्षिक बि.सं. कार्यक्रम थप्नुहोस्" else "ADD REPEATING BS EVENT"
    fun editEventDialogHeader(ne: Boolean) =
        if (ne) "वार्षिक बि.सं. कार्यक्रम सच्याउनुहोस्" else "EDIT REPEATING BS EVENT"
    fun eventTitleLabel(ne: Boolean) = if (ne) "कार्यक्रमको नाम (अनिवार्य):" else "EVENT TITLE (REQUIRED):"
    fun eventTitlePlaceholder(ne: Boolean) =
        if (ne) "जस्तै: बुबाको जन्मदिन / श्राद्ध तिथि" else "e.g., Baba's Birthday / Shraddha Tithi"
    fun eventTitleError(ne: Boolean) =
        if (ne) "कृपया कार्यक्रमको नाम लेख्नुहोस्।" else "Please enter an event title."
    fun repeatsEveryBsYearOn(ne: Boolean) =
        if (ne) "प्रत्येक बि.सं. वर्ष दोहोरिने महिना र गते:" else "REPEATS EVERY BS YEAR ON:"
    fun bsDayLabel(ne: Boolean) = if (ne) "गते:" else "BS Day:"
    fun fromYearLabel(ne: Boolean) = if (ne) "सुरु वर्ष:" else "From:"
    fun shortMonthNote(ne: Boolean, monthName: String, dayStr: String, ruleLabel: String) =
        if (ne) "नोट: बि.सं. महिनाहरू २९–३२ दिनका हुन्छन्। $monthName महिनामा $dayStr गते नपर्ने वर्षमा '$ruleLabel' नियम लागू हुन्छ।"
        else "Note: BS month lengths vary (29–32 days). In years where $monthName has fewer than $dayStr days, rule '$ruleLabel' applies."
    fun notesLabel(ne: Boolean) = if (ne) "विवरण (ऐच्छिक):" else "NOTE (OPTIONAL):"
    fun notesPlaceholder(ne: Boolean) = if (ne) "थप विवरण..." else "Optional details..."
    fun markAsHolidayToggle(ne: Boolean) =
        if (ne) "व्यक्तिगत बिदाका रूपमा रातो रङमा देखाउने" else "Mark as Personal Holiday (Red Date)"
    fun offlineNotificationToggle(ne: Boolean) =
        if (ne) "अफलाइन रिमाइन्डर सूचना (अलार्म)" else "Offline Notification / Reminder"
    fun notifyWhenLabel(ne: Boolean) = if (ne) "सूचना दिन:" else "Notify:"
    fun notifyOffsetOption(ne: Boolean, days: Int) = when (days) {
        0 -> if (ne) "सोही दिन" else "Same day"
        1 -> if (ne) "१ दिन अघि" else "1d prior"
        else -> if (ne) "${BsCalendarData.formatNumber(days, true)} दिन अघि" else "${days}d prior"
    }
    fun time24hLabel(ne: Boolean) = if (ne) "समय (२४-घण्टा):" else "Time (24h):"
    fun minusHourBtn(ne: Boolean) = if (ne) "[-घण्टा]" else "[-H]"
    fun plusHourBtn(ne: Boolean) = if (ne) "[+घण्टा]" else "[+H]"
    fun plus15MinBtn(ne: Boolean) = if (ne) "[+१५मि]" else "[+15m]"
    fun minus15MinBtn(ne: Boolean) = if (ne) "[-१५मि]" else "[-15m]"
    fun cancelBtn(ne: Boolean) = if (ne) "[रद्द]" else "[CANCEL]"
    fun saveEventBtn(ne: Boolean) = if (ne) "[सुरक्षित गर्नुहोस्]" else "[SAVE BS EVENT]"

    // Year / Month Jump Dialog
    fun jumpDialogHeader(ne: Boolean) =
        if (ne) {
            val min = BsCalendarData.formatNumber(BsCalendarData.MIN_YEAR, true)
            val max = BsCalendarData.formatNumber(BsCalendarData.MAX_YEAR, true)
            "बि.सं. वर्ष र महिनामा जानुहोस् ($min–$max)"
        } else {
            "JUMP TO BS YEAR & MONTH (1970–2100)"
        }
    fun minus10YBtn(ne: Boolean) = if (ne) "[-१०व]" else "[-10y]"
    fun minus1YBtn(ne: Boolean) = if (ne) "[-१व]" else "[-1y]"
    fun plus1YBtn(ne: Boolean) = if (ne) "[+१व]" else "[+1y]"
    fun plus10YBtn(ne: Boolean) = if (ne) "[+१०व]" else "[+10y]"
    fun goBtn(ne: Boolean) = if (ne) "[जानुहोस्]" else "[GO]"

    // Settings Screen
    fun settingsHeaderTitle(ne: Boolean) = if (ne) "सेटिङहरू" else "SETTINGS"
    fun sec1ThemeHeader(ne: Boolean) =
        if (ne) "१. थिम र रङ (आलुकार्ड / ड्राकुला)" else "1. THEME PALETTE (ALUCARD / DRACULA)"
    fun activeSurfaceLabel(ne: Boolean, isDark: Boolean) =
        if (ne) {
            if (isDark) "सक्रिय थिम: ड्राकुला अँध्यारो (#282A36)" else "सक्रिय थिम: आलुकार्ड उज्यालो (#FFFBEB)"
        } else {
            if (isDark) "Active surface: Dracula (#282A36)" else "Active surface: Alucard (#FFFBEB)"
        }
    fun sec2MissingDayHeader(ne: Boolean) =
        if (ne) "२. महिना छोटो हुँदाको नियम (३०–३२ गते)" else "2. VARIABLE BS MONTH LENGTH RULE (DAY 30–32)"
    fun missingDayRuleIntro(ne: Boolean) =
        if (ne) "बि.सं. महिनाहरू वर्ष अनुसार २९ देखि ३२ दिनका हुन्छन्। ३०, ३१ वा ३२ गतेको वार्षिक कार्यक्रम छोटो महिनामा पर्दा के गर्ने छान्नुहोस्:"
        else "BS months vary between 29 and 32 days depending on the year. Choose how repeating events on day 30, 31, or 32 behave when an upcoming year's month is shorter:"
    fun sec3DisplayHeader(ne: Boolean) =
        if (ne) "३. पात्रो दृश्य र भाषा" else "3. CALENDAR DISPLAY & LANGUAGE"
    fun useDevanagariToggle(ne: Boolean) =
        if (ne) "नेपाली भाषा र देवनागरी अंक (सबै ठाउँमा प्रयोग गर्ने)" else "Use Nepali Language & Devanagari Numerals Everywhere"
    fun showTithiToggle(ne: Boolean) =
        if (ne) "पात्रोको प्रत्येक कोष्ठमा दैनिक तिथि देखाउने" else "Show Daily Tithi in Every Day Cell"
    fun showAdSubDateToggle(ne: Boolean) =
        if (ne) "कोष्ठमा इस्वी संवत (AD) गते देखाउने" else "Show Gregorian (AD) Sub-Date"
    fun sec4DefaultReminderHeader(ne: Boolean) =
        if (ne) "४. पूर्वनिर्धारित अफलाइन रिमाइन्डर समय" else "4. DEFAULT OFFLINE REMINDER TIME"
    fun newEventAlarmTimeLabel(ne: Boolean) =
        if (ne) "नयाँ कार्यक्रमको अलार्म समय:" else "New Event Alarm Time:"
    fun sec5GoogleSyncHeader(ne: Boolean) =
        if (ne) "५. गुगल क्यालेन्डर सिंक (GOOGLE CALENDAR SYNC)" else "5. GOOGLE CALENDAR SYNC"
    fun googleSyncExplanation(ne: Boolean) =
        if (ne) "बि.सं. वार्षिक कार्यक्रमहरू इस्वी संवत (AD) मा प्रत्येक वर्ष फरक मितिमा पर्छन्। यो सुविधाले आगामी बि.सं. वर्षहरूका सही इस्वी मिति गणना गरी फोनको गुगल क्यालेन्डरमा स्वतः सिंक गर्छ (इन्टरनेट अनुमति बिना)।"
        else "Because BS repeating dates shift on the Gregorian calendar every year, Miti resolves the exact Gregorian dates for upcoming BS years and writes them to your device's Google Calendar provider (zero internet permission needed)."
    fun autoSyncToggle(ne: Boolean) =
        if (ne) "नयाँ/सच्याइएका बि.सं. कार्यक्रम स्वतः गुगल क्यालेन्डरमा सिंक गर्ने"
        else "Auto-sync saved BS events to Google Calendar"
    fun syncHorizonLabel(ne: Boolean, yearsStr: String) =
        if (ne) "सिंक गर्ने आगामी बि.सं. वर्ष संख्या: $yearsStr वर्ष"
        else "Upcoming BS years to project & sync: $yearsStr years"
    fun syncNowBtn(ne: Boolean) =
        if (ne) "[सबै बि.सं. कार्यक्रम अहिले गुगल क्यालेन्डरमा सिंक गर्नुहोस्]"
        else "[SYNC ALL BS EVENTS TO GOOGLE CALENDAR NOW]"
    fun syncStatusTitle(ne: Boolean) = if (ne) "गुगल क्यालेन्डर सिंक सूचना" else "GOOGLE CALENDAR SYNC STATUS"
    fun syncSuccessStatus(ne: Boolean, countStr: String, calName: String) =
        if (ne) "सफलतापूर्वक $countStr वार्षिक मितिहरू '$calName' मा सिंक गरियो।"
        else "Synced $countStr upcoming BS occurrences to '$calName'."
    fun syncPermissionDeniedStatus(ne: Boolean) =
        if (ne) "गुगल क्यालेन्डर सिंकका लागि पात्रो अनुमति (Calendar Permission) आवश्यक छ।"
        else "Calendar permission is required to sync with Google Calendar."
    fun syncNoCalendarStatus(ne: Boolean) =
        if (ne) "फोनमा कुनै लेख्न मिल्ने गुगल क्यालेन्डर भेटिएन। कृपया गुगल क्यालेन्डर एपमा खाता जोड्नुहोस् वा प्रत्येक कार्यक्रममा रहेको [+गुगल] बटन प्रयोग गर्नुहोस्।"
        else "No writable Google Calendar found on device. Sign in to Google Calendar or use [+GCAL] on individual events."
    fun syncErrorStatus(ne: Boolean) =
        if (ne) "गुगल क्यालेन्डर सिंक गर्दा त्रुटि भयो।"
        else "Error communicating with system Calendar provider."
}
