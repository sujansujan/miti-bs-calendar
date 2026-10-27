package np.com.miti.patro

import android.app.Application
import np.com.miti.patro.reminder.NotificationHelper

class MitiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
    }
}
