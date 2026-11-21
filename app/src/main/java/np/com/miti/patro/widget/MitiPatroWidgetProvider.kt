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
import np.com.miti.patro.data.repository.ResolvedEventInstance
import np.com.miti.patro.domain.engine.BsCalendarData
import np.com.miti.patro.domain.engine.BsDateConverter
import np.com.miti.patro.domain.engine.NepaliFestivals
import np.com.miti.patro.domain.engine.TithiCalculator
import np.com.miti.patro.notifications.PersistentTodayNotificationManager
import np.com.miti.patro.ui.theme.AppStrings
import java.time.LocalDate

/**
 * Lightweight plaintext home-screen widget displaying today's Bikram Sambat date,
 * Lunar Tithi, Gregorian sub-date, and today's or next upcoming festival/event.
 */
class MitiPatroWidgetProvider : AppWidgetProvider() {

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
                val allEvents = app.eventRepository.getAllEventsOnce()
                val ne = settings.useDevanagari

                val todayAd = LocalDate.now()
                val todayBs = BsDateConverter.adToBs(todayAd)
                val todayTithi = TithiCalculator.calculateTithi(todayBs, todayAd)
                val todayFestivals = if (settings.showPublicFestivals) {
                    NepaliFestivals.getFestivalsForDay(todayBs, todayTithi)
                } else {
                    emptyList()
                }
                val monthEventsCache = mutableMapOf<Pair<Int, Int>, Map<Int, List<ResolvedEventInstance>>>()
                val todayMonthEvents = app.eventRepository.resolveEventsForMonth(
                    allEvents = allEvents,
                    bsYear = todayBs.year,
                    bsMonth = todayBs.month,
                    strategy = settings.missingDayStrategy
                )
                monthEventsCache[todayBs.year to todayBs.month] = todayMonthEvents
                val todayEvents = todayMonthEvents[todayBs.day].orEmpty()

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
                val purpleColor = Color.parseColor(if (isDark) "#BD93F9" else "#644AC9")
                val redColor = Color.parseColor(if (isDark) "#FF5555" else "#CB3A2A")
                val orangeColor = Color.parseColor(if (isDark) "#FFB86C" else "#A34D14")
                val greenColor = Color.parseColor(if (isDark) "#50FA7B" else "#14710A")

                val yearStr = BsCalendarData.formatNumber(todayBs.year, ne)
                val headerText = "+-- ${AppStrings.appBrand(ne)} • $yearStr"
                val dow = BsDateConverter.dayOfWeekIndex(todayAd)
                val wd = if (ne) BsCalendarData.weekdayFull(dow, true) else BsCalendarData.weekdayShort(dow, false)
                val mName = BsCalendarData.getMonthName(todayBs.month, ne)
                val dayStr = BsCalendarData.formatNumber(todayBs.day, ne)
                val bsDateLine = "$mName $dayStr • $wd"
                val adNe = ne && !settings.adDatesInEnglish
                val adStr = if (adNe) BsCalendarData.formatAdDate(todayAd, true) else BsCalendarData.formatAdDate(todayAd, false)
                val tithiLine = "${todayTithi.stripDisplay(ne)}  |  $adStr"

                val isTodayHoliday = (dow == 6) ||
                    todayFestivals.any { it.isPublicHoliday } ||
                    todayEvents.any { it.event.isPersonalHoliday }
                val bsDateColor = if (isTodayHoliday) redColor else fgColor

                var highlightText = if (ne) {
                    "आगामी ३० दिनमा कुनै पर्व वा कार्यक्रम छैन"
                } else {
                    "No upcoming events (30d)"
                }
                var highlightColor = commentColor

                if (todayEvents.isNotEmpty() || todayFestivals.isNotEmpty()) {
                    val evPart = todayEvents.joinToString(", ") { "● ${it.event.title}" }
                    val festPart = todayFestivals.joinToString(", ") {
                        "* ${if (ne) it.nepaliTitle else it.englishTitle}"
                    }
                    highlightText = listOf(evPart, festPart).filter { it.isNotEmpty() }.joinToString("  ")
                    highlightColor = when {
                        todayFestivals.any { it.isPublicHoliday } || todayEvents.any { it.event.isPersonalHoliday } -> redColor
                        todayEvents.isNotEmpty() -> greenColor
                        else -> orangeColor
                    }
                } else {
                    for (offset in 1..30) {
                        val candAd = todayAd.plusDays(offset.toLong())
                        val candBs = BsDateConverter.adToBs(candAd)
                        val monthMap = monthEventsCache.getOrPut(candBs.year to candBs.month) {
                            app.eventRepository.resolveEventsForMonth(
                                allEvents = allEvents,
                                bsYear = candBs.year,
                                bsMonth = candBs.month,
                                strategy = settings.missingDayStrategy
                            )
                        }
                        val candEvents = monthMap[candBs.day].orEmpty()
                        val candFests = if (settings.showPublicFestivals) {
                            NepaliFestivals.getFestivalsForDay(
                                candBs,
                                TithiCalculator.calculateTithi(candBs, candAd)
                            )
                        } else {
                            emptyList()
                        }
                        if (candEvents.isNotEmpty() || candFests.isNotEmpty()) {
                            val rel = AppStrings.formatRelativeDays(offset.toLong(), ne)
                            val prefix = if (ne) "आगामी ($rel)" else "Next ($rel)"
                            val itemText = candEvents.firstOrNull()?.let { "● ${it.event.title}" }
                                ?: "* ${candFests.first().let { if (ne) it.nepaliTitle else it.englishTitle }}"
                            highlightText = "$prefix: $itemText"
                            val upcomingHoliday = candFests.any { it.isPublicHoliday } ||
                                candEvents.any { it.event.isPersonalHoliday }
                            highlightColor = if (upcomingHoliday) redColor else commentColor
                            break
                        }
                    }
                }

                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(MainActivity.EXTRA_OPEN_TODAY, true)
                }
                val clickPendingIntent = PendingIntent.getActivity(
                    context,
                    101,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                for (widgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_miti_patro).apply {
                        setInt(R.id.widget_root, "setBackgroundResource", bgRes)
                        setTextViewText(R.id.widget_header, headerText)
                        setTextColor(R.id.widget_header, purpleColor)
                        setTextViewText(R.id.widget_bs_date, bsDateLine)
                        setTextColor(R.id.widget_bs_date, bsDateColor)
                        setTextViewText(R.id.widget_tithi_ad, tithiLine)
                        setTextColor(R.id.widget_tithi_ad, commentColor)
                        setTextViewText(R.id.widget_highlight, highlightText)
                        setTextColor(R.id.widget_highlight, highlightColor)
                        setOnClickPendingIntent(R.id.widget_root, clickPendingIntent)
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
            val component = ComponentName(context, MitiPatroWidgetProvider::class.java)
            return mgr.getAppWidgetIds(component).isNotEmpty()
        }

        fun requestUpdateAll(context: Context) {
            requestUpdate(context)
            MitiCompactWidgetProvider.requestUpdate(context)
        }

        fun requestUpdate(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, MitiPatroWidgetProvider::class.java)
            val ids = mgr.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                val intent = Intent(context, MitiPatroWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(intent)
            }
        }
    }
}
