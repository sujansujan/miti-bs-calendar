package np.com.miti.patro.ui.theme

import np.com.miti.patro.domain.engine.BsCalendarData

/**
 * Complete bilingual string dictionary (English / Nepali Devanagari).
 * When Nepali is selected in Settings, 100% of UI headers, buttons, table labels,
 * dialogs, reminder descriptions, and sync messages are rendered in Nepali.
 */
object AppStrings {

    // Top Navigation Bar
    fun prevBtn(ne: Boolean) = if (ne) "[< अघिल्लो]" else "[< PREV]"
    fun nextBtn(ne: Boolean) = if (ne) "[अर्को >]" else "[NEXT >]"
    fun todayBtn(ne: Boolean) = if (ne) "[आज]" else "[TODAY]"
    fun settingsBtn(ne: Boolean) = if (ne) "[सेटिङ]" else "[CFG]"
    fun backBtn(ne: Boolean) = if (ne) "[< पछाडि]" else "[< BACK]"

    // Selected Day Detail Box
    fun saturdayHolidayBadge(ne: Boolean) = if (ne) "[शनिबार बिदा]" else "[SATURDAY HOLIDAY]"
    fun publicHolidayTag(ne: Boolean) = if (ne) "* सार्वजनिक बिदा:" else "* PUBLIC HOLIDAY:"
    fun festivalTag(ne: Boolean) = if (ne) "* पर्व:" else "* FESTIVAL:"
    fun solarTermlabel(ne: Boolean) = if (ne) "सौर्य देशान्तर" else "Solar λ"
    fun addRepeatingBsEventBtn(ne: Boolean) = if (ne) "[+ वार्षिक बि.सं. कार्यक्रम थप्नुहोस्]" else "[+ ADD REPEATING BS EVENT]"

    // Repeating Events Section
    fun repeatingEventsHeader(ne: Boolean) =
        if (ne) "यस दिनका वार्षिक बि.सं. कार्यक्रमहरू" else "REPEATING BS EVENTS ON THIS DAY"
    fun noRepeatingEventsMsg(ne: Boolean) =
        if (ne) "कुनै वार्षिक बि.सं. कार्यक्रम छैन।" else "No repeating BS events on this date."
    fun repeatsAnnuallyEvery(ne: Boolean, monthName: String, dayStr: String) =
        if (ne) "प्रत्येक वर्ष $monthName $dayStr गते दोहोरिने" else "Repeats annually every $monthName $dayStr BS"
    fun clampedDayNote(ne: Boolean, originalDayStr: String, resolvedDayStr: String) =
        if (ne) "(मूल $originalDayStr गते -> यस वर्ष $resolvedDayStr गतेमा सारिएको)"
        else "(Clamped from Day $originalDayStr -> $resolvedDayStr)"
    fun alarmInfo(ne: Boolean, enabled: Boolean, timeStr: String, strategyLabel: String) =
        if (ne) {
            val al = if (enabled) "$timeStr बजे" else "बन्द"
            "अलार्म: $al | महिना छोटो भएमा: $strategyLabel"
        } else {
            val al = if (enabled) timeStr else "OFF"
            "Alarm: $al | Short month: $strategyLabel"
        }
    fun editBtn(ne: Boolean) = if (ne) "[सच्याउनुहोस्]" else "[EDIT]"
    fun deleteBtn(ne: Boolean) = if (ne) "[हटाउनुहोस्]" else "[DEL]"
    fun exportGCalBtn(ne: Boolean) = if (ne) "[+गुगल]" else "[+GCAL]"

    // Month Holidays Summary Box
    fun holidaysInMonthHeader(ne: Boolean, monthName: String, yearStr: String) =
        if (ne) "$monthName $yearStr का बिदा र पर्वहरू" else "HOLIDAYS & FESTIVALS IN ${monthName.uppercase()} $yearStr"
    fun noFestivalsInMonthMsg(ne: Boolean) =
        if (ne) "यस महिनामा कुनै प्रमुख पर्व सूचिकृत छैन (शनिबार बाहेक)।"
        else "No major festivals listed this month (other than Saturdays)."
    fun holidayBadge(ne: Boolean) = if (ne) "[बिदा]" else "[HOLIDAY]"

    // Event Editor Dialog
    fun editBsEventHeader(ne: Boolean) =
        if (ne) "वार्षिक बि.सं. कार्यक्रम सच्याउनुहोस्" else "EDIT REPEATING BS EVENT"
    fun newBsEventHeader(ne: Boolean) =
        if (ne) "नयाँ वार्षिक बि.सं. कार्यक्रम" else "NEW REPEATING BS EVENT"
    fun anchorDateLabel(ne: Boolean, monthName: String, dayStr: String, yearStr: String) =
        if (ne) "मूल मिति: $monthName $dayStr (सुरु: $yearStr बि.सं.)"
        else "ANCHOR DATE: $monthName $dayStr (Origin: $yearStr BS)"
    fun repeatsEveryYearInfo(ne: Boolean, monthName: String, dayStr: String) =
        if (ne) "यो कार्यक्रम प्रत्येक आगामी बिक्रम सम्बत वर्षको $monthName $dayStr गते स्वतः दोहोरिन्छ।"
        else "Repeats automatically every upcoming BS year on $monthName $dayStr."
    fun eventTitleLabel(ne: Boolean) = if (ne) "कार्यक्रमको नाम *" else "EVENT TITLE *"
    fun eventTitlePlaceholder(ne: Boolean) =
        if (ne) "जस्तै: बुबाको जन्मदिन / श्राद्ध तिथि" else "e.g., Baba's Birthday / Shraddha Tithi"
    fun notesLabel(ne: Boolean) = if (ne) "विवरण (ऐच्छिक)" else "NOTES (OPTIONAL)"
    fun notesPlaceholder(ne: Boolean) = if (ne) "थप जानकारी..." else "Optional details..."
    fun shortMonthHandlingTitle(ne: Boolean, dayStr: String) =
        if (ne) "महिना छोटो हुँदाको नियम ($dayStr गते)" else "SHORT MONTH HANDLING (DAY $dayStr)"
    fun shortMonthHandlingDesc(ne: Boolean, dayStr: String) =
        if (ne) "बि.सं. महिनाहरू वर्ष अनुसार २९ देखि ३२ दिनका हुन्छन्। कुनै वर्ष $dayStr गते नपरेमा के गर्ने छान्नुहोस्:"
        else "BS month lengths vary between 29 and 32 days across years. Choose what happens when a future year's month has fewer than $dayStr days:"
    fun reminderNotificationTitle(ne: Boolean) =
        if (ne) "रिमाइन्डर सूचना (अफलाइन अलार्म)" else "REMINDER NOTIFICATION"
    fun notifyOnEventDayToggle(ne: Boolean) =
        if (ne) "कार्यक्रमको दिनमा सूचना दिने" else "Notify on event day"
    fun hourLabel(ne: Boolean, hourStr: String) =
        if (ne) "घण्टा (०-२३): $hourStr" else "Hour (0-23): $hourStr"
    fun minuteLabel(ne: Boolean, minStr: String) =
        if (ne) "मिनेट: $minStr" else "Minute: $minStr"
    fun cancelBtn(ne: Boolean) = if (ne) "[रद्द गर्नुहोस्]" else "[CANCEL]"
    fun saveBtn(ne: Boolean) = if (ne) "[सुरक्षित गर्नुहोस्]" else "[SAVE EVENT]"

    // Year / Month Jump Dialog
    fun jumpToBsYearMonthHeader(ne: Boolean) =
        if (ne) "बि.सं. वर्ष / महिनामा जानुहोस्" else "JUMP TO BS YEAR / MONTH"
    fun yearRangeLabel(ne: Boolean) =
        if (ne) {
            val min = BsCalendarData.formatNumber(BsCalendarData.MIN_YEAR, true)
            val max = BsCalendarData.formatNumber(BsCalendarData.MAX_YEAR, true)
            "वर्ष ($min - $max बि.सं.):"
        } else {
            "YEAR (${BsCalendarData.MIN_YEAR} - ${BsCalendarData.MAX_YEAR} BS):"
        }
    fun selectMonthLabel(ne: Boolean) = if (ne) "महिना छान्नुहोस्:" else "SELECT MONTH:"
    fun goBtn(ne: Boolean) = if (ne) "[जानुहोस्]" else "[GO]"

    // Settings Screen
    fun settingsTitle(ne: Boolean) = if (ne) "सेटिङहरू" else "SETTINGS"
    fun appMetadataSubtitle(ne: Boolean) =
        if (ne) "मिति (Miti) • अफलाइन बिक्रम सम्बत पात्रो" else "Miti • Offline Bikram Sambat Calendar"
    fun sec1ThemeTitle(ne: Boolean) =
        if (ne) "१. रङ र थिम (आलुकार्ड / ड्राकुला)" else "1. THEME (ALUCARD / DRACULA)"
    fun activePaletteDesc(ne: Boolean) =
        if (ne) "उज्यालोका लागि आलुकार्ड (#FFFBEB) र अँध्यारोका लागि ड्राकुला (#282A36)"
        else "Active palette follows Alucard (#FFFBEB) for Light and Dracula (#282A36) for Dark."
    fun sec2DisplayTitle(ne: Boolean) =
        if (ne) "२. पात्रो दृश्य र भाषा" else "2. CALENDAR DISPLAY & LANGUAGE"
    fun useNepaliScriptToggle(ne: Boolean) =
        if (ne) "नेपाली भाषा र देवनागरी अंक प्रयोग गर्ने (सबै ठाउँमा)"
        else "Use Nepali language & Devanagari numerals everywhere"
    fun showGregorianSubDateToggle(ne: Boolean) =
        if (ne) "कोष्ठमा इस्वी संवत (AD) गते पनि देखाउने"
        else "Show Gregorian (AD) sub-date inside cells"
    fun showTithiInGridToggle(ne: Boolean) =
        if (ne) "पात्रोको कोष्ठमा दैनिक तिथि देखाउने"
        else "Show daily Tithi inside calendar cells"
    fun sec3MissingDayTitle(ne: Boolean) =
        if (ne) "३. महिना छोटो हुँदाको पूर्वनिर्धारित नियम (२९–३२ गते)"
        else "3. DEFAULT SHORT-MONTH RULE (DAYS 29..32)"
    fun missingDayRuleDesc(ne: Boolean) =
        if (ne) "३०, ३१ वा ३२ गते राखिएको वार्षिक कार्यक्रम छोटो महिनामा पर्दा कसरी मनाउने:"
        else "Default behavior when a repeating BS event on day 30, 31, or 32 lands in a shorter BS month:"
    fun sec4ReminderTitle(ne: Boolean) =
        if (ne) "४. पूर्वनिर्धारित रिमाइन्डर समय" else "4. DEFAULT REMINDER TIME"
    fun defaultAlarmTimeLabel(ne: Boolean, timeStr: String) =
        if (ne) "नयाँ कार्यक्रमको पूर्वनिर्धारित अलार्म समय: $timeStr (२४-घण्टा)"
        else "Default alarm time for new BS events: $timeStr (24-hour)"
    fun hourMinusBtn(ne: Boolean) = if (ne) "[-घण्टा]" else "[-1h]"
    fun hourPlusBtn(ne: Boolean) = if (ne) "[+घण्टा]" else "[+1h]"
    fun minMinusBtn(ne: Boolean) = if (ne) "[-१५मि]" else "[-15m]"
    fun minPlusBtn(ne: Boolean) = if (ne) "[+१५मि]" else "[+15m]"

    // Section 5: Google Calendar Sync
    fun sec5GoogleSyncTitle(ne: Boolean) =
        if (ne) "५. गुगल क्यालेन्डर सिंक (GOOGLE CALENDAR SYNC)"
        else "5. GOOGLE CALENDAR SYNC"
    fun googleSyncDesc(ne: Boolean) =
        if (ne) "बि.सं. वार्षिक कार्यक्रमहरू इस्वी संवत (AD) मा प्रत्येक वर्ष फरक मितिमा पर्छन्। यो सुविधाले आगामी बि.सं. वर्षहरूका सही इस्वी मिति गणना गरी तपाईंको फोनको गुगल क्यालेन्डरमा स्वतः सिंक गर्छ (इन्टरनेट अनुमति बिना)।"
        else "Because BS repeating dates shift on the Gregorian calendar every year, Miti resolves the exact Gregorian dates for upcoming BS years and writes them directly to your device's Google Calendar provider (zero internet permission needed)."
    fun autoSyncToggle(ne: Boolean) =
        if (ne) "नयाँ/सच्याइएका बि.सं. कार्यक्रम स्वतः गुगल क्यालेन्डरमा सिंक गर्ने"
        else "Auto-sync saved BS events to Google Calendar"
    fun syncHorizonLabel(ne: Boolean, yearsStr: String) =
        if (ne) "सिंक गर्ने आगामी बि.सं. वर्ष संख्या: $yearsStr वर्ष"
        else "Upcoming BS years to project & sync: $yearsStr years"
    fun syncNowBtn(ne: Boolean) =
        if (ne) "[सबै बि.सं. कार्यक्रम अहिले गुगल क्यालेन्डरमा सिंक गर्नुहोस्]"
        else "[SYNC ALL BS EVENTS TO GOOGLE CALENDAR NOW]"
    fun syncSuccessStatus(ne: Boolean, countStr: String, calName: String) =
        if (ne) "सफलतापूर्वक $countStr वार्षिक मितिहरू '$calName' मा सिंक गरियो।"
        else "Synced $countStr upcoming BS occurrences to '$calName'."
    fun syncPermissionDeniedStatus(ne: Boolean) =
        if (ne) "गुगल क्यालेन्डर सिंकका लागि पात्रो अनुमति (Calendar Permission) आवश्यक छ।"
        else "Calendar permission is required to sync with Google Calendar."
    fun syncNoCalendarStatus(ne: Boolean) =
        if (ne) "फोनमा कुनै लेख्न मिल्ने गुगल क्यालेन्डर भेटिएन। कृपया गुगल क्यालेन्डर एपमा खाता जोड्नुहोस् वा प्रत्येक कार्यक्रममा रहेको [+गुगल] बटन प्रयोग गर्नुहोस्।"
        else "No writable Google Calendar found on device. Sign in to the Google Calendar app or use [+GCAL] on individual events."
    fun syncErrorStatus(ne: Boolean) =
        if (ne) "गुगल क्यालेन्डर सिंक गर्दा त्रुटि भयो।"
        else "Error communicating with system Calendar provider."

    fun sec6DataInfoTitle(ne: Boolean) =
        if (ne) "६. अफलाइन डाटा र शुद्धता" else "6. OFFLINE DATA & ACCURACY"
    fun dataCoverageLine1(ne: Boolean) =
        if (ne) "बि.सं. पात्रो डाटा: वि.सं. १९७० – २१०० (सन् १९१३ – २०४४)"
        else "BS Table Coverage: 1970 BS – 2100 BS (1913 AD – 2044 AD)"
    fun dataCoverageLine2(ne: Boolean) =
        if (ne) "चन्द्र तिथि गणना: जिन मियस (Jean Meeus) सूर्य-चन्द्र देशान्तर सूत्र (काठमाडौँ UTC+05:45 सूर्योदय)"
        else "Tithi Engine: Jean Meeus lunar-solar elongation at Kathmandu sunrise (UTC+05:45)"
    fun dataCoverageLine3(ne: Boolean) =
        if (ne) "नेपाली पर्व र बिदा: सौर्य संक्रान्ति र चन्द्र तिथिका आधारमा गणना गरिएको"
        else "Holidays: Fixed BS solar holidays + lunar Tithi festivals + Saturdays"
    fun dataCoverageLine4(ne: Boolean) =
        if (ne) "नेटवर्क अनुमति: शून्य (१००% अफलाइन)"
        else "Network Permissions: NONE (100% Offline)"
}
