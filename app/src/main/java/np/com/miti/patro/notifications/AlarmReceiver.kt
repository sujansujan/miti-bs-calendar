package np.com.miti.patro.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import np.com.miti.patro.MitiApplication
import np.com.miti.patro.domain.engine.BsDateConverter

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getLongExtra(AlarmScheduler.EXTRA_EVENT_ID, -1L)
        if (eventId <= 0L) return

        val pendingResult = goAsync()
        val app = context.applicationContext as MitiApplication

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val event = app.eventRepository.getEventById(eventId) ?: return@launch
                val settings = app.userPreferencesRepository.userSettingsFlow.first()
                val currentBsYear = BsDateConverter.todayBs().year
                val targetYear = intent.getIntExtra(
                    AlarmScheduler.EXTRA_RESOLVED_BS_YEAR,
                    currentBsYear
                )

                val resolved = BsDateConverter.resolveEventOccurrence(
                    event = event,
                    targetBsYear = targetYear,
                    strategy = settings.missingDayStrategy
                )

                if (resolved != null) {
                    val yearsElapsed = (targetYear - event.startBsYear).coerceAtLeast(0)
                    NotificationHelper.showEventReminderNotification(
                        context = context,
                        eventId = event.id,
                        title = event.title,
                        bsDateFormatted = resolved.targetDate.formatHuman(settings.useDevanagari),
                        note = event.note,
                        yearsElapsed = yearsElapsed
                    )
                }

                AlarmScheduler.scheduleNextOccurrence(
                    context = context,
                    event = event,
                    strategy = settings.missingDayStrategy
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
