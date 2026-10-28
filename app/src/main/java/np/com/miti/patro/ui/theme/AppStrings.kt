package np.com.miti.patro.ui.theme

import np.com.miti.patro.domain.engine.BsCalendarData
import kotlin.math.abs

/**
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
    fun sankrantiTag(ne: Boolean) = if (ne) "संक्रान्ति" else "Sankranti"
    fun masantaTag(ne: Boolean) = if (ne) "मसान्त" else "Masanta"

    fun editBtn(ne: Boolean) = if (ne) "सम्पादन" else "EDIT"
    fun delBtn(ne: Boolean) = if (ne) "हटाउने" else "DEL"
    fun gcalExportBtn(ne: Boolean) = "GCAL"

    fun monthScheduleHeader(ne: Boolean, monthName: String, yearStr: String) =
        if (ne) "$monthName $yearStr कार्यक्रम र पर्व" else "${monthName.uppercase()} $yearStr EVENTS"

    fun noMonthItems(ne: Boolean) =
        if (ne) "कुनै कार्यक्रम छैन" else "No events this month"

    fun searchPlaceholder(ne: Boolean) =
        if (ne) "वर्षभरि कार्यक्रम वा पर्व खोज्नुहोस्..." else "Search year events & festivals..."

    fun noSearchMatches(ne: Boolean) =
        if (ne) "कुनै नतिजा भेटिएन" else "No matching events or festivals"

    fun formatRelativeDays(daysDiff: Long, ne: Boolean): String {
        return when {
            daysDiff == 0L -> todayTag(ne)
            daysDiff > 0L -> {
                val n = BsCalendarData.formatNumber(daysDiff.toInt(), ne)
                if (ne) "$n दिन पछि" else "in ${n}d"
            }
            else -> {
                val n = BsCalendarData.formatNumber(abs(daysDiff).toInt(), ne)
                if (ne) "$n दिन अघि" else "${n}d ago"
            }
        }
    }

    fun formatYearAndCountdownBadge(daysDiff: Long, yearsElapsed: Int, ne: Boolean): String {
        val rel = formatRelativeDays(daysDiff, ne)
        return if (yearsElapsed > 0) {
            val yStr = BsCalendarData.formatNumber(yearsElapsed, ne)
            val yTag = if (ne) "वर्ष $yStr" else "#$yStr"
            "[$yTag • $rel]"
        } else {
            "[$rel]"
        }
    }

    // Event Editor Dialog (Minimal + Shraddha Tithi mode)
    fun editEventHeader(ne: Boolean) = if (ne) "कार्यक्रम सम्पादन" else "EDIT EVENT"
    fun newEventHeader(ne: Boolean) = if (ne) "नयाँ कार्यक्रम" else "NEW EVENT"
    fun modeGateyBtn(ne: Boolean) = if (ne) "गते (GATEY)" else "EVENT (GATEY)"
    fun modeShraddhaBtn(ne: Boolean) = if (ne) "श्राद्ध (तिथि)" else "SHRADDHA (TITHI)"
    fun shraddhaTag(ne: Boolean) = if (ne) "श्राद्ध" else "SHRADDHA"
    fun bsAnchorDateLabel(ne: Boolean) = if (ne) "बि.सं. मिति (वार्षिक):" else "BS DATE (YEARLY):"
    fun eventTitleLabel(ne: Boolean) = if (ne) "शीर्षक *" else "TITLE *"
    fun eventTitlePlaceholder(ne: Boolean, isShraddha: Boolean = false) = when {
        isShraddha && ne -> "जस्तै: बुबाको वार्षिक श्राद्ध..."
        isShraddha -> "e.g., Buba Shraddha..."
        ne -> "कार्यक्रमको नाम..."
        else -> "Event title..."
    }
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
    fun goBtn(ne: Boolean) = if (ne) "जानुहोस्" else "GO"

    // Date Converter Screen
    fun converterTopHeader(ne: Boolean) =
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

    // Minimal Settings Screen
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
}
