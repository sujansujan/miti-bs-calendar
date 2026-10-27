# Keep Room entities & DAOs
-keep class np.com.miti.patro.data.local.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }

# Keep BroadcastReceivers
-keep class np.com.miti.patro.reminder.ReminderReceiver { *; }
-keep class np.com.miti.patro.reminder.BootCompletedReceiver { *; }

# Kotlin coroutines
-dontwarn kotlinx.coroutines.**
