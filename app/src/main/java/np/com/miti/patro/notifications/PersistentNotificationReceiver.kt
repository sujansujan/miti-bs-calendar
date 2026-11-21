package np.com.miti.patro.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import np.com.miti.patro.widget.MitiPatroWidgetProvider

class PersistentNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != PersistentTodayNotificationManager.ACTION_REFRESH_TODAY_NOTIFICATION) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                PersistentTodayNotificationManager.sync(context)
                MitiPatroWidgetProvider.requestUpdateAll(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
