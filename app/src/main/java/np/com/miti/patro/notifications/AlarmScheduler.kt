package np.com.miti.patro.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import np.com.miti.patro.data.local.EventEntity
import np.com.miti.patro.domain.engine.BsCalendarData
import np.com.miti.patro.domain.engine.BsDateConverter
import np.com.miti.patro.domain.model.MissingDayStrategy
import java.time.ZoneId
import java.time.ZonedDateTime

object AlarmScheduler {

    const val EXTRA_EVENT_ID = "extra_event_id"
    const val EXTRA_RESOLVED_BS_YEAR = "extra_resolved_bs_year"

    fun scheduleNextOccurrence(
        context: Context,
        event: EventEntity,
        strategy: MissingDayStrategy
    ) {
        if (!event.reminderEnabled) {
            cancelAlarm(context, event.id)
            return
        }

        val nowZdt = ZonedDateTime.now(ZoneId.systemDefault())
        val todayBs = BsDateConverter.adToBs(nowZdt.toLocalDate())
        val startSearchYear = maxOf(todayBs.year, event.startBsYear)

        for (candidateYear in startSearchYear..BsCalendarData.MAX_YEAR) {
            val resolved = BsDateConverter.resolveEventOccurrence(
                event = event,
                targetBsYear = candidateYear,
                strategy = strategy
            ) ?: continue

            val eventAdDate = BsDateConverter.bsToAd(resolved.targetDate)
            val triggerAdDate = eventAdDate.minusDays(event.reminderDaysBefore.toLong())

            val triggerZdt = ZonedDateTime.of(
                triggerAdDate.year,
                triggerAdDate.monthValue,
                triggerAdDate.dayOfMonth,
                event.reminderHour.coerceIn(0, 23),
                event.reminderMinute.coerceIn(0, 59),
                0,
                0,
                ZoneId.systemDefault()
            )

            if (triggerZdt.isAfter(nowZdt)) {
                setExactAlarm(
                    context = context,
                    eventId = event.id,
                    resolvedBsYear = candidateYear,
                    triggerAtMillis = triggerZdt.toInstant().toEpochMilli()
                )
                return
            }
        }
    }

    fun cancelAlarm(context: Context, eventId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
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

    private fun setExactAlarm(
        context: Context,
        eventId: Long,
        resolvedBsYear: Int,
        triggerAtMillis: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_EVENT_ID, eventId)
            putExtra(EXTRA_RESOLVED_BS_YEAR, resolvedBsYear)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            eventId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        try {
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }
}
