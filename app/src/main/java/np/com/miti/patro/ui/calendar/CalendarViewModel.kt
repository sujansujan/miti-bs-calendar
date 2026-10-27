package np.com.miti.patro.ui.calendar

import android.content.Context
import android.net.Uri
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
import np.com.miti.patro.backup.BackupManager
import np.com.miti.patro.data.local.EventEntity
import np.com.miti.patro.data.preferences.NepaliScriptMode
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

data class CalendarUiState(
    val todayBs: BsDate,
    val todayAd: LocalDate,
    val todayDay: BsCalendarDay,
    val todayEvents: List<ResolvedEventInstance> = emptyList(),
    val viewingYear: Int,
    val viewingMonth: Int,
    val selectedBsDate: BsDate? = null,
    val daysInMonthGrid: List<BsCalendarDay?> = emptyList(),
    val eventsByDayInMonth: Map<Int, List<ResolvedEventInstance>> = emptyMap(),
    val allEvents: List<EventEntity> = emptyList(),
    val settings: UserSettings = UserSettings(),
    val statusBannerMessage: String? = null
) {
    /**
     * If a calendar date is highlighted, returns that selection; otherwise returns today's BS date.
     */
    val effectiveAnchorBsDate: BsDate
        get() = selectedBsDate ?: todayBs
}

class CalendarViewModel(
    private val appContext: Context,
    private val eventRepository: EventRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val todayAd: LocalDate = LocalDate.now()
    private val todayBs: BsDate = BsDateConverter.adToBs(todayAd)

    private val _viewingYear = MutableStateFlow(todayBs.year)
    private val _viewingMonth = MutableStateFlow(todayBs.month)
    // Null when no specific calendar cell is highlighted (defaults to Today view & Today date for [+])
    private val _selectedBsDate = MutableStateFlow<BsDate?>(null)
    private val _statusBanner = MutableStateFlow<String?>(null)
    private var cachedGridKey: Pair<Int, Int>? = null
    private var cachedGridCells: List<BsCalendarDay?> = emptyList()

    val uiState: StateFlow<CalendarUiState> = combine(
        _viewingYear,
        _viewingMonth,
        _selectedBsDate,
        eventRepository.allEvents,
        preferencesRepository.userSettingsFlow
    ) { year, month, selectedDate, events, settings ->
        buildMonthUiState(
            year = year,
            month = month,
            selectedBsDate = selectedDate,
            events = events,
            settings = settings,
            banner = _statusBanner.value
        )
    }.combine(_statusBanner) { state, banner ->
        state.copy(statusBannerMessage = banner)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = buildMonthUiState(
            year = todayBs.year,
            month = todayBs.month,
            selectedBsDate = null,
            events = emptyList(),
            settings = UserSettings(),
            banner = null
        )
    )

    private fun buildDayModel(bsDate: BsDate, adDate: LocalDate): BsCalendarDay {
        val dow = BsDateConverter.dayOfWeekIndex(adDate)
        val tithi = TithiCalculator.calculateTithi(bsDate, adDate)
        val festivals = NepaliFestivals.getFestivalsForDay(bsDate, tithi)
        return BsCalendarDay(
            bsDate = bsDate,
            adDate = adDate,
            dayOfWeek = dow,
            isSaturday = (dow == 6),
            tithi = tithi,
            festivals = festivals,
            isToday = (bsDate == todayBs)
        )
    }

    private fun buildMonthUiState(
        year: Int,
        month: Int,
        selectedBsDate: BsDate?,
        events: List<EventEntity>,
        settings: UserSettings,
        banner: String?
    ): CalendarUiState {
        val totalDays = BsCalendarData.daysInMonth(year, month)
        val firstDayBs = BsDate(year, month, 1)
        val firstDayAd = BsDateConverter.bsToAd(firstDayBs)
        val firstDayOfWeek = BsDateConverter.dayOfWeekIndex(firstDayAd)

        val resolvedInMonth = eventRepository.resolveEventsForBsMonth(
            allEvents = events,
            bsYear = year,
            bsMonth = month,
            strategy = settings.missingDayStrategy
        )
        val eventsByDay = resolvedInMonth.groupBy { it.resolvedBsDate.day }

        val cells = ArrayList<BsCalendarDay?>(firstDayOfWeek + totalDays)
        repeat(firstDayOfWeek) { cells.add(null) }

        for (day in 1..totalDays) {
            val bsDate = BsDate(year, month, day)
            val adDate = firstDayAd.plusDays((day - 1).toLong())
            val dow = (firstDayOfWeek + day - 1) % 7
            val isSaturday = dow == 6
            val tithi = TithiCalculator.calculateTithi(bsDate, adDate)
            val festivals = NepaliFestivals.getFestivalsForDay(bsDate, tithi)

            cells.add(
                BsCalendarDay(
                    bsDate = bsDate,
                    adDate = adDate,
                    dayOfWeek = dow,
                    isSaturday = isSaturday,
                    tithi = tithi,
                    festivals = festivals,
                    isToday = bsDate == todayBs
                )
            )
        }

        while (cells.size % 7 != 0) {
            cells.add(null)
        }

        val todayDayModel = buildDayModel(todayBs, todayAd)
        val todayEventsList = if (year == todayBs.year && month == todayBs.month) {
            eventsByDay[todayBs.day].orEmpty()
        } else {
            eventRepository.resolveEventsForBsMonth(
                allEvents = events,
                bsYear = todayBs.year,
                bsMonth = todayBs.month,
                strategy = settings.missingDayStrategy
            ).filter { it.resolvedBsDate.day == todayBs.day }
        }

        val validSelected = if (selectedBsDate != null &&
            selectedBsDate.year == year &&
            selectedBsDate.month == month
        ) {
            selectedBsDate.copy(day = selectedBsDate.day.coerceAtMost(totalDays))
        } else {
            null
        }

        return CalendarUiState(
            todayBs = todayBs,
            todayAd = todayAd,
            todayDay = todayDayModel,
            todayEvents = todayEventsList,
            viewingYear = year,
            viewingMonth = month,
            selectedBsDate = validSelected,
            daysInMonthGrid = cells,
            eventsByDayInMonth = eventsByDay,
            allEvents = events,
            settings = settings,
            statusBannerMessage = banner
        )
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun setStatusBanner(message: String) {
        _statusBanner.value = message
    }

    fun previousMonth() {
        val curY = _viewingYear.value
        val curM = _viewingMonth.value
        if (curM > 1) {
            _viewingMonth.value = curM - 1
        } else if (curY > BsCalendarData.MIN_YEAR) {
            _viewingYear.value = curY - 1
            _viewingMonth.value = 12
        }
        _selectedBsDate.value = null
    }

    fun nextMonth() {
        val curY = _viewingYear.value
        val curM = _viewingMonth.value
        if (curM < 12) {
            _viewingMonth.value = curM + 1
        } else if (curY < BsCalendarData.MAX_YEAR) {
            _viewingYear.value = curY + 1
            _viewingMonth.value = 1
        }
        _selectedBsDate.value = null
    }

    fun previousYear() {
        val nextY = (_viewingYear.value - 1).coerceAtLeast(BsCalendarData.MIN_YEAR)
        _viewingYear.value = nextY
        _selectedBsDate.value = null
    }

    fun nextYear() {
        val nextY = (_viewingYear.value + 1).coerceAtMost(BsCalendarData.MAX_YEAR)
        _viewingYear.value = nextY
        _selectedBsDate.value = null
    }

    fun jumpToMonthYear(year: Int, month: Int) {
        val safeY = year.coerceIn(BsCalendarData.MIN_YEAR, BsCalendarData.MAX_YEAR)
        val safeM = month.coerceIn(1, 12)
        _viewingYear.value = safeY
        _viewingMonth.value = safeM
        _selectedBsDate.value = null
    }

    fun jumpToToday() {
        val currentAd = LocalDate.now()
        val currentBs = BsDateConverter.adToBs(currentAd)
        _viewingYear.value = currentBs.year
        _viewingMonth.value = currentBs.month
        _selectedBsDate.value = null
    }

    /**
     * Toggles selection on a date cell: tapping an unselected date highlights it;
     * tapping the already-highlighted date clears the highlight back to Today.
     */
    fun selectDate(bsDate: BsDate) {
        _viewingYear.value = bsDate.year
        _viewingMonth.value = bsDate.month
        _selectedBsDate.value = if (_selectedBsDate.value == bsDate) null else bsDate
    }

    fun saveEvent(event: EventEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val savedId = eventRepository.saveEvent(event)
            val savedEvent = event.copy(id = savedId)
            val currentSettings = preferencesRepository.userSettingsFlow.first()
            ReminderScheduler.scheduleNextReminder(appContext, savedEvent, currentSettings.missingDayStrategy)

            if (currentSettings.syncToGoogleCalendar) {
                GoogleCalendarSyncHelper.syncSingleEvent(
                    context = appContext,
                    event = savedEvent,
                    strategy = currentSettings.missingDayStrategy,
                    yearsAhead = currentSettings.googleSyncYearsAhead,
                    useDevanagari = currentSettings.useDevanagariEverywhere
                )
            }
        }
    }

    fun deleteEvent(event: EventEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            ReminderScheduler.cancelReminder(appContext, event.id)
            GoogleCalendarSyncHelper.removeSyncedEvent(appContext, event.id)
            eventRepository.deleteEvent(event)
        }
    }

    fun syncSingleEventToGoogleCalendarNow(event: EventEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val settings = preferencesRepository.userSettingsFlow.first()
            val ne = settings.useDevanagariEverywhere
            val result = GoogleCalendarSyncHelper.syncSingleEvent(
                context = appContext,
                event = event,
                strategy = settings.missingDayStrategy,
                yearsAhead = settings.googleSyncYearsAhead,
                useDevanagari = ne
            )
            _statusBanner.value = when (result.errorCode) {
                GoogleCalendarSyncHelper.ErrorCode.PERMISSION_DENIED -> AppStrings.gcalPermDeniedMsg(ne)
                GoogleCalendarSyncHelper.ErrorCode.NO_WRITABLE_CALENDAR -> AppStrings.gcalNoWritableCalMsg(ne)
                GoogleCalendarSyncHelper.ErrorCode.PROVIDER_ERROR -> AppStrings.gcalNoWritableCalMsg(ne)
                null -> AppStrings.gcalSingleSyncSuccessMsg(
                    ne = ne,
                    title = event.title,
                    occurrences = result.syncedOccurrences,
                    calName = result.calendarDisplayName ?: "Google Calendar",
                    preview = result.sampleDatesPreview
                )
            }
        }
    }

    fun syncAllEventsToGoogleCalendarNow() {
        viewModelScope.launch(Dispatchers.IO) {
            val settings = preferencesRepository.userSettingsFlow.first()
            val ne = settings.useDevanagariEverywhere
            val events = eventRepository.getAllEventsSnapshot()
            val result = GoogleCalendarSyncHelper.syncAllEvents(
                context = appContext,
                events = events,
                strategy = settings.missingDayStrategy,
                yearsAhead = settings.googleSyncYearsAhead,
                useDevanagari = ne
            )
            _statusBanner.value = when (result.errorCode) {
                GoogleCalendarSyncHelper.ErrorCode.PERMISSION_DENIED -> AppStrings.gcalPermDeniedMsg(ne)
                GoogleCalendarSyncHelper.ErrorCode.NO_WRITABLE_CALENDAR -> AppStrings.gcalNoWritableCalMsg(ne)
                GoogleCalendarSyncHelper.ErrorCode.PROVIDER_ERROR -> AppStrings.gcalNoWritableCalMsg(ne)
                null -> AppStrings.gcalSyncSuccessMsg(
                    ne = ne,
                    eventsCount = events.size,
                    occurrences = result.syncedOccurrences,
                    calName = result.calendarDisplayName ?: "Google Calendar",
                    preview = result.sampleDatesPreview
                )
            }
        }
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun updateMissingDayStrategy(strategy: MissingDayStrategy) {
        viewModelScope.launch { preferencesRepository.setDefaultMissingDayStrategy(strategy) }
    }

    fun updateShowTithiInGrid(show: Boolean) {
        viewModelScope.launch { preferencesRepository.setShowTithiInGrid(show) }
    }

    fun updateShowGregorianInGrid(show: Boolean) {
        viewModelScope.launch { preferencesRepository.setShowGregorianInGrid(show) }
    }

    fun updateShowPublicFestivals(show: Boolean) {
        viewModelScope.launch { preferencesRepository.setShowPublicFestivals(show) }
    }

    fun updateNepaliScriptMode(mode: NepaliScriptMode) {
        viewModelScope.launch { preferencesRepository.setNepaliScriptMode(mode) }
    }

    fun updateDefaultReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch { preferencesRepository.setDefaultReminderTime(hour, minute) }
    }

    fun updateDefaultReminderDaysBefore(days: Int) {
        viewModelScope.launch { preferencesRepository.setDefaultReminderDaysBefore(days) }
    }

    fun updateSyncToGoogleCalendar(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            preferencesRepository.setSyncToGoogleCalendar(enabled)
            if (enabled) {
                val settings = preferencesRepository.userSettingsFlow.first()
                val events = eventRepository.getAllEventsSnapshot()
                GoogleCalendarSyncHelper.syncAllEvents(
                    context = appContext,
                    events = events,
                    strategy = settings.missingDayStrategy,
                    yearsAhead = settings.googleSyncYearsAhead,
                    useDevanagari = settings.useDevanagariEverywhere
                )
            }
        }
    }

    fun updateGoogleSyncYearsAhead(years: Int) {
        viewModelScope.launch { preferencesRepository.setGoogleSyncYearsAhead(years) }
    }

    fun exportBackupToUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val settings = preferencesRepository.userSettingsFlow.first()
            val ne = settings.useDevanagariEverywhere
            val events = eventRepository.getAllEventsSnapshot()
            val ok = BackupManager.writeBackupToUri(
                context = appContext,
                uri = uri,
                settings = settings,
                events = events
            )
            _statusBanner.value = if (ok) {
                AppStrings.exportSuccessMsg(ne, events.size)
            } else {
                AppStrings.exportErrorMsg(ne)
            }
        }
    }

    fun importBackupFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentSettings = preferencesRepository.userSettingsFlow.first()
            val ne = currentSettings.useDevanagariEverywhere
            val payload = BackupManager.readBackupFromUri(appContext, uri)
            if (payload == null) {
                _statusBanner.value = AppStrings.importErrorMsg(ne)
                return@launch
            }

            preferencesRepository.restoreSettings(payload.settings)
            val existing = eventRepository.getAllEventsSnapshot()
            var importedCount = 0

            for (incoming in payload.events) {
                val match = existing.firstOrNull {
                    it.title.equals(incoming.title, ignoreCase = true) &&
                        it.bsMonth == incoming.bsMonth &&
                        it.bsDay == incoming.bsDay
                }
                val toSave = if (match != null) {
                    incoming.copy(id = match.id)
                } else {
                    incoming.copy(id = 0L)
                }
                val savedId = eventRepository.saveEvent(toSave)
                val savedEntity = toSave.copy(id = savedId)
                ReminderScheduler.scheduleNextReminder(
                    context = appContext,
                    event = savedEntity,
                    strategy = payload.settings.missingDayStrategy
                )
                importedCount++
            }

            if (payload.settings.syncToGoogleCalendar &&
                GoogleCalendarSyncHelper.hasCalendarPermissions(appContext)
            ) {
                val allAfterImport = eventRepository.getAllEventsSnapshot()
                GoogleCalendarSyncHelper.syncAllEvents(
                    context = appContext,
                    events = allAfterImport,
                    strategy = payload.settings.missingDayStrategy,
                    yearsAhead = payload.settings.googleSyncYearsAhead,
                    useDevanagari = payload.settings.useDevanagariEverywhere
                )
            }

            val neAfter = payload.settings.useDevanagariEverywhere
            _statusBanner.value = AppStrings.importSuccessMsg(neAfter, importedCount)
        }
    }

    class Factory(
        private val appContext: Context,
        private val eventRepository: EventRepository,
        private val preferencesRepository: UserPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CalendarViewModel(appContext, eventRepository, preferencesRepository) as T
        }
    }
}
