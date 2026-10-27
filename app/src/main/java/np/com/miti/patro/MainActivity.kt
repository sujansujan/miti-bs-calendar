package np.com.miti.patro

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import np.com.miti.patro.data.local.AppDatabase
import np.com.miti.patro.data.local.EventEntity
import np.com.miti.patro.data.preferences.UserPreferencesRepository
import np.com.miti.patro.data.repository.EventRepository
import np.com.miti.patro.domain.model.BsDate
import np.com.miti.patro.reminder.NotificationHelper
import np.com.miti.patro.sync.GoogleCalendarSyncHelper
import np.com.miti.patro.ui.calendar.CalendarScreen
import np.com.miti.patro.ui.calendar.CalendarViewModel
import np.com.miti.patro.ui.calendar.EventEditorDialog
import np.com.miti.patro.ui.converter.DateConverterScreen
import np.com.miti.patro.ui.settings.SettingsScreen
import np.com.miti.patro.ui.theme.AppStrings
import np.com.miti.patro.ui.theme.LocalMitiPalette
import np.com.miti.patro.ui.theme.MitiTheme
import java.time.LocalDate

private enum class ActiveScreen {
    CALENDAR,
    CONVERTER,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NotificationHelper.createNotificationChannel(applicationContext)

        val database = AppDatabase.getInstance(applicationContext)
        val eventRepository = EventRepository(database.eventDao())
        val preferencesRepository = UserPreferencesRepository(applicationContext)

        setContent {
            val vm: CalendarViewModel = viewModel(
                factory = CalendarViewModel.Factory(
                    appContext = applicationContext,
                    eventRepository = eventRepository,
                    preferencesRepository = preferencesRepository
                )
            )
            val state by vm.uiState.collectAsState()
            var activeScreen by remember { mutableStateOf(ActiveScreen.CALENDAR) }
            var converterEventAnchorDate by remember { mutableStateOf<BsDate?>(null) }
            var pendingEventForGCalSync by remember { mutableStateOf<EventEntity?>(null) }

            // Request POST_NOTIFICATIONS on Android 13+
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = {}
            )

            // Request READ_CALENDAR + WRITE_CALENDAR for Google Calendar BS->AD multi-year sync
            val calendarPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val granted = permissions[Manifest.permission.READ_CALENDAR] == true &&
                    permissions[Manifest.permission.WRITE_CALENDAR] == true
                val singleEvent = pendingEventForGCalSync
                pendingEventForGCalSync = null
                if (granted) {
                    if (singleEvent != null) {
                        vm.syncSingleEventToGoogleCalendarNow(singleEvent)
                    } else {
                        vm.syncAllEventsToGoogleCalendarNow()
                    }
                } else {
                    vm.setStatusBanner(AppStrings.gcalPermDeniedMsg(state.settings.useDevanagariEverywhere))
                }
            }

            // Export Settings & Events (.json) via Storage Access Framework (no storage permissions needed)
            val exportBackupLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/json")
            ) { uri ->
                if (uri != null) {
                    vm.exportBackupToUri(uri)
                }
            }

            // Import Settings & Events (.json) via Storage Access Framework
            val importBackupLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) {
                    vm.importBackupFromUri(uri)
                }
            }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val granted = ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!granted) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            MitiTheme(themeMode = state.settings.themeMode) {
                val palette = LocalMitiPalette.current
                // Outer Surface paints the full window (including status bar & gesture bar area)
                // in the theme background color, while inner Box.safeDrawingPadding() insets all
                // content safely below the status bar / camera cutout and above the navigation bar.
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = palette.background
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .displayCutoutPadding()
                            .safeDrawingPadding()
                    ) {
                        when (activeScreen) {
                            ActiveScreen.CALENDAR -> {
                                CalendarScreen(
                                    state = state,
                                    onPreviousMonth = vm::previousMonth,
                                    onNextMonth = vm::nextMonth,
                                    onPreviousYear = vm::previousYear,
                                    onNextYear = vm::nextYear,
                                    onJumpToMonthYear = vm::jumpToMonthYear,
                                    onJumpToToday = vm::jumpToToday,
                                    onSelectDate = vm::selectDate,
                                    onSaveEvent = vm::saveEvent,
                                    onDeleteEvent = vm::deleteEvent,
                                    onSyncEventToGCal = { event ->
                                        if (GoogleCalendarSyncHelper.hasCalendarPermissions(this@MainActivity)) {
                                            vm.syncSingleEventToGoogleCalendarNow(event)
                                        } else {
                                            pendingEventForGCalSync = event
                                            calendarPermissionLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.READ_CALENDAR,
                                                    Manifest.permission.WRITE_CALENDAR
                                                )
                                            )
                                        }
                                    },
                                    onDismissStatusBanner = vm::clearStatusBanner,
                                    onOpenConverter = { activeScreen = ActiveScreen.CONVERTER },
                                    onOpenSettings = { activeScreen = ActiveScreen.SETTINGS }
                                )
                            }

                            ActiveScreen.CONVERTER -> {
                                DateConverterScreen(
<<<<<<< HEAD
                                    initialBsDate = state.selectedBsDate,
=======
                                    initialBsDate = state.effectiveAnchorBsDate,
>>>>>>> 2f0105a (Initial commit: Miti BS Calendar)
                                    useDevanagari = state.settings.useDevanagariEverywhere,
                                    onBackToCalendar = { activeScreen = ActiveScreen.CALENDAR },
                                    onOpenBsDateInCalendar = { targetBsDate ->
                                        vm.selectDate(targetBsDate)
                                        activeScreen = ActiveScreen.CALENDAR
                                    },
                                    onAddEventOnBsDate = { targetBsDate ->
                                        vm.selectDate(targetBsDate)
                                        converterEventAnchorDate = targetBsDate
                                    }
                                )

                                val anchor = converterEventAnchorDate
                                if (anchor != null) {
                                    EventEditorDialog(
                                        anchorBsDate = anchor,
                                        existingEvent = null,
                                        defaultStrategy = state.settings.defaultMissingDayStrategy,
                                        defaultReminderHour = state.settings.defaultReminderHour,
                                        defaultReminderMinute = state.settings.defaultReminderMinute,
                                        defaultReminderDaysBefore = state.settings.defaultReminderDaysBefore,
                                        useDevanagari = state.settings.useDevanagariEverywhere,
                                        onDismiss = { converterEventAnchorDate = null },
                                        onSave = { entity ->
                                            vm.saveEvent(entity)
                                            converterEventAnchorDate = null
                                            activeScreen = ActiveScreen.CALENDAR
                                        }
                                    )
                                }
                            }

                            ActiveScreen.SETTINGS -> {
                                SettingsScreen(
                                    settings = state.settings,
                                    statusBannerMessage = state.statusBannerMessage,
                                    onDismissBanner = vm::clearStatusBanner,
                                    onBack = { activeScreen = ActiveScreen.CALENDAR },
                                    onThemeModeChanged = vm::updateThemeMode,
                                    onMissingDayStrategyChanged = vm::updateMissingDayStrategy,
                                    onShowTithiChanged = vm::updateShowTithiInGrid,
                                    onShowGregorianChanged = vm::updateShowGregorianInGrid,
                                    onShowPublicFestivalsChanged = vm::updateShowPublicFestivals,
                                    onNepaliScriptModeChanged = vm::updateNepaliScriptMode,
                                    onDefaultReminderTimeChanged = vm::updateDefaultReminderTime,
                                    onDefaultReminderDaysBeforeChanged = vm::updateDefaultReminderDaysBefore,
                                    onSyncToGoogleCalendarChanged = { enable ->
                                        if (enable && !GoogleCalendarSyncHelper.hasCalendarPermissions(this@MainActivity)) {
                                            pendingEventForGCalSync = null
                                            calendarPermissionLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.READ_CALENDAR,
                                                    Manifest.permission.WRITE_CALENDAR
                                                )
                                            )
                                        }
                                        vm.updateSyncToGoogleCalendar(enable)
                                    },
                                    onGoogleSyncYearsAheadChanged = vm::updateGoogleSyncYearsAhead,
                                    onSyncAllToGoogleCalendarNow = {
                                        if (GoogleCalendarSyncHelper.hasCalendarPermissions(this@MainActivity)) {
                                            vm.syncAllEventsToGoogleCalendarNow()
                                        } else {
                                            pendingEventForGCalSync = null
                                            calendarPermissionLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.READ_CALENDAR,
                                                    Manifest.permission.WRITE_CALENDAR
                                                )
                                            )
                                        }
                                    },
                                    onExportBackupClicked = {
                                        val dateTag = LocalDate.now().toString()
                                        exportBackupLauncher.launch("miti-backup-$dateTag.json")
                                    },
                                    onImportBackupClicked = {
                                        importBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
