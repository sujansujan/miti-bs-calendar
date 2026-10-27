package np.com.miti.patro.sync

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import np.com.miti.patro.data.local.EventEntity
import np.com.miti.patro.domain.engine.BsCalendarData
import np.com.miti.patro.domain.engine.BsDateConverter
import np.com.miti.patro.domain.model.MissingDayStrategy
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone

/**
 * Synchronizes repeating Bikram Sambat (BS) events with the device's Google Calendar
 * via Android's local CalendarContract ContentProvider (zero INTERNET permission required).
 *
 * CRITICAL BS RECURRENCE DESIGN:
 * Because Bikram Sambat repeating dates fall on DIFFERENT Gregorian (AD) dates every year
 * (and BS month lengths vary from 29 to 32 days), a standard Google Calendar yearly rule
 * (`RRULE:FREQ=YEARLY`) would wrongly repeat on the same AD date every year.
 *
 * Instead, when you tap `[+GCAL]` on an event (or enable Auto-Sync in Settings), this helper
 * loops through every upcoming BS year `[startYear .. currentBsYear + yearsAhead]`, resolves
 * the exact BS date for that year using [BsDateConverter.resolveAnnualOccurrence], converts
 * each BS year's occurrence to its true Gregorian (AD) date via [BsDateConverter.bsToAd], and
 * inserts/updates each year's occurrence individually in Google Calendar with `RRULE = null`.
 */
object GoogleCalendarSyncHelper {

    private val NEPAL_ZONE: ZoneId = ZoneId.of("Asia/Kathmandu")

    data class SyncResult(
        val success: Boolean,
        val syncedOccurrences: Int,
        val calendarDisplayName: String? = null,
        val sampleDatesPreview: String = "",
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
     * Syncs a single BS repeating event across all upcoming BS years `[start .. currentBsYear + yearsAhead]`
     * into Google Calendar so each future BS year lands on its exact converted AD date.
     */
    fun syncSingleEvent(
        context: Context,
        event: EventEntity,
        strategy: MissingDayStrategy,
        yearsAhead: Int = 15,
        useDevanagari: Boolean = false
    ): SyncResult {
        if (!hasCalendarPermissions(context)) {
            return SyncResult(false, 0, errorCode = ErrorCode.PERMISSION_DENIED)
        }
        val targetCal = findWritableGoogleCalendar(context)
            ?: return SyncResult(false, 0, errorCode = ErrorCode.NO_WRITABLE_CALENDAR)

        return try {
            val (count, preview) = upsertEventOccurrences(
                context = context,
                calendarId = targetCal.id,
                event = event,
                strategy = strategy,
                yearsAhead = yearsAhead,
                useDevanagari = useDevanagari
            )
            SyncResult(
                success = true,
                syncedOccurrences = count,
                calendarDisplayName = targetCal.displayName,
                sampleDatesPreview = preview
            )
        } catch (_: Exception) {
            SyncResult(false, 0, errorCode = ErrorCode.PROVIDER_ERROR)
        }
    }

    /**
     * Syncs all saved BS repeating events across `[currentBsYear .. currentBsYear + yearsAhead]`
     * into Google Calendar on their exact per-year BS->AD converted dates.
     */
    fun syncAllEvents(
        context: Context,
        events: List<EventEntity>,
        strategy: MissingDayStrategy,
        yearsAhead: Int = 15,
        useDevanagari: Boolean = false
    ): SyncResult {
        if (!hasCalendarPermissions(context)) {
            return SyncResult(false, 0, errorCode = ErrorCode.PERMISSION_DENIED)
        }
        val targetCal = findWritableGoogleCalendar(context)
            ?: return SyncResult(false, 0, errorCode = ErrorCode.NO_WRITABLE_CALENDAR)

        return try {
            var totalSynced = 0
            var firstPreview = ""
            for (event in events) {
                val (c, p) = upsertEventOccurrences(
                    context = context,
                    calendarId = targetCal.id,
                    event = event,
                    strategy = strategy,
                    yearsAhead = yearsAhead,
                    useDevanagari = useDevanagari
                )
                totalSynced += c
                if (firstPreview.isEmpty() && p.isNotEmpty()) {
                    firstPreview = p
                }
            }
            SyncResult(
                success = true,
                syncedOccurrences = totalSynced,
                calendarDisplayName = targetCal.displayName,
                sampleDatesPreview = firstPreview
            )
        } catch (_: Exception) {
            SyncResult(false, 0, errorCode = ErrorCode.PROVIDER_ERROR)
        }
    }

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
        event: EventEntity,
        strategy: MissingDayStrategy,
        yearsAhead: Int,
        useDevanagari: Boolean
    ): Pair<Int, String> {
        val todayAd = LocalDate.now(NEPAL_ZONE)
        val currentBsYear = BsDateConverter.adToBs(todayAd).year
        val startYear = minOf(currentBsYear, event.startBsYear).coerceAtLeast(BsCalendarData.MIN_YEAR)
        val endBsYear = (currentBsYear + yearsAhead).coerceAtMost(BsCalendarData.MAX_YEAR)

        val existingUidToIdMap = findExistingEventIdsByPrefix(context, calendarId, "miti_bs_event_${event.id}_%")
        var count = 0
        val previewSamples = mutableListOf<String>()

        for (bsYear in startYear..endBsYear) {
            if (bsYear < event.startBsYear) continue

            val resolvedOcc = BsDateConverter.resolveAnnualOccurrence(
                eventMonth = event.bsMonth,
                eventDay = event.bsDay,
                targetBsYear = bsYear,
                strategy = strategy
            ) ?: continue

            val resolvedBsDate = resolvedOcc.targetDate
            // Convert THIS target BS year's BS date to its exact Gregorian (AD) date
            val adDate = BsDateConverter.bsToAd(resolvedBsDate)

            if (bsYear >= currentBsYear && previewSamples.size < 3) {
                previewSamples.add("$bsYear BS -> $adDate AD")
            }

            val startMillis = adDate
                .atTime(event.reminderHour.coerceIn(0, 23), event.reminderMinute.coerceIn(0, 59))
                .atZone(NEPAL_ZONE)
                .toInstant()
                .toEpochMilli()
            val endMillis = startMillis + 60L * 60L * 1000L

            val uid = "miti_bs_event_${event.id}_${bsYear}"
            val bsMonthName = BsCalendarData.getMonthName(resolvedBsDate.month, useDevanagari)
            val bsDayStr = BsCalendarData.formatNumber(resolvedBsDate.day, useDevanagari)
            val bsYearStr = BsCalendarData.formatNumber(resolvedBsDate.year, useDevanagari)

            val descriptionHeader = if (useDevanagari) {
                "बिक्रम सम्बत वार्षिक कार्यक्रम: $bsMonthName $bsDayStr, $bsYearStr बि.सं. (इस्वी: $adDate)"
            } else {
                "Bikram Sambat Annual Event: $bsMonthName $bsDayStr, $bsYearStr BS (Gregorian: $adDate AD)"
            }
            val fullDescription = buildString {
                append(descriptionHeader)
                if (event.note.isNotBlank()) {
                    append("\n")
                    append(event.note)
                }
            }

            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.TITLE, "${event.title} ($bsMonthName $bsDayStr, $bsYearStr BS)")
                put(CalendarContract.Events.DESCRIPTION, fullDescription)
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                // Explicitly clear RRULE/RDATE so Google Calendar NEVER repeats on the same AD date
                putNull(CalendarContract.Events.RRULE)
                putNull(CalendarContract.Events.RDATE)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getTimeZone("Asia/Kathmandu").id)
                put(CalendarContract.Events.UID_2445, uid)
                put(CalendarContract.Events.HAS_ALARM, if (event.reminderEnabled) 1 else 0)
                put(CalendarContract.Events.EVENT_COLOR, event.resolvedColor.gcalArgb)
            }

            val existingId = existingUidToIdMap[uid]
            if (existingId != null) {
                val updateUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, existingId)
                context.contentResolver.update(updateUri, values, null, null)
            } else {
                val insertedUri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
                val newEventId = insertedUri?.lastPathSegment?.toLongOrNull()
                if (newEventId != null && event.reminderEnabled) {
                    val reminderValues = ContentValues().apply {
                        put(CalendarContract.Reminders.EVENT_ID, newEventId)
                        put(CalendarContract.Reminders.MINUTES, event.reminderDaysBefore * 24 * 60)
                        put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                    }
                    context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
                }
            }
            count++
        }
        return count to previewSamples.joinToString(", ")
    }

    private fun findExistingEventIdsByPrefix(
        context: Context,
        calendarId: Long,
        uidPrefixPattern: String
    ): Map<String, Long> {
        val map = HashMap<String, Long>()
        val projection = arrayOf(CalendarContract.Events._ID, CalendarContract.Events.UID_2445)
        context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            projection,
            "${CalendarContract.Events.CALENDAR_ID} = ? AND ${CalendarContract.Events.UID_2445} LIKE ?",
            arrayOf(calendarId.toString(), uidPrefixPattern),
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val uid = cursor.getString(1) ?: continue
                map[uid] = id
            }
        }
        return map
    }
}
