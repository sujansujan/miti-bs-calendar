package np.com.miti.patro.ui.theme

/**
<<<<<<< HEAD
 * Full UI string table for English and Nepali (Devanagari).
 */
object AppStrings {
    fun todayBtn(ne: Boolean) = if (ne) "आज" else "TODAY"
    fun addEventBtn(ne: Boolean) = if (ne) "+कार्यक्रम" else "+EVENT"
    fun convertBtn(ne: Boolean) = if (ne) "रूपान्तरण" else "CONVERT"
    fun calendarBtn(ne: Boolean) = if (ne) "पात्रो" else "CALENDAR"
    fun cfgBtn(ne: Boolean) = if (ne) "सेटिङ" else "CFG"
    fun backToCalendar(ne: Boolean) = if (ne) "<- पात्रो" else "<- CALENDAR"

    fun legendHoliday(ne: Boolean) = if (ne) "* रातो = बिदा / शनिबार" else "* RED = Holiday / Saturday"
    fun legendEvent(ne: Boolean) = if (ne) "+N = वार्षिक बि.सं. कार्यक्रम (रङ अनुसार)" else "+N = Annual BS Event (Colored)"
    fun legendToday(ne: Boolean) = if (ne) "[दिन] = आज" else "[Day] = Today"

    fun selectedDayHeader(ne: Boolean) = if (ne) "चयन गरिएको मिति विवरण" else "SELECTED DATE SUMMARY"
    fun gregorianLabel(ne: Boolean) = if (ne) "इस्वी संवत् (AD):" else "GREGORIAN (AD):"
    fun tithiLabel(ne: Boolean) = if (ne) "तिथि / पक्ष:" else "LUNAR TITHI:"
    fun holidayStatusLabel(ne: Boolean) = if (ne) "बिदाको स्थिति:" else "HOLIDAY STATUS:"
    fun publicHolidaySaturday(ne: Boolean) = if (ne) "सार्वजनिक बिदा (शनिबार)" else "PUBLIC HOLIDAY (Saturday)"
    fun publicHolidayFestival(ne: Boolean) = if (ne) "सार्वजनिक पर्व बिदा" else "PUBLIC FESTIVAL HOLIDAY"
    fun personalEventHoliday(ne: Boolean) = if (ne) "व्यक्तिगत कार्यक्रम बिदा" else "PERSONAL EVENT HOLIDAY"
    fun workingDay(ne: Boolean) = if (ne) "नियमित कार्य दिन" else "REGULAR WORKING DAY"

    fun festivalsHeader(ne: Boolean) = if (ne) "पर्व तथा दिवसहरू:" else "FESTIVALS & OBSERVANCES:"
    fun noFestivals(ne: Boolean) = if (ne) "कुनै प्रमुख पर्व छैन" else "None listed"
    fun publicHolidayTag(ne: Boolean) = if (ne) "[बिदा]" else "[HOLIDAY]"

    fun eventsOnDayHeader(ne: Boolean) = if (ne) "वार्षिक बि.सं. कार्यक्रमहरू:" else "REPEATING BS EVENTS ON THIS DAY:"
    fun noEventsOnDay(ne: Boolean) = if (ne) "यस मितिमा कुनै वार्षिक कार्यक्रम छैन।" else "No repeating BS events on this date."
    fun addEventOnDayBtn(ne: Boolean) = if (ne) "+ यस मितिमा कार्यक्रम थप्नुहोस्" else "+ ADD BS EVENT ON THIS DAY"

    fun editBtn(ne: Boolean) = if (ne) "सम्पादन" else "EDIT"
    fun delBtn(ne: Boolean) = if (ne) "हटाउनुहोस्" else "DEL"
    fun gcalExportBtn(ne: Boolean) = if (ne) "+GCAL (सबै बि.सं. वर्ष)" else "+GCAL (BS->AD)"

    fun monthScheduleHeader(ne: Boolean, monthName: String, yearStr: String) =
        if (ne) "$monthName $yearStr को तालिका (पर्व र कार्यक्रम)" else "SCHEDULE FOR ${monthName.uppercase()} $yearStr"

    fun noMonthItems(ne: Boolean) =
        if (ne) "यस महिनामा कुनै पर्व वा दर्ता गरिएका कार्यक्रम छैनन्।" else "No festivals or user events in this month."

    // Event Editor Dialog
    fun editEventHeader(ne: Boolean) = if (ne) "बि.सं. कार्यक्रम सम्पादन" else "EDIT ANNUAL BS EVENT"
    fun newEventHeader(ne: Boolean) = if (ne) "नयाँ वार्षिक बि.सं. कार्यक्रम" else "NEW ANNUAL BS EVENT"
    fun bsAnchorDateLabel(ne: Boolean) = if (ne) "बि.सं. मिति (प्रत्येक वर्ष यही मितिमा दोहोरिने):" else "BS ANCHOR DATE (REPEats ANNUALLY ON THIS BS MONTH & DAY):"
    fun eventTitleLabel(ne: Boolean) = if (ne) "कार्यक्रमको नाम *" else "EVENT TITLE *"
    fun eventTitlePlaceholder(ne: Boolean) = if (ne) "उदा. आमाको जन्मदिन / श्राद्ध / वार्षिकोत्सव" else "e.g. Mom's Birthday / Shraddha / Anniversary"
    fun eventNotesLabel(ne: Boolean) = if (ne) "विवरण / कैफियत (ऐच्छिक)" else "NOTES (OPTIONAL)"
    fun eventNotesPlaceholder(ne: Boolean) = if (ne) "थप जानकारी..." else "Optional details..."
    fun eventColorLabel(ne: Boolean) = if (ne) "कार्यक्रमको रङ छान्नुहोस् (पात्रो र गुगल क्यालेन्डरको लागि):" else "EVENT COLOR (CALENDAR CELL, TEXT & GOOGLE CALENDAR):"
    fun markHolidayLabel(ne: Boolean) = if (ne) "यस दिनलाई व्यक्तिगत बिदाको रूपमा चिन्ह लगाउनुहोस्" else "Mark this day as a personal holiday"
    fun missingDayRuleHeader(ne: Boolean) = if (ne) "यदि कुनै वर्ष यो महिना छोटो भएमा (उदा. ३० वा ३१ गते नभएमा):" else "IF DAY DOES NOT EXIST IN A FUTURE BS YEAR'S MONTH:"
    fun reminderHeader(ne: Boolean) = if (ne) "रिमाइन्डर सूचना (अफलाइन अलार्म):" else "REMINDER NOTIFICATION (OFFLINE):"
    fun enableReminderLabel(ne: Boolean) = if (ne) "वार्षिक रिमाइन्डर अन गर्नुहोस्" else "Enable annual BS notification reminder"
    fun reminderTimingLabel(ne: Boolean) = if (ne) "सूचना समय:" else "NOTIFY WHEN:"
    fun sameDay(ne: Boolean) = if (ne) "सोही दिन" else "Same Day"
    fun oneDayBefore(ne: Boolean) = if (ne) "१ दिन अघि" else "1 Day Before"
    fun twoDaysBefore(ne: Boolean) = if (ne) "२ दिन अघि" else "2 Days Before"
    fun sevenDaysBefore(ne: Boolean) = if (ne) "७ दिन अघि" else "7 Days Before"
    fun hourLabel(ne: Boolean) = if (ne) "घण्टा (0-23):" else "HOUR (0-23):"
    fun minuteLabel(ne: Boolean) = if (ne) "मिनेट:" else "MINUTE:"
    fun cancelBtn(ne: Boolean) = if (ne) "रद्द" else "CANCEL"
    fun saveBtn(ne: Boolean) = if (ne) "सुरक्षित गर्नुहोस्" else "SAVE BS EVENT"

    // Year/Month Jump Dialog
    fun jumpHeader(ne: Boolean) = if (ne) "बि.सं. वर्ष र महिना छान्नुहोस् (१९७० - २१००)" else "JUMP TO BS YEAR & MONTH (1970 - 2100)"
    fun selectYearLabel(ne: Boolean) = if (ne) "बि.सं. वर्ष:" else "BS YEAR:"
    fun selectMonthLabel(ne: Boolean) = if (ne) "बि.सं. महिना:" else "BS MONTH:"
=======
 * Minimal UI string table for English and Nepali (Devanagari).
 */
object AppStrings {
    fun todayBtn(ne: Boolean) = if (ne) "आज" else "TODAY"
    fun addEventBtn(ne: Boolean) = "+"
    fun convertBtn(ne: Boolean) = if (ne) "बि↔इ" else "BS↔AD"
    fun calendarBtn(ne: Boolean) = if (ne) "पात्रो" else "CAL"
    fun cfgBtn(ne: Boolean) = if (ne) "सेटिङ" else "CFG"
    fun backToCalendar(ne: Boolean) = if (ne) "<- पात्रो" else "<- BACK"

    fun todayTag(ne: Boolean) = if (ne) "आज" else "TODAY"
    fun selectedTag(ne: Boolean) = if (ne) "चयन" else "SELECTED"
    fun publicHolidayTag(ne: Boolean) = if (ne) "बिदा" else "HOLIDAY"

    fun editBtn(ne: Boolean) = if (ne) "सम्पादन" else "EDIT"
    fun delBtn(ne: Boolean) = if (ne) "हटाउने" else "DEL"
    fun gcalExportBtn(ne: Boolean) = "GCAL"

    fun monthScheduleHeader(ne: Boolean, monthName: String, yearStr: String) =
        if (ne) "$monthName $yearStr कार्यक्रम र पर्व" else "${monthName.uppercase()} $yearStr EVENTS"

    fun noMonthItems(ne: Boolean) =
        if (ne) "कुनै कार्यक्रम छैन" else "No events this month"

    // Event Editor Dialog (Minimal)
    fun editEventHeader(ne: Boolean) = if (ne) "कार्यक्रम सम्पादन" else "EDIT EVENT"
    fun newEventHeader(ne: Boolean) = if (ne) "नयाँ कार्यक्रम" else "NEW EVENT"
    fun bsAnchorDateLabel(ne: Boolean) = if (ne) "बि.सं. मिति (वार्षिक):" else "BS DATE (YEARLY):"
    fun eventTitleLabel(ne: Boolean) = if (ne) "शीर्षक *" else "TITLE *"
    fun eventTitlePlaceholder(ne: Boolean) = if (ne) "कार्यक्रमको नाम..." else "Event title..."
    fun eventNotesLabel(ne: Boolean) = if (ne) "कैफियत" else "NOTE"
    fun eventNotesPlaceholder(ne: Boolean) = if (ne) "ऐच्छिक..." else "Optional..."
    fun eventColorLabel(ne: Boolean) = if (ne) "रङ:" else "COLOR:"
    fun markHolidayLabel(ne: Boolean) = if (ne) "व्यक्तिगत बिदा" else "Personal holiday"
    fun missingDayRuleHeader(ne: Boolean) = if (ne) "छोटो महिना नियम:" else "SHORT MONTH RULE:"
    fun reminderHeader(ne: Boolean) = if (ne) "रिमाइन्डर:" else "REMINDER:"
    fun enableReminderLabel(ne: Boolean) = if (ne) "रिमाइन्डर अलार्म" else "Reminder alarm"
    fun reminderTimingLabel(ne: Boolean) = if (ne) "दिन:" else "WHEN:"
    fun sameDay(ne: Boolean) = if (ne) "सोही दिन" else "0d"
    fun oneDayBefore(ne: Boolean) = if (ne) "१ दिन" else "1d"
    fun twoDaysBefore(ne: Boolean) = if (ne) "२ दिन" else "2d"
    fun sevenDaysBefore(ne: Boolean) = if (ne) "७ दिन" else "7d"
    fun hourLabel(ne: Boolean) = if (ne) "घण्टा:" else "HR:"
    fun minuteLabel(ne: Boolean) = if (ne) "मिनेट:" else "MIN:"
    fun cancelBtn(ne: Boolean) = if (ne) "रद्द" else "CANCEL"
    fun saveBtn(ne: Boolean) = if (ne) "सेभ" else "SAVE"

    // Year/Month Jump Dialog
    fun jumpHeader(ne: Boolean) = if (ne) "वर्ष र महिना (१९७०-२१००)" else "YEAR & MONTH (1970-2100)"
    fun selectYearLabel(ne: Boolean) = if (ne) "वर्ष:" else "YEAR:"
    fun selectMonthLabel(ne: Boolean) = if (ne) "महिना:" else "MONTH:"
>>>>>>> 2f0105a (Initial commit: Miti BS Calendar)
    fun goBtn(ne: Boolean) = if (ne) "जानुहोस्" else "GO"

    // Date Converter Screen
    fun converterTopHeader(ne: Boolean) =
<<<<<<< HEAD
        if (ne) "मिति रूपान्तरण (बि.सं. <-> इ.सं. | १९७०-२१०० बि.सं. / १९१३-२०४४ इ.सं.)"
        else "DATE CONVERTER (BS <-> AD | 1970-2100 BS / 1913-2044 AD)"

    fun bsToAdBoxHeader(ne: Boolean) =
        if (ne) "१. विक्रम संवत् (BS) -> इस्वी संवत् (AD) रूपान्तरण"
        else "1. BIKRAM SAMBAT (BS) -> GREGORIAN (AD) CONVERTER"

    fun adToBsBoxHeader(ne: Boolean) =
        if (ne) "२. इस्वी संवत् (AD) -> विक्रम संवत् (BS) रूपान्तरण"
        else "2. GREGORIAN (AD) -> BIKRAM SAMBAT (BS) CONVERTER"

    fun convertedResultHeader(ne: Boolean) =
        if (ne) "रूपान्तरित मिति र पञ्चाङ्ग विवरण:"
        else "CONVERTED DATE & PANCHANGA DETAILS:"

    fun bsDateLabel(ne: Boolean) = if (ne) "विक्रम संवत् (BS):" else "BIKRAM SAMBAT (BS):"
    fun adDateLabel(ne: Boolean) = if (ne) "इस्वी संवत् (AD):" else "GREGORIAN (AD):"
    fun weekdayLabel(ne: Boolean) = if (ne) "बार:" else "WEEKDAY:"
    fun openInCalendarBtn(ne: Boolean) = if (ne) "पात्रोमा हेर्नुहोस्" else "OPEN IN CALENDAR"
    fun addEventOnConvertedBtn(ne: Boolean) = if (ne) "+ यस बि.सं. मितिमा कार्यक्रम थप्नुहोस्" else "+ ADD BS EVENT ON THIS DATE"
    fun adYearLabel(ne: Boolean) = if (ne) "इ.सं. वर्ष (1913-2044):" else "AD YEAR (1913-2044):"
    fun adMonthLabel(ne: Boolean) = if (ne) "इ.सं. महिना (1-12):" else "AD MONTH (1-12):"
    fun adDayLabel(ne: Boolean) = if (ne) "इ.सं. तारिख:" else "AD DAY:"
    fun bsDayLabel(ne: Boolean) = if (ne) "बि.सं. गते:" else "BS DAY:"

    // Settings Screen
    fun settingsTopHeader(ne: Boolean) = if (ne) "मिति (MITI) — सेटिङ र कन्फिगरेसन" else "MITI — SETTINGS & CONFIGURATION"
    fun themeSectionHeader(ne: Boolean) = if (ne) "१. थिम र रङ (SYSTEM / ALUCARD / DRACULA)" else "1. APPEARANCE & THEME PALETTE"
    fun themeSystem(ne: Boolean) = if (ne) "प्रणाली अनुसार (System Default: Light=Alucard, Dark=Dracula)" else "Follow System Theme (Light = Alucard, Dark = Dracula)"
    fun themeAlucard(ne: Boolean) = if (ne) "आलुकार्ड लाइट (Alucard Official Warm Daylight)" else "Alucard Light (Warm Daylight Print Palette)"
    fun themeDracula(ne: Boolean) = if (ne) "ड्राकुला डार्क (Dracula Official Dark Slate)" else "Dracula Dark (High-Contrast Slate Palette)"

    fun missingDaySectionHeader(ne: Boolean) = if (ne) "२. छोटो महिनामा कार्यक्रम दोहोरिने नियम" else "2. DEFAULT MISSING-DAY RULE FOR SHORT BS MONTHS"
    fun missingDaySectionNote(ne: Boolean) =
        if (ne) "बि.सं. महिनाको लम्बाइ प्रत्येक वर्ष २९ देखि ३२ दिनसम्म फरक हुन सक्छ। यदि कुनै कार्यक्रम ३१ वा ३२ गतेमा छ र अर्को वर्ष त्यो महिनामा ३० दिन मात्र छ भने:"
        else "BS month lengths vary between 29 and 32 days across years. When an event saved on day 30, 31, or 32 repeats in a year whose month has fewer days:"

    fun displaySectionHeader(ne: Boolean) = if (ne) "३. भाषा र पात्रो प्रदर्शन विकल्प" else "3. LANGUAGE & CALENDAR DISPLAY OPTIONS"
    fun languageScopeLabel(ne: Boolean) = if (ne) "नेपाली (देवनागरी) भाषा प्रयोग:" else "NEPALI (DEVANAGARI) LANGUAGE SCOPE:"
    fun langEnglishEverywhere(ne: Boolean) = if (ne) "अंग्रेजी मात्र (English everywhere)" else "English everywhere"
    fun langCalendarTabOnly(ne: Boolean) = if (ne) "पात्रो ट्याबमा मात्र नेपाली (Nepali on Calendar tab only)" else "Nepali on Calendar tab only (Settings & Converter in English)"
    fun langNepaliEverywhere(ne: Boolean) = if (ne) "सबै ठाउँमा नेपाली (Nepali everywhere)" else "Nepali everywhere (Calendar, Converter, Dialogs & Settings)"

    fun showTithiToggle(ne: Boolean) = if (ne) "प्रत्येक दिनको कोठामा तिथि देखाउनुहोस् (औंसी देखि पूर्णिमा)" else "Show daily lunar Tithi in calendar grid cells"
    fun showAdToggle(ne: Boolean) = if (ne) "प्रत्येक दिनको कोठामा इस्वी संवत् (AD) दिन देखाउनुहोस्" else "Show Gregorian (AD) day number in calendar grid cells"
    fun showFestivalsToggle(ne: Boolean) = if (ne) "प्रमुख नेपाली चाडपर्व र बिदाहरू देखाउनुहोस्" else "Highlight major Nepali festivals and public holidays"

    fun reminderSectionHeader(ne: Boolean) = if (ne) "४. पूर्वनिर्धारित रिमाइन्डर समय" else "4. DEFAULT EVENT REMINDER TIME"
    fun defaultHourLabel(ne: Boolean) = if (ne) "पूर्वनिर्धारित घण्टा (२४-घण्टा ढाँचा):" else "Default Notification Hour (24h):"
    fun defaultDaysBeforeLabel(ne: Boolean) = if (ne) "कति दिन अघि सूचना दिने:" else "Default Advance Notice:"

    fun gcalSectionHeader(ne: Boolean) = if (ne) "५. गुगल क्यालेन्डर (GOOGLE CALENDAR) सिंक" else "5. GOOGLE CALENDAR BS->AD MULTI-YEAR SYNC"
    fun gcalAutoSyncToggle(ne: Boolean) =
        if (ne) "कार्यक्रम सेभ गर्दा वा बदल्दा स्वतः गुगल क्यालेन्डरमा सिंक गर्नुहोस्"
        else "Automatically sync BS events to Google Calendar on save/delete"
    fun gcalYearsAheadLabel(ne: Boolean) =
        if (ne) "आगामी कति बि.सं. वर्षसम्म सिंक गर्ने (प्रत्येक वर्षको सही AD मितिमा):"
        else "Future BS years to project (each BS year resolved to its exact AD date):"
    fun gcalSyncNowBtn(ne: Boolean) =
        if (ne) "सबै बि.सं. कार्यक्रमहरू अहिले गुगल क्यालेन्डरमा सिंक गर्नुहोस्"
        else "SYNC ALL BS EVENTS TO GOOGLE CALENDAR NOW"
    fun gcalPermissionNote(ne: Boolean) =
        if (ne) "नोट: गुगल क्यालेन्डरको सामान्य 'Yearly Repeat' ले अंग्रेजी (AD) मिति मात्र दोहोर्याउँछ। त्यसैले यस एपले आगामी प्रत्येक बि.सं. वर्षको सही अंग्रेजी (AD) मिति गणना गरी छुट्टाछुट्टै कार्यक्रमको रूपमा गुगल क्यालेन्डरमा दर्ता गर्छ।"
        else "Why per-year BS->AD sync? Google Calendar's native yearly repeat repeats on the Gregorian (AD) date, which drifts away from the Bikram Sambat date every year. Miti computes the exact converted AD date for each upcoming BS year and writes individual yearly occurrences so every future year lands on the true BS date."

    fun gcalSyncSuccessMsg(ne: Boolean, eventsCount: Int, occurrences: Int, calName: String, preview: String) =
        if (ne) "सफलतापूर्वक $eventsCount बि.सं. कार्यक्रम ($occurrences वार्षिक मितिहरू) '$calName' मा सिंक गरियो। उदा: $preview"
        else "Synced $eventsCount BS event(s) ($occurrences yearly BS->AD occurrences) to '$calName'. ($preview)"

    fun gcalSingleSyncSuccessMsg(ne: Boolean, title: String, occurrences: Int, calName: String, preview: String) =
        if (ne) "'$title' का आगामी $occurrences बि.सं. वर्षहरू सही AD मितिमा '$calName' मा सिंक गरियो: $preview"
        else "Synced '$title' across $occurrences BS years to '$calName' on exact AD dates: $preview"

    fun gcalNoWritableCalMsg(ne: Boolean) =
        if (ne) "कुनै लेख्न मिल्ने गुगल क्यालेन्डर भेटिएन। कृपया फोनमा Google Calendar खाता लगइन भएको जाँच गर्नुहोस्।"
        else "No writable calendar found on device. Ensure Google Calendar is signed in."

    fun gcalPermDeniedMsg(ne: Boolean) =
        if (ne) "क्यालेन्डर अनुमति अस्वीकृत भयो। गुगल क्यालेन्डर सिंकको लागि अनुमति आवश्यक छ।"
        else "Calendar permission denied. Grant calendar access to sync BS events to Google Calendar."

    fun verificationSectionHeader(ne: Boolean) = if (ne) "६. डाटा स्रोत र प्रमाणीकरण जानकारी" else "6. DATA ACCURACY & OFFICIAL VERIFICATION NOTES"
    fun verificationBody(ne: Boolean) =
        if (ne) "यस एपमा वि.सं. १९७० देखि २१०० सम्मका महिनाका दिनहरू र मीमांसा/सूर्यसिद्धान्त चन्द्र-सौर्य गणितबाट गणना गरिएका तिथिहरू समावेश छन्। नेपाल सरकार पञ्चाङ्ग निर्णायक विकास समितिको आधिकारिक पात्रो वा हाम्रो पात्रोसँग दशैं, तिहार र एकादशीका तिथिहरू रुजु गर्न सकिन्छ।"
        else "BS month lengths (1970-2100 BS) use authoritative precomputed tables anchored to 1970 Baisakh 1 = 1913-04-13 AD. Daily Tithi is computed offline via Meeus lunar-solar elongation at Nepal Standard Time (UTC+05:45) sunrise. Verify exact Dashain/Tihar/Ekadashi sunrise boundary overrides against the Nepal Panchanga Nirnayak Bikash Samiti bulletin or Hamro Patro."

    fun backupSectionHeader(ne: Boolean) =
        if (ne) "६. ब्याकअप र रिस्टोर (सेटिङ र कार्यक्रम निर्यात / आयात)"
        else "6. BACKUP & RESTORE (EXPORT / IMPORT SETTINGS & EVENTS)"

    fun backupSectionNote(ne: Boolean) =
        if (ne) "तपाईंको सबै सेटिङहरू र वार्षिक बि.सं. कार्यक्रमहरूलाई एउटै JSON फाइलमा सुरक्षित (Export) गर्न वा अर्को फोनमा सार्न (Import) सक्नुहुन्छ।"
        else "Save all your settings and repeating BS events to a portable .json file, or restore a previous backup file onto this device."

    fun exportBackupBtn(ne: Boolean) =
        if (ne) "सेटिङ र कार्यक्रम निर्यात गर्नुहोस् (EXPORT .JSON)"
        else "EXPORT SETTINGS & EVENTS (.JSON)"

    fun importBackupBtn(ne: Boolean) =
        if (ne) "ब्याकअप फाइल आयात गर्नुहोस् (IMPORT .JSON)"
        else "IMPORT SETTINGS & EVENTS (.JSON)"

    fun exportSuccessMsg(ne: Boolean, count: Int) =
        if (ne) "सफलतापूर्वक सेटिङ र $count कार्यक्रमहरू JSON फाइलमा निर्यात गरियो।"
        else "Exported settings and $count BS event(s) to JSON backup file."

    fun exportErrorMsg(ne: Boolean) =
        if (ne) "ब्याकअप फाइल सुरक्षित गर्न सकिएन।"
        else "Failed to write backup file."

    fun importSuccessMsg(ne: Boolean, addedCount: Int) =
        if (ne) "ब्याकअपबाट सेटिङ र $addedCount कार्यक्रमहरू सफलतापूर्वक रिस्टोर गरियो।"
        else "Restored settings and imported $addedCount BS event(s) from backup file."

    fun importErrorMsg(ne: Boolean) =
        if (ne) "ब्याकअप फाइल पढ्न सकिएन। कृपया सही .json फाइल छान्नुहोस्।"
        else "Invalid or unreadable backup JSON file."

    fun verificationSectionHeader7(ne: Boolean) =
        if (ne) "७. डाटा स्रोत र प्रमाणीकरण जानकारी"
        else "7. DATA ACCURACY & OFFICIAL VERIFICATION NOTES"
=======
        if (ne) "मिति रूपान्तरण (बि.सं. <-> इ.सं.)" else "DATE CONVERTER (BS <-> AD)"
    fun bsToAdBoxHeader(ne: Boolean) = if (ne) "बि.सं. -> इ.सं." else "BS -> AD"
    fun adToBsBoxHeader(ne: Boolean) = if (ne) "इ.सं. -> बि.सं." else "AD -> BS"
    fun convertedResultHeader(ne: Boolean) = if (ne) "नतिजा:" else "RESULT:"
    fun bsDateLabel(ne: Boolean) = if (ne) "बि.सं.:" else "BS:"
    fun adDateLabel(ne: Boolean) = if (ne) "इ.सं.:" else "AD:"
    fun tithiLabel(ne: Boolean) = if (ne) "तिथि:" else "TITHI:"
    fun festivalsHeader(ne: Boolean) = if (ne) "पर्व:" else "FESTIVAL:"
    fun openInCalendarBtn(ne: Boolean) = if (ne) "पात्रोमा हेर्ने" else "VIEW IN CAL"
    fun addEventOnConvertedBtn(ne: Boolean) = "+"
    fun adYearLabel(ne: Boolean) = if (ne) "इ.सं. वर्ष:" else "AD YEAR:"
    fun adMonthLabel(ne: Boolean) = if (ne) "इ.सं. महिना:" else "AD MONTH:"
    fun adDayLabel(ne: Boolean) = if (ne) "इ.सं. दिन:" else "AD DAY:"
    fun bsDayLabel(ne: Boolean) = if (ne) "बि.सं. गते:" else "BS DAY:"

    // Minimal Settings Screen (No unnecessary exposition)
    fun settingsTopHeader(ne: Boolean) = if (ne) "मिति — सेटिङ" else "MITI — SETTINGS"
    fun themeSectionHeader(ne: Boolean) = if (ne) "थिम (THEME)" else "THEME"
    fun themeSystem(ne: Boolean) = if (ne) "प्रणाली (System)" else "System (Auto)"
    fun themeAlucard(ne: Boolean) = if (ne) "आलुकार्ड (Light)" else "Alucard (Light)"
    fun themeDracula(ne: Boolean) = if (ne) "ड्राकुला (Dark)" else "Dracula (Dark)"

    fun displaySectionHeader(ne: Boolean) = if (ne) "भाषा र दृश्य" else "LANGUAGE & DISPLAY"
    fun langEnglishEverywhere(ne: Boolean) = if (ne) "English" else "English"
    fun langCalendarTabOnly(ne: Boolean) = if (ne) "पात्रोमा मात्र नेपाली" else "Nepali (Calendar tab only)"
    fun langNepaliEverywhere(ne: Boolean) = if (ne) "सबै ठाउँमा नेपाली" else "Nepali (Everywhere)"

    fun showTithiToggle(ne: Boolean) = if (ne) "कोठामा तिथि" else "Show Tithi in cells"
    fun showAdToggle(ne: Boolean) = if (ne) "कोठामा AD मिति" else "Show AD day in cells"
    fun showFestivalsToggle(ne: Boolean) = if (ne) "चाडपर्व र बिदा" else "Show festivals & holidays"

    fun missingDaySectionHeader(ne: Boolean) = if (ne) "छोटो महिना नियम" else "SHORT MONTH RULE"

    fun reminderSectionHeader(ne: Boolean) = if (ne) "रिमाइन्डर समय" else "DEFAULT REMINDER"
    fun defaultHourLabel(ne: Boolean) = if (ne) "समय:" else "Time:"
    fun defaultDaysBeforeLabel(ne: Boolean) = if (ne) "अग्रिम:" else "Advance:"

    fun gcalSectionHeader(ne: Boolean) = if (ne) "गुगल क्यालेन्डर सिंक" else "GOOGLE CALENDAR SYNC"
    fun gcalAutoSyncToggle(ne: Boolean) = if (ne) "अटो-सिंक (BS->AD)" else "Auto-sync (BS->AD)"
    fun gcalYearsAheadLabel(ne: Boolean) = if (ne) "वर्ष:" else "Years ahead:"
    fun gcalSyncNowBtn(ne: Boolean) = if (ne) "अहिले सिंक गर्नुहोस्" else "SYNC NOW"

    fun gcalSyncSuccessMsg(ne: Boolean, eventsCount: Int, occurrences: Int, calName: String, preview: String) =
        if (ne) "$eventsCount कार्यक्रम ($occurrences मिति) '$calName' मा सिंक भयो"
        else "Synced $eventsCount event(s) ($occurrences dates) to '$calName'"

    fun gcalSingleSyncSuccessMsg(ne: Boolean, title: String, occurrences: Int, calName: String, preview: String) =
        if (ne) "'$title' ($occurrences वर्ष) '$calName' मा सिंक भयो"
        else "Synced '$title' ($occurrences yrs) to '$calName'"

    fun gcalNoWritableCalMsg(ne: Boolean) =
        if (ne) "गुगल क्यालेन्डर भेटिएन" else "No writable Google Calendar found"

    fun gcalPermDeniedMsg(ne: Boolean) =
        if (ne) "क्यालेन्डर अनुमति आवश्यक छ" else "Calendar permission required"

    fun backupSectionHeader(ne: Boolean) = if (ne) "ब्याकअप (BACKUP)" else "BACKUP & RESTORE"
    fun exportBackupBtn(ne: Boolean) = if (ne) "निर्यात (EXPORT .JSON)" else "EXPORT .JSON"
    fun importBackupBtn(ne: Boolean) = if (ne) "आयात (IMPORT .JSON)" else "IMPORT .JSON"

    fun exportSuccessMsg(ne: Boolean, count: Int) =
        if (ne) "$count कार्यक्रम निर्यात गरियो" else "Exported settings & $count event(s)"
    fun exportErrorMsg(ne: Boolean) =
        if (ne) "निर्यात असफल" else "Export failed"
    fun importSuccessMsg(ne: Boolean, addedCount: Int) =
        if (ne) "$addedCount कार्यक्रम आयात गरियो" else "Restored settings & $addedCount event(s)"
    fun importErrorMsg(ne: Boolean) =
        if (ne) "अमान्य फाइल" else "Invalid backup file"
>>>>>>> 2f0105a (Initial commit: Miti BS Calendar)
}
