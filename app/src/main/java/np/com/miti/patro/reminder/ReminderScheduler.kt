package np.com.miti.patro.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import np.com.miti.patro.data.local.EventEntity
import np.com.miti.patro.domain.engine.BsCalendarData
import np.com.miti.patro.domain.engine.BsDateConverter
import np.com.miti.patro.domain.model.MissingDayStrategy
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

object ReminderScheduler {

    const val EXTRA_EVENT_ID = "extra_event_id"
    const val EXTRA_TARGET_BS_YEAR = "extra_target_bs_year"

    fun scheduleNextReminder(
        context: Context,
        event: EventEntity,
        strategy: MissingDayStrategy,
        now: ZonedDateTime = ZonedDateTime.now()
    ) {
        cancelReminder(context, event.id)
        if (!event.reminderEnabled) return

        val nextTrigger = findNextTriggerDateTime(
            event = event,
            strategy = strategy,
            now = now
        ) ?: return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_EVENT_ID, event.id)
            putExtra(EXTRA_TARGET_BS_YEAR, nextTrigger.targetBsYear)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerMillis = nextTrigger.triggerZonedDateTime.toInstant().toEpochMilli()

        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        if (canScheduleExact) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        }
    }

    fun cancelReminder(context: Context, eventId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            eventId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    data class NextTriggerResult(
        val targetBsYear: Int,
        val triggerZonedDateTime: ZonedDateTime
    )

    fun findNextTriggerDateTime(
        event: EventEntity,
        strategy: MissingDayStrategy,
        now: ZonedDateTime = ZonedDateTime.now(),
        zoneId: ZoneId = now.zone
    ): NextTriggerResult? {
        val currentBs = BsDateConverter.adToBs(now.toLocalDate())
        val startSearchYear = maxOf(currentBs.year, event.startBsYear)

        for (candidateYear in startSearchYear..BsCalendarData.MAX_YEAR) {
            val resolved = BsDateConverter.resolveEventOccurrence(
                event = event,
                targetBsYear = candidateYear,
                strategy = strategy
            ) ?: continue

            val eventAdDate = BsDateConverter.bsToAd(resolved.targetDate)
            val notifyAdDate = eventAdDate.minusDays(event.reminderDaysBefore.toLong())
            val notifyTime = LocalTime.of(
                event.reminderHour.coerceIn(0, 23),
                event.reminderMinute.coerceIn(0, 59)
            )
            val triggerZdt = ZonedDateTime.of(LocalDateTime.of(notifyAdDate, notifyTime), zoneId)

            if (triggerZdt.isAfter(now)) {
                return NextTriggerResult(
                    targetBsYear = candidateYear,
                    triggerZonedDateTime = triggerZdt
                )
            }
        }
        return null
    }
}
