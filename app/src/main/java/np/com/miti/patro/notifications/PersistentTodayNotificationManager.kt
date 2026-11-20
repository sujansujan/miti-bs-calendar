package np.com.miti.patro.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import np.com.miti.patro.MitiApplication
import np.com.miti.patro.data.repository.ResolvedEventInstance
import np.com.miti.patro.domain.engine.BsCalendarData
import np.com.miti.patro.domain.engine.BsDateConverter
import np.com.miti.patro.domain.engine.NepaliFestivals
import np.com.miti.patro.domain.engine.TithiCalculator
import np.com.miti.patro.ui.theme.AppStrings
import java.time.LocalDate
import java.time.ZoneId

/**
 * Manages the optional silent ongoing notification showing today's Bikram Sambat date,
 * Lunar Tithi, Gregorian sub-date, and today's (or next upcoming) festivals and events.
 */
object PersistentTodayNotificationManager {

    const val ACTION_REFRESH_TODAY_NOTIFICATION = "np.com.miti.patro.action.REFRESH_TODAY_NOTIFICATION"
    private const val MIDNIGHT_ALARM_REQUEST_CODE = 2_000_000_002

    fun syncAsync(context: Context) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            sync(appContext)
        }
    }

    suspend fun sync(context: Context) {
        val app = context.applicationContext as MitiApplication
        val settings = app.userPreferencesRepository.userSettingsFlow.first()

        if (!settings.persistentTodayNotification) {
            cancel(app)
            return
        }

        if (!NotificationHelper.hasNotificationPermission(app)) {
            return
        }

        val ne = settings.useDevanagari
        val adNe = ne && !settings.adDatesInEnglish
        val allEvents = app.eventRepository.getAllEventsOnce()

        val todayAd = LocalDate.now()
        val todayBs = BsDateConverter.adToBs(todayAd)
        val todayTithi = TithiCalculator.calculateTithi(todayBs, todayAd)
        val todayDow = BsDateConverter.dayOfWeekIndex(todayAd)
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

        val wd = if (ne) BsCalendarData.weekdayFull(todayDow, true) else BsCalendarData.weekdayShort(todayDow, false)
        val title = if (ne) {
            "${todayBs.formatHuman(true)}, $wd"
        } else {
            "${todayBs.formatHuman(false)} ($wd)"
        }

        val tithiLine = todayTithi.stripDisplay(ne)
        val adStr = if (adNe) BsCalendarData.formatAdDate(todayAd, true) else BsCalendarData.formatAdDate(todayAd, false)

        val todayEventText = todayEvents.joinToString(", ") { "● ${it.event.title}" }
        val todayFestText = todayFestivals.joinToString(", ") {
            "* ${if (ne) it.nepaliTitle else it.englishTitle}"
        }
        val todayCombined = listOf(todayEventText, todayFestText).filter { it.isNotEmpty() }.joinToString("  ")

        val summary = if (todayCombined.isNotEmpty()) {
            "$todayCombined  |  $tithiLine"
        } else {
            "$tithiLine  |  $adStr"
        }

        val expanded = buildString {
            append("$tithiLine  •  $adStr")
            if (todayEventText.isNotEmpty()) {
                append("\n").append(todayEventText)
            }
            if (todayFestText.isNotEmpty()) {
                append("\n").append(todayFestText)
            }
            if (todayCombined.isEmpty()) {
                var nextLine: String? = null
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
                        nextLine = "$prefix: $itemText"
                        break
                    }
                }
                if (nextLine != null) {
                    append("\n").append(nextLine)
                }
            }
        }

        NotificationHelper.showPersistentTodayNotification(
            context = app,
            dayIconText = BsCalendarData.formatNumber(todayBs.day, ne),
            title = title,
            summary = summary,
            expandedText = expanded
        )
        scheduleNextMidnightRefresh(app)
    }

    fun cancel(context: Context) {
        NotificationHelper.cancelPersistentTodayNotification(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, PersistentNotificationReceiver::class.java).apply {
            action = ACTION_REFRESH_TODAY_NOTIFICATION
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            MIDNIGHT_ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun scheduleNextMidnightRefresh(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val nextMidnightMillis = LocalDate.now()
            .plusDays(1)
            .atTime(0, 0, 5)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val intent = Intent(context, PersistentNotificationReceiver::class.java).apply {
            action = ACTION_REFRESH_TODAY_NOTIFICATION
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            MIDNIGHT_ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        try {
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC,
                    nextMidnightMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC,
                    nextMidnightMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC,
                nextMidnightMillis,
                pendingIntent
            )
        }
    }
}
