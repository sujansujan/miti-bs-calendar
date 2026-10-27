package np.com.miti.patro.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import np.com.miti.patro.data.local.AppDatabase
import np.com.miti.patro.data.preferences.UserPreferencesRepository

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_TIME_CHANGED &&
            action != Intent.ACTION_TIMEZONE_CHANGED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val prefs = UserPreferencesRepository(context).getSettingsOnce()
                val events = db.eventDao().getAllEventsOnce()
                for (event in events) {
                    if (event.reminderEnabled) {
                        ReminderScheduler.scheduleNextReminder(
                            context = context,
                            event = event,
                            strategy = prefs.missingDayStrategy
                        )
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
