package np.com.miti.patro.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.IconCompat
import np.com.miti.patro.MainActivity
import np.com.miti.patro.R

object NotificationHelper {

    const val CHANNEL_ID = "miti_bs_event_reminders"
    const val PERSISTENT_CHANNEL_ID = "miti_persistent_today_date"
    const val PERSISTENT_NOTIFICATION_ID = 2_000_000_001

    fun createNotificationChannel(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val reminderChannel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_desc)
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(reminderChannel)

        val persistentChannel = NotificationChannel(
            PERSISTENT_CHANNEL_ID,
            context.getString(R.string.persistent_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.persistent_channel_desc)
            enableVibration(false)
            setSound(null, null)
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(persistentChannel)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun createDayNumberStatusIcon(context: Context, dayText: String): IconCompat {
        val size = 96
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val typeface = ResourcesCompat.getFont(context, R.font.jetbrains_mono_bold)
            ?: Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            this.typeface = typeface
            textAlign = Paint.Align.CENTER
            textSize = if (dayText.length <= 1) 76f else 62f
        }
        val fm = paint.fontMetrics
        val baselineY = size / 2f - (fm.ascent + fm.descent) / 2f
        canvas.drawText(dayText, size / 2f, baselineY, paint)
        return IconCompat.createWithBitmap(bitmap)
    }

    fun showPersistentTodayNotification(
        context: Context,
        dayIconText: String,
        title: String,
        summary: String,
        expandedText: String
    ) {
        if (!hasNotificationPermission(context)) return

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            PERSISTENT_NOTIFICATION_ID,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val smallIcon = try {
            createDayNumberStatusIcon(context, dayIconText)
        } catch (_: Exception) {
            IconCompat.createWithResource(context, R.drawable.ic_notification_calendar)
        }

        val notification = NotificationCompat.Builder(context, PERSISTENT_CHANNEL_ID)
            .setSmallIcon(smallIcon)
            .setContentTitle(title)
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(PERSISTENT_NOTIFICATION_ID, notification)
    }

    fun cancelPersistentTodayNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(PERSISTENT_NOTIFICATION_ID)
    }

    fun showEventReminderNotification(
        context: Context,
        eventId: Long,
        title: String,
        bsDateFormatted: String,
        note: String,
        yearsElapsed: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            eventId.toInt(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val anniversarySuffix = if (yearsElapsed > 0) " (Year #$yearsElapsed)" else ""
        val body = buildString {
            append("BS Date: ").append(bsDateFormatted).append(anniversarySuffix)
            if (note.isNotBlank()) {
                append("\n").append(note)
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_calendar)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(eventId.toInt(), notification)
    }
}
