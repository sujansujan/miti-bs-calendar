package np.com.miti.patro.ui.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import np.com.miti.patro.data.local.AppDatabase
import np.com.miti.patro.data.local.EventEntity
import np.com.miti.patro.data.preferences.ThemeMode
import np.com.miti.patro.data.preferences.UserPreferencesRepository
import np.com.miti.patro.data.preferences.UserSettings
import np.com.miti.patro.data.repository.EventRepository
import np.com.miti.patro.data.repository.ResolvedEventInstance
import np.com.miti.patro.domain.engine.BsCalendarData
import np.com.miti.patro.domain.engine.BsDateConverter
import np.com.miti.patro.domain.engine.NepaliFestivals
import np.com.miti.patro.domain.engine.TithiCalculator
import np.com.miti.patro.domain.model.BsCalendarDay
import np.com.miti.patro.domain.model.BsDate
import np.com.miti.patro.domain.model.MissingDayStrategy
import np.com.miti.patro.reminder.ReminderScheduler
import np.com.miti.patro.sync.GoogleCalendarSyncHelper
import np.com.miti.patro.ui.theme.AppStrings
import java.time.LocalDate

data class CalendarMonthUiState(
    val visibleYear: Int,
    val visibleMonth: Int,
    val daysInMonth: Int,
    val leadingEmptyCells: Int, // 0 = Sunday .. 6 = Saturday
    val days: List<BsCalendarDay>,
    val selectedDate: BsDate,
    val selectedDayModel: BsCalendarDay?,
    val todayBsDate: BsDate,
    val adMonthSpanLabel: String,
    val eventsByDay: Map<Int, List<ResolvedEventInstance>>,
    val allEvents: List<EventEntity>,
    val settings: UserSettings,
    val statusBannerMessage: String? = null
)

class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val database = AppDatabase.getInstance(appContext)
    private val eventRepository = EventRepository(database.eventDao())
    private val prefsRepository = UserPreferencesRepository(appContext)

    private val todayAd: LocalDate
        get() = LocalDate.now()

    private val initialTodayBs: BsDate = BsDateConverter.adToBs(LocalDate.now())

    private val _visibleYearMonth = MutableStateFlow(initialTodayBs.year to initialTodayBs.month)
    private val _selectedDate = MutableStateFlow(initialTodayBs)
    private val _statusBanner = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CalendarMonthUiState> = combine(
        _visibleYearMonth,
        _selectedDate,
        eventRepository.allEventsFlow,
        prefsRepository.settingsFlow,
        _statusBanner
    ) { (year, month), selected, events, settings, banner ->
        buildMonthUiState(
            year = year,
            month = month,
            selected = selected,
            allEvents = events,
            settings = settings,
            statusBannerMessage = banner
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = buildMonthUiState(
            year = initialTodayBs.year,
            month = initialTodayBs.month,
            selected = initialTodayBs,
            allEvents = emptyList(),
            settings = UserSettings(),
            statusBannerMessage = null
        )
    )

    private fun buildMonthUiState(
        year: Int,
        month: Int,
        selected: BsDate,
        allEvents: List<EventEntity>,
        settings: UserSettings,
        statusBannerMessage: String?
    ): CalendarMonthUiState {
        val ne = settings.useDevanagari
        val todayBs = BsDateConverter.adToBs(todayAd)
        val totalDays = BsCalendarData.getDaysInMonth(year, month)
        val dayModels = ArrayList<BsCalendarDay>(totalDays)

        for (day in 1..totalDays) {
            val bsDate = BsDate(year, month, day)
            val adDate = BsDateConverter.bsToAd(bsDate)
            val tithi = TithiCalculator.calculateTithi(bsDate, adDate)
            val festivals = NepaliFestivals.getFestivalsForDay(bsDate, tithi)
            dayModels.add(
                BsCalendarDay(
                    bsDate = bsDate,
                    adDate = adDate,
                    dayOfWeek = adDate.dayOfWeek,
                    tithi = tithi,
                    festivals = festivals,
                    isToday = (bsDate == todayBs)
                )
            )
        }

        val firstDayAd = dayModels.first().adDate
        val lastDayAd = dayModels.last().adDate
        val leadingEmpty = firstDayAd.dayOfWeek.value % 7

        val firstMonthShort = BsCalendarData.getAdMonthName(firstDayAd.monthValue, ne)
        val lastMonthShort = BsCalendarData.getAdMonthName(lastDayAd.monthValue, ne)
        val y1 = BsCalendarData.formatNumber(firstDayAd.year, ne)
        val y2 = BsCalendarData.formatNumber(lastDayAd.year, ne)

        val adSpan = if (firstDayAd.year == lastDayAd.year) {
            if (firstDayAd.monthValue == lastDayAd.monthValue) {
                "$firstMonthShort $y1"
            } else {
                "$firstMonthShort/$lastMonthShort $y1"
            }
        } else {
            "$firstMonthShort $y1 – $lastMonthShort $y2"
        }

        val effectiveSelected = if (selected.year == year && selected.month == month) {
            BsDate(year, month, selected.day.coerceIn(1, totalDays))
        } else {
            selected
        }

        val selectedModel = dayModels.firstOrNull { it.bsDate == effectiveSelected } ?: run {
            val ad = BsDateConverter.bsToAd(effectiveSelected)
            val tithi = TithiCalculator.calculateTithi(effectiveSelected, ad)
            BsCalendarDay(
                bsDate = effectiveSelected,
                adDate = ad,
                dayOfWeek = ad.dayOfWeek,
                tithi = tithi,
                festivals = NepaliFestivals.getFestivalsForDay(effectiveSelected, tithi),
                isToday = (effectiveSelected == todayBs)
            )
        }

        val eventsMap = eventRepository.resolveEventsForMonth(
            allEvents = allEvents,
            bsYear = year,
            bsMonth = month,
            strategy = settings.missingDayStrategy
        )

        return CalendarMonthUiState(
            visibleYear = year,
            visibleMonth = month,
            daysInMonth = totalDays,
            leadingEmptyCells = leadingEmpty,
            days = dayModels,
            selectedDate = effectiveSelected,
            selectedDayModel = selectedModel,
            todayBsDate = todayBs,
            adMonthSpanLabel = adSpan,
            eventsByDay = eventsMap,
            allEvents = allEvents,
            settings = settings,
            statusBannerMessage = statusBannerMessage
        )
    }

    fun selectDay(bsDate: BsDate) {
        _selectedDate.value = bsDate
        if (_visibleYearMonth.value != (bsDate.year to bsDate.month)) {
            _visibleYearMonth.value = bsDate.year to bsDate.month
        }
    }

    fun prevMonth() {
        val (y, m) = _visibleYearMonth.value
        val (newY, newM) = if (m == 1) (y - 1) to 12 else y to (m - 1)
        if (newY >= BsCalendarData.MIN_YEAR) {
            _visibleYearMonth.value = newY to newM
            val maxD = BsCalendarData.getDaysInMonth(newY, newM)
            _selectedDate.value = BsDate(newY, newM, _selectedDate.value.day.coerceIn(1, maxD))
        }
    }

    fun nextMonth() {
        val (y, m) = _visibleYearMonth.value
        val (newY, newM) = if (m == 12) (y + 1) to 1 else y to (m + 1)
        if (newY <= BsCalendarData.MAX_YEAR) {
            _visibleYearMonth.value = newY to newM
            val maxD = BsCalendarData.getDaysInMonth(newY, newM)
            _selectedDate.value = BsDate(newY, newM, _selectedDate.value.day.coerceIn(1, maxD))
        }
    }

    fun prevYear() {
        val (y, m) = _visibleYearMonth.value
        val newY = (y - 1).coerceAtLeast(BsCalendarData.MIN_YEAR)
        _visibleYearMonth.value = newY to m
        val maxD = BsCalendarData.getDaysInMonth(newY, m)
        _selectedDate.value = BsDate(newY, m, _selectedDate.value.day.coerceIn(1, maxD))
    }

    fun nextYear() {
        val (y, m) = _visibleYearMonth.value
        val newY = (y + 1).coerceAtMost(BsCalendarData.MAX_YEAR)
        _visibleYearMonth.value = newY to m
        val maxD = BsCalendarData.getDaysInMonth(newY, m)
        _selectedDate.value = BsDate(newY, m, _selectedDate.value.day.coerceIn(1, maxD))
    }

    fun jumpToYearMonth(year: Int, month: Int) {
        val clampedY = year.coerceIn(BsCalendarData.MIN_YEAR, BsCalendarData.MAX_YEAR)
        val clampedM = month.coerceIn(1, 12)
        _visibleYearMonth.value = clampedY to clampedM
        val maxD = BsCalendarData.getDaysInMonth(clampedY, clampedM)
        _selectedDate.value = BsDate(clampedY, clampedM, _selectedDate.value.day.coerceIn(1, maxD))
    }

    fun jumpToToday() {
        val todayBs = BsDateConverter.adToBs(todayAd)
        _visibleYearMonth.value = todayBs.year to todayBs.month
        _selectedDate.value = todayBs
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun saveEvent(event: EventEntity) {
        viewModelScope.launch {
            val savedId = eventRepository.saveEvent(event)
            val savedEvent = event.copy(id = savedId)
            val settings = prefsRepository.getSettingsOnce()
            if (savedEvent.reminderEnabled) {
                ReminderScheduler.scheduleNextReminder(
                    context = appContext,
                    event = savedEvent,
                    strategy = settings.missingDayStrategy
                )
            } else {
                ReminderScheduler.cancelReminder(appContext, savedId)
            }

            if (settings.syncToGoogleCalendar && GoogleCalendarSyncHelper.hasCalendarPermissions(appContext)) {
                withContext(Dispatchers.IO) {
                    GoogleCalendarSyncHelper.syncSingleEvent(
                        context = appContext,
                        event = savedEvent,
                        strategy = settings.missingDayStrategy,
                        yearsAhead = settings.googleSyncYearsAhead,
                        useDevanagari = settings.useDevanagari
                    )
                }
            }
        }
    }

    fun deleteEvent(event: EventEntity) {
        viewModelScope.launch {
            ReminderScheduler.cancelReminder(appContext, event.id)
            withContext(Dispatchers.IO) {
                GoogleCalendarSyncHelper.removeSyncedEvent(appContext, event.id)
            }
            eventRepository.deleteEvent(event)
        }
    }

    fun exportOccurrenceToGoogleCalendar(instance: ResolvedEventInstance) {
        val ne = uiState.value.settings.useDevanagari
        GoogleCalendarSyncHelper.launchAddToGoogleCalendarIntent(
            context = appContext,
            instance = instance,
            useDevanagari = ne
        )
    }

    fun syncAllEventsToGoogleCalendarNow() {
        viewModelScope.launch {
            val settings = prefsRepository.getSettingsOnce()
            val ne = settings.useDevanagari
            val events = eventRepository.getAllEventsOnce()
            val result = withContext(Dispatchers.IO) {
                GoogleCalendarSyncHelper.syncAllEvents(
                    context = appContext,
                    events = events,
                    strategy = settings.missingDayStrategy,
                    yearsAhead = settings.googleSyncYearsAhead,
                    useDevanagari = ne
                )
            }
            _statusBanner.value = when {
                result.success -> AppStrings.syncSuccessStatus(
                    ne = ne,
                    countStr = BsCalendarData.formatNumber(result.syncedOccurrences, ne),
                    calName = result.calendarDisplayName ?: "Google Calendar"
                )
                result.errorCode == GoogleCalendarSyncHelper.ErrorCode.PERMISSION_DENIED ->
                    AppStrings.syncPermissionDeniedStatus(ne)
                result.errorCode == GoogleCalendarSyncHelper.ErrorCode.NO_WRITABLE_CALENDAR ->
                    AppStrings.syncNoCalendarStatus(ne)
                else -> AppStrings.syncErrorStatus(ne)
            }
        }
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            prefsRepository.setThemeMode(mode)
        }
    }

    fun updateMissingDayStrategy(strategy: MissingDayStrategy) {
        viewModelScope.launch {
            prefsRepository.setMissingDayStrategy(strategy)
            val events = eventRepository.getAllEventsOnce()
            for (event in events) {
                if (event.reminderEnabled) {
                    ReminderScheduler.scheduleNextReminder(appContext, event, strategy)
                }
            }
        }
    }

    fun updateUseDevanagari(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepository.setUseDevanagari(enabled)
        }
    }

    fun updateShowGregorianSubDate(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepository.setShowGregorianSubDate(enabled)
        }
    }

    fun updateShowTithiInGrid(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepository.setShowTithiInGrid(enabled)
        }
    }

    fun updateDefaultReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            prefsRepository.setDefaultReminderTime(hour, minute)
        }
    }

    fun updateSyncToGoogleCalendar(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepository.setSyncToGoogleCalendar(enabled)
            if (enabled && GoogleCalendarSyncHelper.hasCalendarPermissions(appContext)) {
                syncAllEventsToGoogleCalendarNow()
            }
        }
    }

    fun updateGoogleSyncYearsAhead(years: Int) {
        viewModelScope.launch {
            prefsRepository.setGoogleSyncYearsAhead(years)
        }
    }
}
