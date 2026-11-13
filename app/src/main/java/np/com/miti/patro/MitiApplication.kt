package np.com.miti.patro

import android.app.Application
import np.com.miti.patro.data.local.AppDatabase
import np.com.miti.patro.data.preferences.UserPreferencesRepository
import np.com.miti.patro.data.repository.EventRepository
import np.com.miti.patro.notifications.NotificationHelper

class MitiApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val eventRepository: EventRepository by lazy { EventRepository(database.eventDao()) }
    val userPreferencesRepository: UserPreferencesRepository by lazy { UserPreferencesRepository(this) }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
    }
}
