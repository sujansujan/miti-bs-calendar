package np.com.miti.patro.sync

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import np.com.miti.patro.data.local.EventEntity
import np.com.miti.patro.domain.engine.BsCalendarData
import np.com.miti.patro.domain.engine.BsDateConverter
import np.com.miti.patro.domain.model.MissingDayStrategy
import np.com.miti.patro.domain.model.ResolvedBsOccurrence
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Syncs repeating BS events and Shraddha Tithi events to Android's CalendarProvider (Google Calendar)
 * across N future BS years by computing the exact AD date for each individual BS year.
 */
object GoogleCalendarSyncHelper {

    private const val SYNC_TAG_PREFIX = "[MITI-BS-EVENT-ID:"

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

    private fun findPrimaryWritableCalendarId(context: Context): Long? {
        if (!hasCalendarPermissions(context)) return null
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        )
        val selection = "${CalendarContract.Calendars.VISIBLE} = 1 AND " +
            "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ${CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR}"

        var fallbackId: Long? = null
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            selection,
            null,
            null
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(CalendarContract.Calendars._ID)
            val primIdx = cursor.getColumnIndex(CalendarContract.Calendars.IS_PRIMARY)
            while (cursor.moveToNext()) {
                val calId = cursor.getLong(idIdx)
                val isPrimary = if (primIdx >= 0) cursor.getInt(primIdx) == 1 else false
                if (isPrimary) return calId
                if (fallbackId == null) fallbackId = calId
            }
        }
        return fallbackId
    }

    fun syncEventAcrossYears(
        context: Context,
        event: EventEntity,
        strategy: MissingDayStrategy,
        yearsAhead: Int,
        useDevanagari: Boolean = false
    ): Int {
        val calId = findPrimaryWritableCalendarId(context) ?: return 0
        deleteSyncedOccurrences(context, event.id)

        val currentBsYear = BsDateConverter.todayBs().year
        val startYear = if (event.isRepeating) maxOf(currentBsYear, event.startBsYear) else event.startBsYear
        val endYear = if (event.isRepeating) {
            (currentBsYear + yearsAhead).coerceAtMost(BsCalendarData.MAX_YEAR)
        } else {
            event.startBsYear.coerceIn(BsCalendarData.MIN_YEAR, BsCalendarData.MAX_YEAR)
        }
        val zoneId = ZoneId.of("Asia/Kathmandu")

        var insertedCount = 0
        for (targetYear in startYear..endYear) {
            val occurrences = BsDateConverter.resolveEventOccurrencesInBsYear(
                event = event,
                targetBsYear = targetYear,
                strategy = strategy
            )
            for (resolved in occurrences) {
                val adDate = BsDateConverter.bsToAd(resolved.targetDate)
                val startZdt = ZonedDateTime.of(
                    adDate.year,
                    adDate.monthValue,
                    adDate.dayOfMonth,
                    event.reminderHour.coerceIn(0, 23),
                    event.reminderMinute.coerceIn(0, 59),
                    0,
                    0,
                    zoneId
                )
                val startMillis = startZdt.toInstant().toEpochMilli()
                val endMillis = startMillis + 60 * 60 * 1000L

                val bsMonthName = BsCalendarData.getMonthName(resolved.targetDate.month, useDevanagari)
                val bsYearStr = BsCalendarData.formatNumber(resolved.targetDate.year, useDevanagari)
                val bsDayStr = BsCalendarData.formatNumber(resolved.targetDate.day, useDevanagari)
                val tag = "$SYNC_TAG_PREFIX${event.id}]"
                val descText = if (useDevanagari) {
                    "बिक्रम सम्बत मिति: $bsYearStr $bsMonthName $bsDayStr गते\n${event.note}\n$tag".trim()
                } else {
                    "Bikram Sambat Date: $bsYearStr $bsMonthName $bsDayStr BS\n${event.note}\n$tag".trim()
                }

                val values = ContentValues().apply {
                    put(CalendarContract.Events.CALENDAR_ID, calId)
                    put(CalendarContract.Events.TITLE, "${event.title} ($bsMonthName $bsDayStr)")
                    put(CalendarContract.Events.DESCRIPTION, descText)
                    put(CalendarContract.Events.DTSTART, startMillis)
                    put(CalendarContract.Events.DTEND, endMillis)
                    put(CalendarContract.Events.EVENT_TIMEZONE, "Asia/Kathmandu")
                }

                val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
                if (uri != null) {
                    insertedCount++
                    val gcalEventId = uri.lastPathSegment?.toLongOrNull()
                    if (gcalEventId != null && event.reminderEnabled) {
                        val remValues = ContentValues().apply {
                            put(CalendarContract.Reminders.EVENT_ID, gcalEventId)
                            put(CalendarContract.Reminders.MINUTES, event.reminderDaysBefore * 24 * 60)
                            put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                        }
                        context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, remValues)
                    }
                }
            }
        }
        return insertedCount
    }

    fun deleteSyncedOccurrences(context: Context, eventId: Long) {
        if (!hasCalendarPermissions(context)) return
        val tag = "$SYNC_TAG_PREFIX$eventId]"
        try {
            context.contentResolver.delete(
                CalendarContract.Events.CONTENT_URI,
                "${CalendarContract.Events.DESCRIPTION} LIKE ?",
                arrayOf("%$tag%")
            )
        } catch (_: Exception) {
        }
    }

    fun launchSystemCalendarInsertIntent(
        context: Context,
        event: EventEntity,
        strategy: MissingDayStrategy,
        useDevanagari: Boolean = false
    ) {
        val currentBs = BsDateConverter.todayBs()
        val startSearchYear = maxOf(currentBs.year, event.startBsYear)
        val endSearchYear = (startSearchYear + 2).coerceAtMost(BsCalendarData.MAX_YEAR)
        var targetOccurrence: ResolvedBsOccurrence? = null
        for (y in startSearchYear..endSearchYear) {
            val occurrences = BsDateConverter.resolveEventOccurrencesInBsYear(event, y, strategy)
            val match = occurrences.firstOrNull { it.targetDate >= currentBs }
            if (match != null) {
                targetOccurrence = match
                break
            }
        }
        if (targetOccurrence == null) return

        val adDate = BsDateConverter.bsToAd(targetOccurrence.targetDate)
        val zoneId = ZoneId.of("Asia/Kathmandu")
        val startMillis = ZonedDateTime.of(
            adDate.year,
            adDate.monthValue,
            adDate.dayOfMonth,
            event.reminderHour.coerceIn(0, 23),
            event.reminderMinute.coerceIn(0, 59),
            0,
            0,
            zoneId
        ).toInstant().toEpochMilli()

        val bsMonthName = BsCalendarData.getMonthName(targetOccurrence.targetDate.month, useDevanagari)
        val bsDayStr = BsCalendarData.formatNumber(targetOccurrence.targetDate.day, useDevanagari)
        val bsYearStr = BsCalendarData.formatNumber(targetOccurrence.targetDate.year, useDevanagari)
        val desc = if (useDevanagari) {
            "बिक्रम सम्बत मिति: $bsYearStr $bsMonthName $bsDayStr गते\n${event.note}"
        } else {
            "Bikram Sambat Date: $bsYearStr $bsMonthName $bsDayStr BS\n${event.note}"
        }

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, "${event.title} ($bsMonthName $bsDayStr)")
            putExtra(CalendarContract.Events.DESCRIPTION, desc)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, startMillis + 60 * 60 * 1000L)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }
}
