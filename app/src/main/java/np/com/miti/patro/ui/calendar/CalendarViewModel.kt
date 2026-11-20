package np.com.miti.patro.ui.calendar

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import np.com.miti.patro.MitiApplication
import np.com.miti.patro.backup.BackupManager
import np.com.miti.patro.data.local.EventEntity
import np.com.miti.patro.data.preferences.NepaliScriptMode
import np.com.miti.patro.data.preferences.ThemeMode
import np.com.miti.patro.data.preferences.UserSettings
import np.com.miti.patro.data.repository.ResolvedEventInstance
import np.com.miti.patro.domain.engine.BsCalendarData
import np.com.miti.patro.domain.engine.BsDateConverter
import np.com.miti.patro.domain.engine.NepaliFestivals
import np.com.miti.patro.domain.engine.TithiCalculator
import np.com.miti.patro.domain.model.BsCalendarDay
import np.com.miti.patro.domain.model.BsDate
import np.com.miti.patro.domain.model.MissingDayStrategy
import np.com.miti.patro.notifications.AlarmScheduler
import np.com.miti.patro.notifications.PersistentTodayNotificationManager
import np.com.miti.patro.sync.GoogleCalendarSyncHelper
import np.com.miti.patro.widget.MitiPatroWidgetProvider
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

@Immutable
data class CalendarUiState(
    val viewingYear: Int,
    val viewingMonth: Int,
    val selectedBsDate: BsDate?,
    val todayBs: BsDate,
    val todayAd: LocalDate,
    val todayDay: BsCalendarDay,
    val todayEvents: List<ResolvedEventInstance>,
    val daysInMonthGrid: List<BsCalendarDay?>,
    val eventsByDayInMonth: Map<Int, List<ResolvedEventInstance>>,
    val allEvents: List<EventEntity>,
    val settings: UserSettings,
    val statusBannerMessage: String? = null
) {
    val effectiveAnchorBsDate: BsDate
        get() = selectedBsDate ?: todayBs
}

class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MitiApplication
    private val eventRepo = app.eventRepository
    private val prefsRepo = app.userPreferencesRepository

    private val initialTodayBs = BsDateConverter.todayBs()
    private val _viewingYearMonth = MutableStateFlow(initialTodayBs.year to initialTodayBs.month)
    private val _selectedBsDate = MutableStateFlow<BsDate?>(null)
    private val _statusBanner = MutableStateFlow<String?>(null)

    private data class MonthGridCacheKey(
        val year: Int,
        val month: Int,
        val todayBs: BsDate
    )

    private val gridCache = ConcurrentHashMap<MonthGridCacheKey, List<BsCalendarDay?>>(48)

    val uiState: StateFlow<CalendarUiState> = combine(
        _viewingYearMonth,
        _selectedBsDate,
        eventRepo.allEventsFlow,
        prefsRepo.userSettingsFlow,
        _statusBanner
    ) { (year, month), selectedDate, allEvents, settings, banner ->
        val todayAd = LocalDate.now()
        val todayBs = BsDateConverter.adToBs(todayAd)

        val gridCells = getMonthGridForPage(year, month, todayBs)

        val eventsMap = eventRepo.resolveEventsForMonth(
            allEvents = allEvents,
            bsYear = year,
            bsMonth = month,
            strategy = settings.missingDayStrategy
        )

        val todayTithi = TithiCalculator.calculateTithi(todayBs, todayAd)
        val todayDow = BsDateConverter.dayOfWeekIndex(todayAd)
        val todayDayModel = BsCalendarDay(
            bsDate = todayBs,
            adDate = todayAd,
            dayOfWeek = todayDow,
            isSaturday = (todayDow == 6),
            tithi = todayTithi,
            festivals = NepaliFestivals.getFestivalsForDay(todayBs, todayTithi),
            isToday = true
        )

        val todayMonthEvents = if (year == todayBs.year && month == todayBs.month) {
            eventsMap[todayBs.day].orEmpty()
        } else {
            eventRepo.resolveEventsForMonth(
                allEvents = allEvents,
                bsYear = todayBs.year,
                bsMonth = todayBs.month,
                strategy = settings.missingDayStrategy
            )[todayBs.day].orEmpty()
        }

        CalendarUiState(
            viewingYear = year,
            viewingMonth = month,
            selectedBsDate = selectedDate,
            todayBs = todayBs,
            todayAd = todayAd,
            todayDay = todayDayModel,
            todayEvents = todayMonthEvents,
            daysInMonthGrid = gridCells,
            eventsByDayInMonth = eventsMap,
            allEvents = allEvents,
            settings = settings,
            statusBannerMessage = banner
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = createInitialState()
    )

    private fun createInitialState(): CalendarUiState {
        val todayAd = LocalDate.now()
        val todayBs = BsDateConverter.adToBs(todayAd)
        val grid = buildMonthGrid(todayBs.year, todayBs.month, todayBs)
        val todayTithi = TithiCalculator.calculateTithi(todayBs, todayAd)
        val todayDow = BsDateConverter.dayOfWeekIndex(todayAd)
        val todayDayModel = BsCalendarDay(
            bsDate = todayBs,
            adDate = todayAd,
            dayOfWeek = todayDow,
            isSaturday = (todayDow == 6),
            tithi = todayTithi,
            festivals = NepaliFestivals.getFestivalsForDay(todayBs, todayTithi),
            isToday = true
        )
        return CalendarUiState(
            viewingYear = todayBs.year,
            viewingMonth = todayBs.month,
            selectedBsDate = null,
            todayBs = todayBs,
            todayAd = todayAd,
            todayDay = todayDayModel,
            todayEvents = emptyList(),
            daysInMonthGrid = grid,
            eventsByDayInMonth = emptyMap(),
            allEvents = emptyList(),
            settings = UserSettings()
        )
    }

    private fun buildMonthGrid(year: Int, month: Int, todayBs: BsDate): List<BsCalendarDay?> {
        val totalDays = BsCalendarData.getDaysInMonth(year, month)
        val firstDayAd = BsDateConverter.bsToAd(year, month, 1)
        val leadingEmptyCells = BsDateConverter.dayOfWeekIndex(firstDayAd)

        val cells = ArrayList<BsCalendarDay?>(42)
        repeat(leadingEmptyCells) { cells.add(null) }

        for (dayNum in 1..totalDays) {
            val bs = BsDate(year, month, dayNum)
            val ad = firstDayAd.plusDays((dayNum - 1).toLong())
            val dow = (leadingEmptyCells + dayNum - 1) % 7
            val tithi = TithiCalculator.calculateTithi(bs, ad)
            val festivals = NepaliFestivals.getFestivalsForDay(bs, tithi)
            cells.add(
                BsCalendarDay(
                    bsDate = bs,
                    adDate = ad,
                    dayOfWeek = dow,
                    isSaturday = (dow == 6),
                    tithi = tithi,
                    festivals = festivals,
                    isToday = (bs == todayBs)
                )
            )
        }

        while (cells.size % 7 != 0) {
            cells.add(null)
        }
        return cells
    }

    fun getMonthGridForPage(year: Int, month: Int, todayBs: BsDate): List<BsCalendarDay?> {
        val safeYear = year.coerceIn(BsCalendarData.MIN_YEAR, BsCalendarData.MAX_YEAR)
        val safeMonth = month.coerceIn(1, 12)
        val key = MonthGridCacheKey(safeYear, safeMonth, todayBs)
        gridCache[key]?.let { return it }
        if (gridCache.size >= 48) {
            gridCache.clear()
        }
        val built = buildMonthGrid(safeYear, safeMonth, todayBs)
        gridCache[key] = built
        return built
    }

    fun getEventsForMonthPage(
        allEvents: List<EventEntity>,
        year: Int,
        month: Int,
        strategy: MissingDayStrategy
    ): Map<Int, List<ResolvedEventInstance>> {
        val safeYear = year.coerceIn(BsCalendarData.MIN_YEAR, BsCalendarData.MAX_YEAR)
        val safeMonth = month.coerceIn(1, 12)
        return eventRepo.resolveEventsForMonth(
            allEvents = allEvents,
            bsYear = safeYear,
            bsMonth = safeMonth,
            strategy = strategy
        )
    }

    fun onPagerSettledMonth(year: Int, month: Int) {
        val target = year.coerceIn(BsCalendarData.MIN_YEAR, BsCalendarData.MAX_YEAR) to month.coerceIn(1, 12)
        if (_viewingYearMonth.value != target) {
            _viewingYearMonth.value = target
            _selectedBsDate.value = null
        }
    }

    fun selectDate(bsDate: BsDate) {
        _selectedBsDate.value = if (_selectedBsDate.value == bsDate) null else bsDate
    }

    fun jumpToToday() {
        val today = BsDateConverter.todayBs()
        _viewingYearMonth.value = today.year to today.month
        _selectedBsDate.value = null
    }

    fun jumpToBsDate(bsDate: BsDate) {
        _viewingYearMonth.value = bsDate.year to bsDate.month
        _selectedBsDate.value = bsDate
    }

    fun jumpToMonthYear(year: Int, month: Int) {
        val safeYear = year.coerceIn(BsCalendarData.MIN_YEAR, BsCalendarData.MAX_YEAR)
        val safeMonth = month.coerceIn(1, 12)
        _viewingYearMonth.value = safeYear to safeMonth
        _selectedBsDate.value = null
    }

    fun previousMonth() {
        val (y, m) = _viewingYearMonth.value
        val (newY, newM) = if (m == 1) (y - 1) to 12 else y to (m - 1)
        if (newY >= BsCalendarData.MIN_YEAR) {
            _viewingYearMonth.value = newY to newM
            _selectedBsDate.value = null
        }
    }

    fun nextMonth() {
        val (y, m) = _viewingYearMonth.value
        val (newY, newM) = if (m == 12) (y + 1) to 1 else y to (m + 1)
        if (newY <= BsCalendarData.MAX_YEAR) {
            _viewingYearMonth.value = newY to newM
            _selectedBsDate.value = null
        }
    }

    fun previousYear() {
        val (y, m) = _viewingYearMonth.value
        if (y - 1 >= BsCalendarData.MIN_YEAR) {
            _viewingYearMonth.value = (y - 1) to m
            _selectedBsDate.value = null
        }
    }

    fun nextYear() {
        val (y, m) = _viewingYearMonth.value
        if (y + 1 <= BsCalendarData.MAX_YEAR) {
            _viewingYearMonth.value = (y + 1) to m
            _selectedBsDate.value = null
        }
    }

    fun dismissStatusBanner() {
        _statusBanner.value = null
    }

    fun saveEvent(event: EventEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val savedId = eventRepo.saveEvent(event)
            val savedEntity = event.copy(id = savedId)
            val settings = uiState.value.settings
            AlarmScheduler.scheduleNextOccurrence(
                context = app,
                event = savedEntity,
                strategy = settings.missingDayStrategy
            )
            if (settings.syncToGoogleCalendar && GoogleCalendarSyncHelper.hasCalendarPermissions(app)) {
                val synced = GoogleCalendarSyncHelper.syncEventAcrossYears(
                    context = app,
                    event = savedEntity,
                    strategy = settings.missingDayStrategy,
                    yearsAhead = settings.googleSyncYearsAhead,
                    useDevanagari = settings.useDevanagari
                )
                _statusBanner.value = "Saved & synced $synced yearly BS occurrences to Google Calendar."
            }
            MitiPatroWidgetProvider.requestUpdate(app)
            PersistentTodayNotificationManager.sync(app)
        }
    }

    fun deleteEvent(event: EventEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            AlarmScheduler.cancelAlarm(app, event.id)
            GoogleCalendarSyncHelper.deleteSyncedOccurrences(app, event.id)
            eventRepo.deleteEvent(event)
            MitiPatroWidgetProvider.requestUpdate(app)
            PersistentTodayNotificationManager.sync(app)
        }
    }

    fun exportSingleEventToGoogleCalendar(event: EventEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val settings = uiState.value.settings
            if (GoogleCalendarSyncHelper.hasCalendarPermissions(app)) {
                val count = GoogleCalendarSyncHelper.syncEventAcrossYears(
                    context = app,
                    event = event,
                    strategy = settings.missingDayStrategy,
                    yearsAhead = settings.googleSyncYearsAhead,
                    useDevanagari = settings.useDevanagari
                )
                if (count > 0) {
                    _statusBanner.value = "Synced '$count' yearly BS occurrences of '${event.title}' to Google Calendar."
                    return@launch
                }
            }
            GoogleCalendarSyncHelper.launchSystemCalendarInsertIntent(
                context = app,
                event = event,
                strategy = settings.missingDayStrategy,
                useDevanagari = settings.useDevanagari
            )
        }
    }

    fun syncAllEventsToGoogleCalendarNow() {
        viewModelScope.launch(Dispatchers.IO) {
            val settings = uiState.value.settings
            if (!GoogleCalendarSyncHelper.hasCalendarPermissions(app)) {
                _statusBanner.value = "Calendar permission required to batch-sync events."
                return@launch
            }
            val events = eventRepo.getAllEventsOnce()
            var totalInserted = 0
            for (ev in events) {
                totalInserted += GoogleCalendarSyncHelper.syncEventAcrossYears(
                    context = app,
                    event = ev,
                    strategy = settings.missingDayStrategy,
                    yearsAhead = settings.googleSyncYearsAhead,
                    useDevanagari = settings.useDevanagari
                )
            }
            _statusBanner.value = "Synced ${events.size} BS events ($totalInserted occurrences) to Google Calendar."
        }
    }

    fun exportBackupToUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val settings = uiState.value.settings
            val events = eventRepo.getAllEventsOnce()
            val ok = BackupManager.writeBackupToUri(app, uri, settings, events)
            _statusBanner.value = if (ok) {
                "Exported settings and ${events.size} events to JSON."
            } else {
                "Failed to export backup file."
            }
        }
    }

    fun importBackupFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val payload = BackupManager.readBackupFromUri(app, uri)
            if (payload == null) {
                _statusBanner.value = "Invalid or oversized backup JSON file."
                return@launch
            }
            prefsRepo.restoreAllSettings(payload.settings)
            val oldEvents = eventRepo.getAllEventsOnce()
            for (old in oldEvents) {
                AlarmScheduler.cancelAlarm(app, old.id)
            }
            eventRepo.replaceAllEvents(payload.events)
            val newEvents = eventRepo.getAllReminderEnabledEvents()
            for (ev in newEvents) {
                AlarmScheduler.scheduleNextOccurrence(app, ev, payload.settings.missingDayStrategy)
            }
            MitiPatroWidgetProvider.requestUpdate(app)
            PersistentTodayNotificationManager.sync(app)
            _statusBanner.value = "Restored settings and ${payload.events.size} events from JSON."
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            prefsRepo.setThemeMode(mode)
            MitiPatroWidgetProvider.requestUpdate(app)
        }
    }

    fun setMissingDayStrategy(strategy: MissingDayStrategy) {
        viewModelScope.launch(Dispatchers.IO) {
            prefsRepo.setMissingDayStrategy(strategy)
            val events = eventRepo.getAllReminderEnabledEvents()
            for (ev in events) {
                AlarmScheduler.scheduleNextOccurrence(app, ev, strategy)
            }
            MitiPatroWidgetProvider.requestUpdate(app)
            PersistentTodayNotificationManager.sync(app)
        }
    }

    fun setNepaliScriptMode(mode: NepaliScriptMode) {
        viewModelScope.launch(Dispatchers.IO) {
            prefsRepo.setNepaliScriptMode(mode)
            MitiPatroWidgetProvider.requestUpdate(app)
            PersistentTodayNotificationManager.sync(app)
        }
    }

    fun setShowGregorianSubDate(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setShowGregorianSubDate(enabled) }
    }

    fun setAdDatesInEnglish(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            prefsRepo.setAdDatesInEnglish(enabled)
            MitiPatroWidgetProvider.requestUpdate(app)
            PersistentTodayNotificationManager.sync(app)
        }
    }

    fun setShowTithiInGrid(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setShowTithiInGrid(enabled) }
    }

    fun setShowPublicFestivals(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            prefsRepo.setShowPublicFestivals(enabled)
            MitiPatroWidgetProvider.requestUpdate(app)
            PersistentTodayNotificationManager.sync(app)
        }
    }

    fun setPersistentTodayNotification(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            prefsRepo.setPersistentTodayNotification(enabled)
            PersistentTodayNotificationManager.sync(app)
        }
    }

    fun setDefaultReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch { prefsRepo.setDefaultReminderTime(hour, minute) }
    }

    fun setDefaultReminderDaysBefore(days: Int) {
        viewModelScope.launch { prefsRepo.setDefaultReminderDaysBefore(days) }
    }

    fun setSyncToGoogleCalendar(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setSyncToGoogleCalendar(enabled) }
    }

    fun setGoogleSyncYearsAhead(years: Int) {
        viewModelScope.launch { prefsRepo.setGoogleSyncYearsAhead(years) }
    }
}
