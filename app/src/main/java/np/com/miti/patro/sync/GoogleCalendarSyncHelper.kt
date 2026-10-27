package np.com.miti.patro.sync

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import np.com.miti.patro.domain.engine.BsCalendarData
import np.com.miti.patro.domain.engine.BsDateConverter
import np.com.miti.patro.domain.model.BsDate
import np.com.miti.patro.domain.model.BsEvent
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone

/**
 * Synchronizes repeating Bikram Sambat (BS) events with the device's Google Calendar
 * (via Android's local CalendarContract ContentProvider — zero INTERNET permission required;
 * the Android OS Google Calendar sync adapter handles cloud sync in the background).
 *
 * Because Bikram Sambat repeating events shift on the Gregorian calendar every year
 * (and BS month lengths vary from 29 to 32 days), standard Gregorian RRULE:FREQ=YEARLY
 * cannot accurately represent an annual BS date. Instead, this helper resolves the exact
 * Gregorian date for each upcoming BS year [currentBsYear .. currentBsYear + yearsAhead]
 * using BsDateConverter.resolveAnnualOccurrence() and writes/updates individual yearly
 * instances tagged with a deterministic UID_2445 (`miti_bs_event_<id>_<bsYear>`).
 *
 * Also provides a zero-permission Intent.ACTION_INSERT launcher so users can export any
 * single occurrence directly into the Google Calendar UI if they prefer not to grant
 * calendar provider permissions.
 */
object GoogleCalendarSyncHelper {

    private val NEPAL_ZONE: ZoneId = ZoneId.of("Asia/Kathmandu")

    data class SyncResult(
        val success: Boolean,
        val syncedOccurrences: Int,
        val calendarDisplayName: String? = null,
        val errorCode: ErrorCode? = null
    )

    enum class ErrorCode {
        PERMISSION_DENIED,
        NO_WRITABLE_CALENDAR,
        PROVIDER_ERROR
    }

    fun hasCalendarPermissions(context: Context): Boolean {
        val readGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        val writeGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        return readGranted && writeGranted
    }

    private data class TargetCalendar(
        val id: Long,
        val displayName: String,
        val accountName: String
    )

    /**
     * Finds the primary writable Google Calendar (`com.google`) on the device, or falls back
     * to any writable local calendar if no Google account is signed in.
     */
    private fun findWritableGoogleCalendar(context: Context): TargetCalendar? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        )
        val candidates = mutableListOf<Triple<TargetCalendar, Boolean, Boolean>>()
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            "${CalendarContract.Calendars.VISIBLE} = 1",
            null,
            null
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
            val nameIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            val accNameIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
            val accTypeIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_TYPE)
            val primaryIdx = cursor.getColumnIndex(CalendarContract.Calendars.IS_PRIMARY)
            val accessIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)

            while (cursor.moveToNext()) {
                val access = cursor.getInt(accessIdx)
                if (access >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR) {
                    val id = cursor.getLong(idIdx)
                    val name = cursor.getString(nameIdx) ?: "Calendar"
                    val accName = cursor.getString(accNameIdx) ?: ""
                    val accType = cursor.getString(accTypeIdx) ?: ""
                    val isPrimary = primaryIdx >= 0 && cursor.getInt(primaryIdx) == 1
                    val isGoogle = accType.contains("com.google", ignoreCase = true)
                    candidates.add(Triple(TargetCalendar(id, name, accName), isGoogle, isPrimary))
                }
            }
        }
        return candidates
            .sortedWith(
                compareByDescending<Triple<TargetCalendar, Boolean, Boolean>> { it.second }
                    .thenByDescending { it.third }
            )
            .firstOrNull()?.first
    }

    /**
     * Syncs a single BS repeating event across [yearsAhead] upcoming BS years into Google Calendar.
     */
    fun syncSingleEvent(
        context: Context,
        event: BsEvent,
        yearsAhead: Int = 5,
        nepaliScript: Boolean = false
    ): SyncResult {
        if (!hasCalendarPermissions(context)) {
            return SyncResult(false, 0, errorCode = ErrorCode.PERMISSION_DENIED)
        }
        val targetCal = findWritableGoogleCalendar(context)
            ?: return SyncResult(false, 0, errorCode = ErrorCode.NO_WRITABLE_CALENDAR)

        return try {
            val count = upsertEventOccurrences(context, targetCal.id, event, yearsAhead, nepaliScript)
            SyncResult(true, count, calendarDisplayName = targetCal.displayName)
        } catch (_: Exception) {
            SyncResult(false, 0, errorCode = ErrorCode.PROVIDER_ERROR)
        }
    }

    /**
     * Syncs all saved BS repeating events across [yearsAhead] upcoming BS years into Google Calendar.
     */
    fun syncAllEvents(
        context: Context,
        events: List<BsEvent>,
        yearsAhead: Int = 5,
        nepaliScript: Boolean = false
    ): SyncResult {
        if (!hasCalendarPermissions(context)) {
            return SyncResult(false, 0, errorCode = ErrorCode.PERMISSION_DENIED)
        }
        val targetCal = findWritableGoogleCalendar(context)
            ?: return SyncResult(false, 0, errorCode = ErrorCode.NO_WRITABLE_CALENDAR)

        return try {
            var totalSynced = 0
            for (event in events) {
                totalSynced += upsertEventOccurrences(context, targetCal.id, event, yearsAhead, nepaliScript)
            }
            SyncResult(true, totalSynced, calendarDisplayName = targetCal.displayName)
        } catch (_: Exception) {
            SyncResult(false, 0, errorCode = ErrorCode.PROVIDER_ERROR)
        }
    }

    /**
     * Deletes all synced Google Calendar occurrences for a deleted BS event.
     */
    fun removeSyncedEvent(context: Context, eventId: Long) {
        if (!hasCalendarPermissions(context)) return
        try {
            context.contentResolver.delete(
                CalendarContract.Events.CONTENT_URI,
                "${CalendarContract.Events.UID_2445} LIKE ?",
                arrayOf("miti_bs_event_${eventId}_%")
            )
        } catch (_: Exception) {
        }
    }

    private fun upsertEventOccurrences(
        context: Context,
        calendarId: Long,
        event: BsEvent,
        yearsAhead: Int,
        nepaliScript: Boolean
    ): Int {
        val todayAd = LocalDate.now(NEPAL_ZONE)
        val currentBsYear = BsDateConverter.adToBs(todayAd).year
        val endBsYear = (currentBsYear + yearsAhead).coerceAtMost(BsCalendarData.MAX_YEAR)

        var count = 0
        for (bsYear in currentBsYear..endBsYear) {
            if (bsYear < event.originBsYear) continue

            val resolvedBsDate: BsDate = BsDateConverter.resolveAnnualOccurrence(
                targetYear = bsYear,
                bsMonth = event.bsMonth,
                bsDay = event.bsDay,
                strategy = event.missingDayStrategy
            ) ?: continue

            val adDate = BsDateConverter.bsToAd(resolvedBsDate)
            val startMillis = adDate
                .atTime(event.reminderHour, event.reminderMinute)
                .atZone(NEPAL_ZONE)
                .toInstant()
                .toEpochMilli()
            val endMillis = startMillis + 60L * 60L * 1000L // 1-hour block

            val uid = "miti_bs_event_${event.id}_${bsYear}"
            val bsMonthName = BsCalendarData.getMonthName(resolvedBsDate.month, nepaliScript)
            val bsDayStr = BsCalendarData.formatNumber(resolvedBsDate.day, nepaliScript)
            val bsYearStr = BsCalendarData.formatNumber(resolvedBsDate.year, nepaliScript)

            val descriptionHeader = if (nepaliScript) {
                "बिक्रम सम्बत वार्षिक कार्यक्रम: $bsMonthName $bsDayStr, $bsYearStr बि.सं."
            } else {
                "Bikram Sambat Annual Event: $bsMonthName $bsDayStr, $bsYearStr BS"
            }
            val fullDescription = buildString {
                append(descriptionHeader)
                if (event.notes.isNotBlank()) {
                    append("\n")
                    append(event.notes)
                }
            }

            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.TITLE, "${event.title} ($bsMonthName $bsDayStr BS)")
                put(CalendarContract.Events.DESCRIPTION, fullDescription)
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getTimeZone("Asia/Kathmandu").id)
                put(CalendarContract.Events.UID_2445, uid)
                put(CalendarContract.Events.HAS_ALARM, if (event.reminderEnabled) 1 else 0)
            }

            val existingId = findExistingEventIdByUid(context, calendarId, uid)
            if (existingId != null) {
                val updateUri = android.content.ContentUris.withAppendedId(
                    CalendarContract.Events.CONTENT_URI,
                    existingId
                )
                context.contentResolver.update(updateUri, values, null, null)
            } else {
                val insertedUri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
                val newEventId = insertedUri?.lastPathSegment?.toLongOrNull()
                if (newEventId != null && event.reminderEnabled) {
                    val reminderValues = ContentValues().apply {
                        put(CalendarContract.Reminders.EVENT_ID, newEventId)
                        put(CalendarContract.Reminders.MINUTES, 0)
                        put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                    }
                    context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
                }
            }
            count++
        }
        return count
    }

    private fun findExistingEventIdByUid(context: Context, calendarId: Long, uid: String): Long? {
        val projection = arrayOf(CalendarContract.Events._ID)
        context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            projection,
            "${CalendarContract.Events.CALENDAR_ID} = ? AND ${CalendarContract.Events.UID_2445} = ?",
            arrayOf(calendarId.toString(), uid),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getLong(0)
            }
        }
        return null
    }

    /**
     * Zero-permission fallback: Opens the Google Calendar / system calendar event creation sheet
     * pre-populated with the resolved Gregorian date and BS details for a specific year.
     */
    fun launchAddToGoogleCalendarIntent(
        context: Context,
        event: BsEvent,
        resolvedBsDate: BsDate,
        adDate: LocalDate,
        nepaliScript: Boolean
    ) {
        val startMillis = adDate
            .atTime(event.reminderHour, event.reminderMinute)
            .atZone(NEPAL_ZONE)
            .toInstant()
            .toEpochMilli()
        val endMillis = startMillis + 60L * 60L * 1000L

        val bsMonthName = BsCalendarData.getMonthName(resolvedBsDate.month, nepaliScript)
        val bsDayStr = BsCalendarData.formatNumber(resolvedBsDate.day, nepaliScript)
        val bsYearStr = BsCalendarData.formatNumber(resolvedBsDate.year, nepaliScript)

        val desc = if (nepaliScript) {
            "बिक्रम सम्बत मिति: $bsYearStr $bsMonthName $bsDayStr\n${event.notes}".trim()
        } else {
            "Bikram Sambat Date: $bsYearStr $bsMonthName $bsDayStr BS\n${event.notes}".trim()
        }

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
            putExtra(CalendarContract.Events.TITLE, "${event.title} ($bsMonthName $bsDayStr)")
            putExtra(CalendarContract.Events.DESCRIPTION, desc)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }
}
