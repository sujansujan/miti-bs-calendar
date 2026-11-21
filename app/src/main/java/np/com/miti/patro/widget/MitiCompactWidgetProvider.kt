package np.com.miti.patro.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import np.com.miti.patro.MainActivity
import np.com.miti.patro.MitiApplication
import np.com.miti.patro.R
import np.com.miti.patro.data.preferences.ThemeMode
import np.com.miti.patro.domain.engine.BsCalendarData
import np.com.miti.patro.domain.engine.BsDateConverter
import np.com.miti.patro.domain.engine.NepaliFestivals
import np.com.miti.patro.domain.engine.TithiCalculator
import np.com.miti.patro.notifications.PersistentTodayNotificationManager
import java.time.LocalDate

/**
 * Compact 2x1 plaintext date-badge widget showing today's BS date, weekday, and Lunar Tithi.
 */
class MitiCompactWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action == Intent.ACTION_DATE_CHANGED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            requestUpdate(context)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        if (appWidgetIds.isEmpty()) return

        val pendingResult = goAsync()
        val app = context.applicationContext as MitiApplication

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = app.userPreferencesRepository.userSettingsFlow.first()
                val ne = settings.useDevanagari
                val todayAd = LocalDate.now()
                val todayBs = BsDateConverter.adToBs(todayAd)
                val todayTithi = TithiCalculator.calculateTithi(todayBs, todayAd)
                val dow = BsDateConverter.dayOfWeekIndex(todayAd)
                val todayFestivals = if (settings.showPublicFestivals) {
                    NepaliFestivals.getFestivalsForDay(todayBs, todayTithi)
                } else {
                    emptyList()
                }

                val isSystemDark = (context.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                val isDark = when (settings.themeMode) {
                    ThemeMode.SYSTEM -> isSystemDark
                    ThemeMode.ALUCARD -> false
                    ThemeMode.DRACULA -> true
                }

                val bgRes = if (isDark) R.drawable.widget_bg_dracula else R.drawable.widget_bg_alucard
                val fgColor = Color.parseColor(if (isDark) "#F8F8F2" else "#1F1F1F")
                val commentColor = Color.parseColor(if (isDark) "#6272A4" else "#6C664B")
                val redColor = Color.parseColor(if (isDark) "#FF5555" else "#CB3A2A")
                val cyanColor = Color.parseColor(if (isDark) "#8BE9FD" else "#036A96")

                val mShort = BsCalendarData.getShortMonthName(todayBs.month, ne)
                val dayStr = BsCalendarData.formatNumber(todayBs.day, ne)
                val wd = BsCalendarData.weekdayShort(dow, ne)
                val dateLine = "$mShort $dayStr • $wd"
                val tithiLine = todayTithi.stripDisplay(ne)

                val isHoliday = (dow == 6) || todayFestivals.any { it.isPublicHoliday }
                val dateColor = if (isHoliday) redColor else fgColor

                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(MainActivity.EXTRA_OPEN_TODAY, true)
                }
                val clickPendingIntent = PendingIntent.getActivity(
                    context,
                    102,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                for (widgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_miti_compact).apply {
                        setInt(R.id.compact_widget_root, "setBackgroundResource", bgRes)
                        setTextViewText(R.id.compact_widget_bs_date, dateLine)
                        setTextColor(R.id.compact_widget_bs_date, dateColor)
                        setTextViewText(R.id.compact_widget_tithi, tithiLine)
                        setTextColor(R.id.compact_widget_tithi, if (todayTithi.isEkadashi || todayTithi.isPurnima) cyanColor else commentColor)
                        setOnClickPendingIntent(R.id.compact_widget_root, clickPendingIntent)
                    }
                    appWidgetManager.updateAppWidget(widgetId, views)
                }
                PersistentTodayNotificationManager.ensureMidnightAlarmIfNeeded(app)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        fun hasActiveWidgets(context: Context): Boolean {
            val mgr = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, MitiCompactWidgetProvider::class.java)
            return mgr.getAppWidgetIds(component).isNotEmpty()
        }

        fun requestUpdate(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, MitiCompactWidgetProvider::class.java)
            val ids = mgr.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                val intent = Intent(context, MitiCompactWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(intent)
            }
        }
    }
}
