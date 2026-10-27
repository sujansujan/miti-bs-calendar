package np.com.miti.patro.sync

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import np.com.miti.patro.data.local.EventEntity
import np.com.miti.patro.data.repository.ResolvedEventInstance
import np.com.miti.patro.domain.engine.BsCalendarData
import np.com.miti.patro.domain.engine.BsDateConverter
import np.com.miti.patro.domain.model.MissingDayStrategy
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone

/**
 * Synchronizes repeating Bikram Sambat (BS) events with the device's Google Calendar
 * via Android's local CalendarContract ContentProvider (zero INTERNET permission required).
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

    fun syncSingleEvent(
        context: Context,
        event: EventEntity,
        strategy: MissingDayStrategy,
        yearsAhead: Int = 5,
        useDevanagari: Boolean = false
    ): SyncResult {
        if (!hasCalendarPermissions(context)) {
            return SyncResult(false, 0, errorCode = ErrorCode.PERMISSION_DENIED)
        }
        val targetCal = findWritableGoogleCalendar(context)
            ?: return SyncResult(false, 0, errorCode = ErrorCode.NO_WRITABLE_CALENDAR)

        return try {
            val count = upsertEventOccurrences(
                context = context,
                calendarId = targetCal.id,
                event = event,
                strategy = strategy,
                yearsAhead = yearsAhead,
                useDevanagari = useDevanagari
            )
            SyncResult(true, count, calendarDisplayName = targetCal.displayName)
        } catch (_: Exception) {
            SyncResult(false, 0, errorCode = ErrorCode.PROVIDER_ERROR)
        }
    }

    fun syncAllEvents(
        context: Context,
        events: List<EventEntity>,
        strategy: MissingDayStrategy,
        yearsAhead: Int = 5,
        useDevanagari: Boolean = false
    ): SyncResult {
        if (!hasCalendarPermissions(context)) {
            return SyncResult(false, 0, errorCode = ErrorCode.PERMISSION_DENIED)
        }
        val targetCal = findWritableGoogleCalendar(context)
            ?: return SyncResult(false, 0, errorCode = ErrorCode.NO_WRITABLE_CALENDAR)

        return try {
            var totalSynced = 0
            for (event in events) {
                totalSynced += upsertEventOccurrences(
                    context = context,
                    calendarId = targetCal.id,
                    event = event,
                    strategy = strategy,
                    yearsAhead = yearsAhead,
                    useDevanagari = useDevanagari
                )
            }
            SyncResult(true, totalSynced, calendarDisplayName = targetCal.displayName)
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
    ): Int {
        val todayAd = LocalDate.now(NEPAL_ZONE)
        val currentBsYear = BsDateConverter.adToBs(todayAd).year
        val endBsYear = (currentBsYear + yearsAhead).coerceAtMost(BsCalendarData.MAX_YEAR)

        var count = 0
        for (bsYear in currentBsYear..endBsYear) {
            if (bsYear < event.startBsYear) continue

            val resolvedOcc = BsDateConverter.resolveAnnualOccurrence(
                eventMonth = event.bsMonth,
                eventDay = event.bsDay,
                targetBsYear = bsYear,
                strategy = strategy
            ) ?: continue

            val resolvedBsDate = resolvedOcc.targetDate
            val adDate = BsDateConverter.bsToAd(resolvedBsDate)
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
                "बिक्रम सम्बत वार्षिक कार्यक्रम: $bsMonthName $bsDayStr, $bsYearStr बि.सं."
            } else {
                "Bikram Sambat Annual Event: $bsMonthName $bsDayStr, $bsYearStr BS"
            }
            val fullDescription = buildString {
                append(descriptionHeader)
                if (event.note.isNotBlank()) {
                    append("
")
                    append(event.note)
                }
            }

            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.TITLE, "${event.title} ($bsMonthName $bsDayStr)")
                put(CalendarContract.Events.DESCRIPTION, fullDescription)
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getTimeZone("Asia/Kathmandu").id)
                put(CalendarContract.Events.UID_2445, uid)
                put(CalendarContract.Events.HAS_ALARM, if (event.reminderEnabled) 1 else 0)
            }

            val existingId = findExistingEventIdByUid(context, calendarId, uid)
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

    fun launchAddToGoogleCalendarIntent(
        context: Context,
        instance: ResolvedEventInstance,
        useDevanagari: Boolean
    ) {
        val event = instance.entity
        val resolvedBsDate = instance.effectiveBsDate
        val adDate = instance.effectiveAdDate

        val startMillis = adDate
            .atTime(event.reminderHour.coerceIn(0, 23), event.reminderMinute.coerceIn(0, 59))
            .atZone(NEPAL_ZONE)
            .toInstant()
            .toEpochMilli()
        val endMillis = startMillis + 60L * 60L * 1000L

        val bsMonthName = BsCalendarData.getMonthName(resolvedBsDate.month, useDevanagari)
        val bsDayStr = BsCalendarData.formatNumber(resolvedBsDate.day, useDevanagari)
        val bsYearStr = BsCalendarData.formatNumber(resolvedBsDate.year, useDevanagari)

        val desc = if (useDevanagari) {
            "बिक्रम सम्बत मिति: $bsYearStr $bsMonthName $bsDayStr
${event.note}".trim()
        } else {
            "Bikram Sambat Date: $bsYearStr $bsMonthName $bsDayStr BS
${event.note}".trim()
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
