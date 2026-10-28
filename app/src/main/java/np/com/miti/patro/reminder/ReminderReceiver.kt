package np.com.miti.patro.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import np.com.miti.patro.data.local.AppDatabase
import np.com.miti.patro.data.preferences.UserPreferencesRepository
import np.com.miti.patro.domain.engine.BsCalendarData
import np.com.miti.patro.domain.engine.BsDateConverter
import java.time.ZonedDateTime

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getLongExtra(ReminderScheduler.EXTRA_EVENT_ID, -1L)
        if (eventId <= 0L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val prefsRepo = UserPreferencesRepository(context)
                val settings = prefsRepo.getSettingsOnce()
                val ne = settings.useDevanagari
                val event = db.eventDao().getEventById(eventId) ?: return@launch

                val currentBsYear = BsDateConverter.adToBs(ZonedDateTime.now().toLocalDate()).year
                val targetBsYear = intent.getIntExtra(
                    ReminderScheduler.EXTRA_TARGET_BS_YEAR,
                    currentBsYear
                )

                val resolved = BsDateConverter.resolveEventOccurrence(
                    event = event,
                    targetBsYear = targetBsYear,
                    strategy = settings.missingDayStrategy
                )

                val suffix = if (ne) "बि.सं." else "BS"
                val bsLabel = if (resolved != null) {
                    val mName = BsCalendarData.getMonthName(resolved.targetDate.month, ne)
                    val dStr = BsCalendarData.formatNumber(resolved.targetDate.day, ne)
                    val yStr = BsCalendarData.formatNumber(resolved.targetDate.year, ne)
                    "$yStr $mName $dStr $suffix"
                } else {
                    val mName = BsCalendarData.getMonthName(event.bsMonth, ne)
                    val dStr = BsCalendarData.formatNumber(event.bsDay, ne)
                    "$mName $dStr $suffix"
                }

                NotificationHelper.showEventReminderNotification(
                    context = context,
                    eventId = event.id,
                    title = event.title,
                    bsDateLabel = bsLabel,
                    note = event.note
                )

                ReminderScheduler.scheduleNextReminder(
                    context = context,
                    event = event,
                    strategy = settings.missingDayStrategy,
                    now = ZonedDateTime.now().plusMinutes(1)
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
