package np.com.miti.patro.ui.calendar

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import np.com.miti.patro.data.preferences.ThemeMode
import np.com.miti.patro.data.preferences.UserPreferences
import np.com.miti.patro.data.preferences.UserPreferencesRepository
import np.com.miti.patro.data.repository.EventRepository
import np.com.miti.patro.domain.engine.BsCalendarData
import np.com.miti.patro.domain.engine.BsDateConverter
import np.com.miti.patro.domain.engine.LunarTithiCalculator
import np.com.miti.patro.domain.engine.NepaliHolidayCatalog
import np.com.miti.patro.domain.model.BsDate
import np.com.miti.patro.domain.model.BsEvent
import np.com.miti.patro.domain.model.DayCellInfo
import np.com.miti.patro.domain.model.MissingDayStrategy
import np.com.miti.patro.domain.model.MonthGridState
import np.com.miti.patro.domain.model.ResolvedBsEvent
import np.com.miti.patro.reminder.EventAlarmScheduler
import np.com.miti.patro.sync.GoogleCalendarSyncHelper
import np.com.miti.patro.ui.theme.AppStrings
import java.time.LocalDate
import java.time.ZoneId

data class CalendarUiState(
    val todayBs: BsDate,
    val selectedBsDate: BsDate,
    val monthGrid: MonthGridState,
    val preferences: UserPreferences,
    val allEvents: List<BsEvent> = emptyList(),
    val isAddEditDialogOpen: Boolean = false,
    val editingEvent: BsEvent? = null,
    val isYearMonthPickerOpen: Boolean = false,
    val statusBannerMessage: String? = null
)

class CalendarViewModel(
    private val appContext: Context,
    private val eventRepository: EventRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val alarmScheduler: EventAlarmScheduler
) : ViewModel() {

    private val nepalZone = ZoneId.of("Asia/Kathmandu")
    private val initialTodayAd = LocalDate.now(nepalZone)
    private val initialTodayBs = BsDateConverter.adToBs(initialTodayAd)

    private val viewedYear = MutableStateFlow(initialTodayBs.year)
    private val viewedMonth = MutableStateFlow(initialTodayBs.month)
    private val selectedDate = MutableStateFlow(initialTodayBs)
    private val dialogState = MutableStateFlow<Pair<Boolean, BsEvent?>>(false to null)
    private val yearMonthPickerState = MutableStateFlow(false)
    private val statusBanner = MutableStateFlow<String?>(null)

    private data class NavTuple(
        val year: Int,
        val month: Int,
        val selected: BsDate,
        val dialog: Pair<Boolean, BsEvent?>,
        val pickerOpen: Boolean
    )

    private val navFlow = combine(
        viewedYear,
        viewedMonth,
        selectedDate,
        dialogState,
        yearMonthPickerState
    ) { year, month, sel, dlg, picker ->
        NavTuple(year, month, sel, dlg, picker)
    }

    val uiState: StateFlow<CalendarUiState> = combine(
        navFlow,
        eventRepository.allEventsFlow,
        preferencesRepository.preferencesFlow,
        statusBanner
    ) { nav, events, prefs, banner ->
        val todayAd = LocalDate.now(nepalZone)
        val todayBs = BsDateConverter.adToBs(todayAd)
        val grid = buildMonthGrid(
            bsYear = nav.year,
            bsMonth = nav.month,
            todayBs = todayBs,
            allEvents = events,
            nepaliScript = prefs.useNepaliScript
        )
        val safeSelected = if (nav.selected.year == nav.year && nav.selected.month == nav.month) {
            val maxDay = BsCalendarData.getDaysInMonth(nav.year, nav.month)
            nav.selected.copy(day = nav.selected.day.coerceAtMost(maxDay))
        } else {
            BsDate(nav.year, nav.month, 1)
        }

        CalendarUiState(
            todayBs = todayBs,
            selectedBsDate = safeSelected,
            monthGrid = grid,
            preferences = prefs,
            allEvents = events,
            isAddEditDialogOpen = nav.dialog.first,
            editingEvent = nav.dialog.second,
            isYearMonthPickerOpen = nav.pickerOpen,
            statusBannerMessage = banner
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalendarUiState(
            todayBs = initialTodayBs,
            selectedBsDate = initialTodayBs,
            monthGrid = buildMonthGrid(
                bsYear = initialTodayBs.year,
                bsMonth = initialTodayBs.month,
                todayBs = initialTodayBs,
                allEvents = emptyList(),
                nepaliScript = false
            ),
            preferences = UserPreferences()
        )
    )

    fun selectDate(date: BsDate) {
        selectedDate.value = date
        if (date.year != viewedYear.value || date.month != viewedMonth.value) {
            viewedYear.value = date.year
            viewedMonth.value = date.month
        }
    }

    fun previousMonth() {
        val y = viewedYear.value
        val m = viewedMonth.value
        if (m > 1) {
            viewedMonth.value = m - 1
            selectedDate.value = BsDate(y, m - 1, 1)
        } else if (y > BsCalendarData.MIN_YEAR) {
            viewedYear.value = y - 1
            viewedMonth.value = 12
            selectedDate.value = BsDate(y - 1, 12, 1)
        }
    }

    fun nextMonth() {
        val y = viewedYear.value
        val m = viewedMonth.value
        if (m < 12) {
            viewedMonth.value = m + 1
            selectedDate.value = BsDate(y, m + 1, 1)
        } else if (y < BsCalendarData.MAX_YEAR) {
            viewedYear.value = y + 1
            viewedMonth.value = 1
            selectedDate.value = BsDate(y + 1, 1, 1)
        }
    }

    fun jumpToToday() {
        val todayBs = BsDateConverter.adToBs(LocalDate.now(nepalZone))
        viewedYear.value = todayBs.year
        viewedMonth.value = todayBs.month
        selectedDate.value = todayBs
    }

    fun jumpToYearMonth(year: Int, month: Int) {
        val clampedY = year.coerceIn(BsCalendarData.MIN_YEAR, BsCalendarData.MAX_YEAR)
        val clampedM = month.coerceIn(1, 12)
        viewedYear.value = clampedY
        viewedMonth.value = clampedM
        selectedDate.value = BsDate(clampedY, clampedM, 1)
        yearMonthPickerState.value = false
    }

    fun openYearMonthPicker() {
        yearMonthPickerState.value = true
    }

    fun closeYearMonthPicker() {
        yearMonthPickerState.value = false
    }

    fun openAddEventDialog() {
        dialogState.value = true to null
    }

    fun openEditEventDialog(event: BsEvent) {
        dialogState.value = true to event
    }

    fun closeEventDialog() {
        dialogState.value = false to null
    }

    fun clearStatusBanner() {
        statusBanner.value = null
    }

    fun saveEvent(
        existingId: Long?,
        title: String,
        notes: String,
        bsMonth: Int,
        bsDay: Int,
        originBsYear: Int,
        missingDayStrategy: MissingDayStrategy,
        reminderEnabled: Boolean,
        reminderHour: Int,
        reminderMinute: Int
    ) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) return

        viewModelScope.launch {
            val event = BsEvent(
                id = existingId ?: 0L,
                title = trimmedTitle,
                notes = notes.trim(),
                bsMonth = bsMonth.coerceIn(1, 12),
                bsDay = bsDay.coerceIn(1, 32),
                originBsYear = originBsYear,
                missingDayStrategy = missingDayStrategy,
                reminderEnabled = reminderEnabled,
                reminderHour = reminderHour.coerceIn(0, 23),
                reminderMinute = reminderMinute.coerceIn(0, 59),
                createdAtEpochMillis = System.currentTimeMillis()
            )
            val savedId = if (existingId == null || existingId == 0L) {
                eventRepository.insertEvent(event)
            } else {
                eventRepository.updateEvent(event)
                existingId
            }
            val savedEvent = event.copy(id = savedId)
            alarmScheduler.scheduleNextOccurrence(savedEvent)

            val prefs = preferencesRepository.preferencesFlow.first()
            if (prefs.syncToGoogleCalendar && GoogleCalendarSyncHelper.hasCalendarPermissions(appContext)) {
                withContext(Dispatchers.IO) {
                    GoogleCalendarSyncHelper.syncSingleEvent(
                        context = appContext,
                        event = savedEvent,
                        yearsAhead = prefs.googleSyncYearsAhead,
                        nepaliScript = prefs.useNepaliScript
                    )
                }
            }
            dialogState.value = false to null
        }
    }

    fun deleteEvent(event: BsEvent) {
        viewModelScope.launch {
            alarmScheduler.cancelAlarm(event.id)
            withContext(Dispatchers.IO) {
                GoogleCalendarSyncHelper.removeSyncedEvent(appContext, event.id)
            }
            eventRepository.deleteEvent(event)
        }
    }

    fun exportOccurrenceToGoogleCalendar(resolved: ResolvedBsEvent, adDate: LocalDate, nepaliScript: Boolean) {
        GoogleCalendarSyncHelper.launchAddToGoogleCalendarIntent(
            context = appContext,
            event = resolved.event,
            resolvedBsDate = resolved.resolvedDate,
            adDate = adDate,
            nepaliScript = nepaliScript
        )
    }

    fun syncAllEventsToGoogleCalendarNow() {
        viewModelScope.launch {
            val prefs = preferencesRepository.preferencesFlow.first()
            val ne = prefs.useNepaliScript
            val events = eventRepository.getAllEventsSnapshot()
            val result = withContext(Dispatchers.IO) {
                GoogleCalendarSyncHelper.syncAllEvents(
                    context = appContext,
                    events = events,
                    yearsAhead = prefs.googleSyncYearsAhead,
                    nepaliScript = ne
                )
            }
            statusBanner.value = when {
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

    // Settings mutations
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun setUseNepaliScript(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setUseNepaliScript(enabled) }
    }

    fun setShowGregorianSubDate(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setShowGregorianSubDate(enabled) }
    }

    fun setShowTithiInGrid(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setShowTithiInGrid(enabled) }
    }

    fun setDefaultMissingDayStrategy(strategy: MissingDayStrategy) {
        viewModelScope.launch { preferencesRepository.setDefaultMissingDayStrategy(strategy) }
    }

    fun setDefaultReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch { preferencesRepository.setDefaultReminderTime(hour, minute) }
    }

    fun setSyncToGoogleCalendar(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setSyncToGoogleCalendar(enabled)
            if (enabled && GoogleCalendarSyncHelper.hasCalendarPermissions(appContext)) {
                syncAllEventsToGoogleCalendarNow()
            }
        }
    }

    fun setGoogleSyncYearsAhead(years: Int) {
        viewModelScope.launch {
            preferencesRepository.setGoogleSyncYearsAhead(years)
        }
    }

    private fun buildMonthGrid(
        bsYear: Int,
        bsMonth: Int,
        todayBs: BsDate,
        allEvents: List<BsEvent>,
        nepaliScript: Boolean
    ): MonthGridState {
        val totalDays = BsCalendarData.getDaysInMonth(bsYear, bsMonth)
        val firstBs = BsDate(bsYear, bsMonth, 1)
        val lastBs = BsDate(bsYear, bsMonth, totalDays)
        val firstAd = BsDateConverter.bsToAd(firstBs)
        val lastAd = BsDateConverter.bsToAd(lastBs)
        val leadingEmpty = BsDateConverter.dayOfWeekIndex(firstAd)

        // Pre-resolve all user events that land in this (bsYear, bsMonth)
        val resolvedByDay = mutableMapOf<Int, MutableList<ResolvedBsEvent>>()
        for (event in allEvents) {
            if (bsYear < event.originBsYear) continue
            val resolvedDate = BsDateConverter.resolveAnnualOccurrence(
                targetYear = bsYear,
                bsMonth = event.bsMonth,
                bsDay = event.bsDay,
                strategy = event.missingDayStrategy
            )
            if (resolvedDate != null && resolvedDate.year == bsYear && resolvedDate.month == bsMonth) {
                val isClamped = resolvedDate.day != event.bsDay || resolvedDate.month != event.bsMonth
                resolvedByDay.getOrPut(resolvedDate.day) { mutableListOf() }
                    .add(ResolvedBsEvent(event = event, resolvedDate = resolvedDate, isClampedOrRolled = isClamped))
            }
        }

        val cells = ArrayList<DayCellInfo>(totalDays)
        for (day in 1..totalDays) {
            val currentBs = BsDate(bsYear, bsMonth, day)
            val currentAd = firstAd.plusDays((day - 1).toLong())
            val dow = (leadingEmpty + (day - 1)) % 7
            val isSaturday = dow == 6
            val tithi = LunarTithiCalculator.calculateTithiAtSunrise(currentAd)
            val holidays = NepaliHolidayCatalog.getHolidaysForDay(bsMonth, day, tithi)
            val dayEvents = resolvedByDay[day] ?: emptyList()

            cells.add(
                DayCellInfo(
                    bsDate = currentBs,
                    adDate = currentAd,
                    dayOfWeekIndex = dow,
                    isSaturday = isSaturday,
                    isToday = currentBs == todayBs,
                    tithi = tithi,
                    holidays = holidays,
                    events = dayEvents
                )
            )
        }

        val m1 = BsCalendarData.getAdMonthName(firstAd.monthValue, nepaliScript)
        val m2 = BsCalendarData.getAdMonthName(lastAd.monthValue, nepaliScript)
        val y1 = BsCalendarData.formatNumber(firstAd.year, nepaliScript)
        val y2 = BsCalendarData.formatNumber(lastAd.year, nepaliScript)

        val adSpan = if (firstAd.year == lastAd.year) {
            if (firstAd.month == lastAd.month) "$m1 $y1" else "$m1/$m2 $y1"
        } else {
            "$m1 $y1 – $m2 $y2"
        }

        return MonthGridState(
            bsYear = bsYear,
            bsMonth = bsMonth,
            totalDays = totalDays,
            leadingEmptyCells = leadingEmpty,
            adMonthSpanLabel = adSpan,
            days = cells
        )
    }

    class Factory(
        private val appContext: Context,
        private val eventRepository: EventRepository,
        private val preferencesRepository: UserPreferencesRepository,
        private val alarmScheduler: EventAlarmScheduler
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CalendarViewModel(appContext, eventRepository, preferencesRepository, alarmScheduler) as T
        }
    }
}
