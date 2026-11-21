package np.com.miti.patro.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import np.com.miti.patro.MitiApplication
import np.com.miti.patro.widget.MitiPatroWidgetProvider

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val validActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED
        )
        if (intent.action !in validActions) return

        val pendingResult = goAsync()
        val app = context.applicationContext as MitiApplication

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = app.userPreferencesRepository.userSettingsFlow.first()
                val events = app.eventRepository.getAllReminderEnabledEvents()
                for (event in events) {
                    AlarmScheduler.scheduleNextOccurrence(
                        context = context,
                        event = event,
                        strategy = settings.missingDayStrategy
                    )
                }
                MitiPatroWidgetProvider.requestUpdateAll(context)
                PersistentTodayNotificationManager.sync(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
