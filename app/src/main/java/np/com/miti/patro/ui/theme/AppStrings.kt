package np.com.miti.patro.ui.theme

import np.com.miti.patro.domain.engine.BsCalendarData
import kotlin.math.abs

/**
 * Bilingual English & Nepali (Devanagari) UI strings for Miti.
 */
object AppStrings {

    fun appBrand(ne: Boolean) = if (ne) "मिति" else "MITI"
    fun adhikMaasTag(ne: Boolean) = if (ne) "[अधिक मास]" else "[ADHIK]"

    fun todayBtn(ne: Boolean) = if (ne) "आज" else "TODAY"
    fun convertBtn(ne: Boolean) = "BS↔AD"
    fun cfgBtn(ne: Boolean) = if (ne) "सेटिङ" else "CFG"

    fun todayTag(ne: Boolean) = if (ne) "आज" else "TODAY"
    fun selectedTag(ne: Boolean) = if (ne) "चयन" else "SELECTED"
    fun publicHolidayTag(ne: Boolean) = if (ne) "बिदा" else "HOLIDAY"
    fun sankrantiTag(ne: Boolean) = if (ne) "संक्रान्ति" else "Sankranti"
    fun masantaTag(ne: Boolean) = if (ne) "मसान्त" else "Masanta"
    fun shraddhaTag(ne: Boolean) = if (ne) "श्राद्ध" else "Shraddha"

    fun gcalExportBtn(ne: Boolean) = if (ne) "गुगल" else "GCAL"
    fun editBtn(ne: Boolean) = if (ne) "सम्पादन" else "EDIT"
    fun delBtn(ne: Boolean) = if (ne) "हटाउनु" else "DEL"

    fun monthScheduleHeader(ne: Boolean, monthName: String, yearStr: String) =
        if (ne) "$monthName $yearStr कार्यक्रम र पर्व" else "${monthName.uppercase()} $yearStr EVENTS & FESTIVALS"

    fun noMonthItems(ne: Boolean) =
        if (ne) "यस महिनामा कुनै पर्व वा कार्यक्रम छैन।" else "No festivals or events in this month."

    fun searchPlaceholder(ne: Boolean) =
        if (ne) "वर्षभरि खोज्नुहोस् (पर्व / कार्यक्रम)..." else "Search year (festival / event)..."

    fun noSearchMatches(ne: Boolean) =
        if (ne) "कुनै परिणाम भेटिएन।" else "No matching events or festivals in this year."

    fun formatRelativeDays(daysDiff: Long, ne: Boolean): String {
        val absDays = abs(daysDiff).toInt()
        val numStr = BsCalendarData.formatNumber(absDays, ne)
        return when {
            daysDiff == 0L -> if (ne) "आज" else "today"
            daysDiff == 1L -> if (ne) "भोलि" else "tomorrow"
            daysDiff == -1L -> if (ne) "हिजो" else "yesterday"
            daysDiff > 1L -> if (ne) "$numStr दिन पछि" else "in ${numStr}d"
            else -> if (ne) "$numStr दिन अघि" else "${numStr}d ago"
        }
    }

    fun formatYearAndCountdownBadge(daysDiff: Long, yearsElapsed: Int, ne: Boolean): String {
        val rel = formatRelativeDays(daysDiff, ne)
        return if (yearsElapsed > 0) {
            val yStr = BsCalendarData.formatNumber(yearsElapsed, ne)
            if (ne) "[वर्ष $yStr • $rel]" else "[#$yStr • $rel]"
        } else {
            "[$rel]"
        }
    }

    // Event Editor Dialog
    fun editEventHeader(ne: Boolean) = if (ne) "कार्यक्रम सम्पादन" else "EDIT BS EVENT"
    fun newEventHeader(ne: Boolean) = if (ne) "नयाँ कार्यक्रम थप्नुहोस्" else "ADD REPEATING BS EVENT"
    fun eventModeLabel(ne: Boolean) = if (ne) "प्रकार:" else "REPEAT BY:"
    fun modeSolarBs(ne: Boolean) = if (ne) "गते (BS Date)" else "BS Date (Gatey)"
    fun modeShraddhaTithi(ne: Boolean) = if (ne) "श्राद्ध तिथि (Tithi)" else "Shraddha (Lunar Tithi)"
    fun bsDateFieldLabel(ne: Boolean) = if (ne) "बिक्रम सम्बत मिति:" else "BS DATE:"
    fun shraddhaTithiFieldLabel(ne: Boolean) = if (ne) "महिना र श्राद्ध तिथि:" else "BS MONTH & SHRADDHA TITHI:"
    fun titleFieldLabel(ne: Boolean) = if (ne) "शीर्षक:" else "TITLE:"
    fun titlePlaceholder(ne: Boolean) = if (ne) "उदा. जन्मदिन / बुबाको श्राद्ध" else "e.g. Mom's Birthday / Shraddha"
    fun notesFieldLabel(ne: Boolean) = if (ne) "कैफियत (ऐच्छिक):" else "NOTES (OPTIONAL):"
    fun notesPlaceholder(ne: Boolean) = if (ne) "थप विवरण..." else "Optional note..."
    fun colorLabel(ne: Boolean) = if (ne) "रङ:" else "COLOR:"
    fun personalHolidayCheckbox(ne: Boolean) = if (ne) "व्यक्तिगत बिदा (रातो दिन)" else "Personal Holiday (Red Day)"
    fun reminderCheckbox(ne: Boolean) = if (ne) "अलार्म सूचना" else "Reminder Notification"
    fun timeLabel(ne: Boolean) = if (ne) "समय:" else "TIME:"
    fun cancelBtn(ne: Boolean) = if (ne) "रद्द" else "CANCEL"
    fun saveBtn(ne: Boolean) = if (ne) "सेभ" else "SAVE"

    // Year/Month Jump Dialog
    fun jumpHeader(ne: Boolean) = if (ne) "वर्ष र महिना छान्नुहोस्" else "SELECT BS YEAR & MONTH"
    fun goBtn(ne: Boolean) = if (ne) "जानुहोस्" else "GO"

    // Date Converter Screen
    fun backToCalendar(ne: Boolean) = if (ne) "< पात्रो" else "< CALENDAR"
    fun converterTopHeader(ne: Boolean) = if (ne) "मिति रूपान्तरण (BS ↔ AD)" else "DATE CONVERTER (BS ↔ AD)"
    fun bsToAdBoxHeader(ne: Boolean) = if (ne) "बि.सं. → ई.सं. (BS → AD)" else "BS -> AD"
    fun adToBsBoxHeader(ne: Boolean) = if (ne) "ई.सं. → बि.सं. (AD → BS)" else "AD -> BS"
    fun bsDayLabel(ne: Boolean) = if (ne) "गते:" else "BS DAY:"
    fun adDayLabel(ne: Boolean) = if (ne) "तारिख:" else "AD DAY:"
    fun bsDateLabel(ne: Boolean) = if (ne) "बि.सं.:" else "BS:"
    fun adDateLabel(ne: Boolean) = if (ne) "ई.सं.:" else "AD:"
    fun tithiLabel(ne: Boolean) = if (ne) "तिथि:" else "TITHI:"
    fun openInCalendarBtn(ne: Boolean) = if (ne) "पात्रोमा हेर्नुहोस्" else "VIEW IN CALENDAR"

    // Settings Screen
    fun settingsTopHeader(ne: Boolean) = if (ne) "सेटिङहरू" else "SETTINGS"
    fun themeSectionHeader(ne: Boolean) = if (ne) "थिम (THEME)" else "THEME"
    fun scriptSectionHeader(ne: Boolean) = if (ne) "भाषा र लिपि (SCRIPT)" else "NEPALI SCRIPT"
    fun gridDisplayHeader(ne: Boolean) = if (ne) "पात्रो दृश्य (DISPLAY)" else "CALENDAR DISPLAY"
    fun showAdToggle(ne: Boolean) = if (ne) "ई.सं. तारिख देखाउने (AD Sub-date)" else "Show AD Sub-Date in Cells"
    fun adDatesInEnglishToggle(ne: Boolean) = if (ne) "ई.सं. मिति अंग्रेजीमा देखाउने (AD in EN)" else "Keep AD Dates in English"
    fun showTithiToggle(ne: Boolean) = if (ne) "तिथि देखाउने (Lunar Tithi)" else "Show Lunar Tithi in Cells"
    fun showFestivalsToggle(ne: Boolean) = if (ne) "चाडपर्व र बिदा देखाउने" else "Show Public Festivals & Holidays"
    fun persistentNotifToggle(ne: Boolean) = if (ne) "आजको मिति सूचना पट्टीमा राख्ने" else "Persistent Today Notification"
    fun persistentNotifDesc(ne: Boolean) = if (ne) "आजको मिति, तिथि र पर्व/कार्यक्रम सधैं सूचना पट्टीमा देखाउँछ" else "Pin today's BS date, Tithi & events in status bar"
    fun missingDayHeader(ne: Boolean) = if (ne) "३१/३२ गते नहुँदा (DAY 31/32 RULE)" else "MISSING DAY 31/32 RULE"
    fun defaultReminderHeader(ne: Boolean) = if (ne) "डिफल्ट अलार्म समय" else "DEFAULT REMINDER TIME"
    fun gcalSyncHeader(ne: Boolean) = if (ne) "गुगल क्यालेन्डर सिंक" else "GOOGLE CALENDAR SYNC"
    fun autoSyncToggle(ne: Boolean) = if (ne) "सेभ गर्दा स्वतः गुगल क्यालेन्डरमा पठाउने" else "Auto-Sync Events to Google Calendar"
    fun yearsAheadLabel(ne: Boolean) = if (ne) "आगामी वर्ष संख्या:" else "Sync Years Ahead:"
    fun syncAllNowBtn(ne: Boolean) = if (ne) "सबै कार्यक्रम गुगलमा पठाउनुहोस्" else "SYNC ALL EVENTS NOW"
    fun backupSectionHeader(ne: Boolean) = if (ne) "ब्याकअप र रिस्टोर (BACKUP)" else "BACKUP & RESTORE (JSON)"
    fun exportBackupBtn(ne: Boolean) = if (ne) "निर्यात (EXPORT JSON)" else "EXPORT JSON"
    fun importBackupBtn(ne: Boolean) = if (ne) "आयात (IMPORT JSON)" else "IMPORT JSON"
}
