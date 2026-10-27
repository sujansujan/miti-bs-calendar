package np.com.miti.patro

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
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
import np.com.miti.patro.sync.GoogleCalendarSyncHelper
import np.com.miti.patro.ui.calendar.CalendarScreen
import np.com.miti.patro.ui.calendar.CalendarViewModel
import np.com.miti.patro.ui.calendar.EventEditorDialog
import np.com.miti.patro.ui.calendar.YearMonthJumpDialog
import np.com.miti.patro.ui.settings.SettingsScreen
import np.com.miti.patro.ui.theme.LocalMitiPalette
import np.com.miti.patro.ui.theme.MitiTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as MitiApplication

        setContent {
            val viewModel: CalendarViewModel = viewModel(
                factory = CalendarViewModel.Factory(
                    appContext = applicationContext,
                    eventRepository = app.eventRepository,
                    preferencesRepository = app.preferencesRepository,
                    alarmScheduler = app.alarmScheduler
                )
            )
            val uiState by viewModel.uiState.collectAsState()
            var showSettingsScreen by remember { mutableStateOf(false) }

            // Request POST_NOTIFICATIONS permission on Android 13+ when reminders are used
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = {}
            )

            // Request READ_CALENDAR + WRITE_CALENDAR permissions when Google Calendar Sync is enabled or triggered
            var pendingManualSyncAfterPerm by remember { mutableStateOf(false) }
            val calendarPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val granted = permissions[Manifest.permission.READ_CALENDAR] == true &&
                    permissions[Manifest.permission.WRITE_CALENDAR] == true
                if (granted && pendingManualSyncAfterPerm) {
                    viewModel.syncAllEventsToGoogleCalendarNow()
                }
                pendingManualSyncAfterPerm = false
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

            MitiTheme(themeMode = uiState.preferences.themeMode) {
                val palette = LocalMitiPalette.current
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                    color = palette.background
                ) {
                    if (showSettingsScreen) {
                        SettingsScreen(
                            preferences = uiState.preferences,
                            statusBannerMessage = uiState.statusBannerMessage,
                            onBack = { showSettingsScreen = false },
                            onSelectTheme = viewModel::setThemeMode,
                            onToggleNepaliScript = viewModel::setUseNepaliScript,
                            onToggleGregorianSubDate = viewModel::setShowGregorianSubDate,
                            onToggleTithiInGrid = viewModel::setShowTithiInGrid,
                            onSelectMissingDayStrategy = viewModel::setDefaultMissingDayStrategy,
                            onChangeDefaultReminderTime = viewModel::setDefaultReminderTime,
                            onToggleGoogleCalendarSync = { enabled ->
                                if (enabled && !GoogleCalendarSyncHelper.hasCalendarPermissions(this@MainActivity)) {
                                    pendingManualSyncAfterPerm = true
                                    viewModel.setSyncToGoogleCalendar(true)
                                    calendarPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.READ_CALENDAR,
                                            Manifest.permission.WRITE_CALENDAR
                                        )
                                    )
                                } else {
                                    viewModel.setSyncToGoogleCalendar(enabled)
                                }
                            },
                            onChangeGoogleSyncYearsAhead = viewModel::setGoogleSyncYearsAhead,
                            onSyncAllToGoogleCalendarNow = {
                                if (!GoogleCalendarSyncHelper.hasCalendarPermissions(this@MainActivity)) {
                                    pendingManualSyncAfterPerm = true
                                    calendarPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.READ_CALENDAR,
                                            Manifest.permission.WRITE_CALENDAR
                                        )
                                    )
                                } else {
                                    viewModel.syncAllEventsToGoogleCalendarNow()
                                }
                            },
                            onDismissStatusBanner = viewModel::clearStatusBanner
                        )
                    } else {
                        CalendarScreen(
                            uiState = uiState,
                            onSelectDate = viewModel::selectDate,
                            onPreviousMonth = viewModel::previousMonth,
                            onNextMonth = viewModel::nextMonth,
                            onJumpToToday = viewModel::jumpToToday,
                            onOpenYearMonthPicker = viewModel::openYearMonthPicker,
                            onOpenAddEvent = viewModel::openAddEventDialog,
                            onOpenEditEvent = viewModel::openEditEventDialog,
                            onDeleteEvent = viewModel::deleteEvent,
                            onExportEventToGCal = { resolved, adDate ->
                                viewModel.exportOccurrenceToGoogleCalendar(
                                    resolved = resolved,
                                    adDate = adDate,
                                    nepaliScript = uiState.preferences.useNepaliScript
                                )
                            },
                            onOpenSettings = { showSettingsScreen = true }
                        )
                    }

                    if (uiState.isAddEditDialogOpen) {
                        EventEditorDialog(
                            selectedBsDate = uiState.selectedBsDate,
                            existingEvent = uiState.editingEvent,
                            defaultMissingDayStrategy = uiState.preferences.defaultMissingDayStrategy,
                            defaultReminderHour = uiState.preferences.defaultReminderHour,
                            defaultReminderMinute = uiState.preferences.defaultReminderMinute,
                            nepaliScript = uiState.preferences.useNepaliScript,
                            onDismiss = viewModel::closeEventDialog,
                            onSave = viewModel::saveEvent
                        )
                    }

                    if (uiState.isYearMonthPickerOpen) {
                        YearMonthJumpDialog(
                            initialYear = uiState.monthGrid.bsYear,
                            initialMonth = uiState.monthGrid.bsMonth,
                            nepaliScript = uiState.preferences.useNepaliScript,
                            onDismiss = viewModel::closeYearMonthPicker,
                            onConfirm = viewModel::jumpToYearMonth
                        )
                    }
                }
            }
        }
    }
}
