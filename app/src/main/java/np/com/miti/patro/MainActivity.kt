package np.com.miti.patro

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import np.com.miti.patro.sync.GoogleCalendarSyncHelper
import np.com.miti.patro.ui.calendar.CalendarScreen
import np.com.miti.patro.ui.calendar.CalendarViewModel
import np.com.miti.patro.ui.calendar.EventEditorDialog
import np.com.miti.patro.ui.converter.DateConverterScreen
import np.com.miti.patro.ui.settings.SettingsScreen
import np.com.miti.patro.ui.theme.LocalMitiPalette
import np.com.miti.patro.ui.theme.MitiTheme

private enum class ActiveScreen {
    CALENDAR,
    CONVERTER,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private val vm: CalendarViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val state by vm.uiState.collectAsStateWithLifecycle()
            var activeScreen by remember { mutableStateOf(ActiveScreen.CALENDAR) }
            var quickAddBsDate by remember { mutableStateOf<np.com.miti.patro.domain.model.BsDate?>(null) }
            var hasCalendarPermission by remember {
                mutableStateOf(GoogleCalendarSyncHelper.hasCalendarPermissions(this))
            }

            val notifPermLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { }

            val calendarPermLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { grants ->
                val granted = grants[Manifest.permission.READ_CALENDAR] == true &&
                    grants[Manifest.permission.WRITE_CALENDAR] == true
                hasCalendarPermission = granted
                if (granted) {
                    vm.setSyncToGoogleCalendar(true)
                }
            }

            val exportJsonLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/json")
            ) { uri ->
                if (uri != null) {
                    vm.exportBackupToUri(uri)
                }
            }

            val importJsonLauncher = rememberLauncherForActivityResult(
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
                        notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            MitiTheme(themeMode = state.settings.themeMode) {
                val palette = LocalMitiPalette.current

                BackHandler(enabled = activeScreen != ActiveScreen.CALENDAR) {
                    activeScreen = ActiveScreen.CALENDAR
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(palette.background)
                        .systemBarsPadding()
                ) {
                    AnimatedContent(
                        targetState = activeScreen,
                        transitionSpec = {
                            val forward = targetState.ordinal > initialState.ordinal
                            val inAnim = fadeIn(tween(180)) + slideInHorizontally(tween(180)) {
                                if (forward) it / 6 else -it / 6
                            }
                            val outAnim = fadeOut(tween(150)) + slideOutHorizontally(tween(150)) {
                                if (forward) -it / 6 else it / 6
                            }
                            inAnim togetherWith outAnim
                        },
                        label = "screen_transition"
                    ) { screen ->
                        when (screen) {
                            ActiveScreen.CALENDAR -> {
                                CalendarScreen(
                                    state = state,
                                    onPreviousMonth = vm::previousMonth,
                                    onNextMonth = vm::nextMonth,
                                    onPreviousYear = vm::previousYear,
                                    onNextYear = vm::nextYear,
                                    onJumpToMonthYear = vm::jumpToMonthYear,
                                    onJumpToToday = vm::jumpToToday,
                                    onJumpToDate = vm::jumpToBsDate,
                                    onPagerMonthChanged = vm::onPagerSettledMonth,
                                    getMonthGridForPage = vm::getMonthGridForPage,
                                    getEventsForMonthPage = vm::getEventsForMonthPage,
                                    onSelectDate = vm::selectDate,
                                    onSaveEvent = vm::saveEvent,
                                    onDeleteEvent = vm::deleteEvent,
                                    onSyncEventToGCal = vm::exportSingleEventToGoogleCalendar,
                                    onDismissStatusBanner = vm::dismissStatusBanner,
                                    onOpenConverter = { activeScreen = ActiveScreen.CONVERTER },
                                    onOpenSettings = { activeScreen = ActiveScreen.SETTINGS }
                                )
                            }

                            ActiveScreen.CONVERTER -> {
                                DateConverterScreen(
                                    initialBsDate = state.effectiveAnchorBsDate,
                                    useDevanagari = state.settings.useDevanagariEverywhere,
                                    onBackToCalendar = { activeScreen = ActiveScreen.CALENDAR },
                                    onOpenBsDateInCalendar = { targetBs ->
                                        vm.jumpToBsDate(targetBs)
                                        activeScreen = ActiveScreen.CALENDAR
                                    },
                                    onAddEventOnBsDate = { targetBs ->
                                        quickAddBsDate = targetBs
                                    }
                                )
                            }

                            ActiveScreen.SETTINGS -> {
                                SettingsScreen(
                                    settings = state.settings,
                                    hasCalendarPermission = hasCalendarPermission,
                                    onBackToCalendar = { activeScreen = ActiveScreen.CALENDAR },
                                    onSelectTheme = vm::setThemeMode,
                                    onSelectStrategy = vm::setMissingDayStrategy,
                                    onSelectNepaliScriptMode = vm::setNepaliScriptMode,
                                    onToggleGregorianInGrid = vm::setShowGregorianSubDate,
                                    onToggleTithiInGrid = vm::setShowTithiInGrid,
                                    onTogglePublicFestivals = vm::setShowPublicFestivals,
                                    onUpdateDefaultReminderTime = vm::setDefaultReminderTime,
                                    onUpdateDefaultReminderDaysBefore = vm::setDefaultReminderDaysBefore,
                                    onToggleGoogleCalendarSync = { enable ->
                                        if (enable && !GoogleCalendarSyncHelper.hasCalendarPermissions(this@MainActivity)) {
                                            calendarPermLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.READ_CALENDAR,
                                                    Manifest.permission.WRITE_CALENDAR
                                                )
                                            )
                                        } else {
                                            vm.setSyncToGoogleCalendar(enable)
                                        }
                                    },
                                    onUpdateGoogleSyncYearsAhead = vm::setGoogleSyncYearsAhead,
                                    onSyncAllEventsToGoogleNow = {
                                        if (!GoogleCalendarSyncHelper.hasCalendarPermissions(this@MainActivity)) {
                                            calendarPermLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.READ_CALENDAR,
                                                    Manifest.permission.WRITE_CALENDAR
                                                )
                                            )
                                        } else {
                                            vm.syncAllEventsToGoogleCalendarNow()
                                        }
                                    },
                                    onExportBackupJson = {
                                        val todayIso = state.todayBs.formatIso()
                                        exportJsonLauncher.launch("miti-bs-backup-$todayIso.json")
                                    },
                                    onImportBackupJson = {
                                        importJsonLauncher.launch(arrayOf("application/json", "*/*"))
                                    }
                                )
                            }
                        }
                    }

                    if (quickAddBsDate != null) {
                        EventEditorDialog(
                            anchorBsDate = quickAddBsDate!!,
                            existingEvent = null,
                            defaultStrategy = state.settings.defaultMissingDayStrategy,
                            defaultReminderHour = state.settings.defaultReminderHour,
                            defaultReminderMinute = state.settings.defaultReminderMinute,
                            defaultReminderDaysBefore = state.settings.defaultReminderDaysBefore,
                            useDevanagari = state.settings.useDevanagariEverywhere,
                            onDismiss = { quickAddBsDate = null },
                            onSave = { entity ->
                                vm.saveEvent(entity)
                                quickAddBsDate = null
                            }
                        )
                    }
                }
            }
        }
    }
}
