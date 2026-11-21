package np.com.miti.patro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import np.com.miti.patro.domain.engine.BsCalendarData
import np.com.miti.patro.notifications.AlarmScheduler
import np.com.miti.patro.notifications.NotificationHelper
import np.com.miti.patro.notifications.PersistentTodayNotificationManager
import np.com.miti.patro.sync.GoogleCalendarSyncHelper
import np.com.miti.patro.ui.calendar.CalendarScreen
import np.com.miti.patro.ui.calendar.CalendarViewModel
import np.com.miti.patro.ui.calendar.EventEditorDialog
import np.com.miti.patro.ui.components.AsciiHorizontalRule
import np.com.miti.patro.ui.components.BracketButton
import np.com.miti.patro.ui.components.MitiHaptic
import np.com.miti.patro.ui.components.PlaintextBox
import np.com.miti.patro.ui.converter.DateConverterScreen
import np.com.miti.patro.ui.settings.SettingsScreen
import np.com.miti.patro.ui.theme.AppStrings
import np.com.miti.patro.ui.theme.LocalMitiPalette
import np.com.miti.patro.ui.theme.MitiMonospaceFont
import np.com.miti.patro.ui.theme.MitiTheme

private enum class ActiveScreen {
    CALENDAR,
    CONVERTER,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_OPEN_TODAY = "extra_open_today"
    }

    private val vm: CalendarViewModel by viewModels()
    private val openTodayTrigger = mutableIntStateOf(0)
    private val canExactAlarmsState = mutableStateOf(true)

    override fun onResume() {
        super.onResume()
        canExactAlarmsState.value = AlarmScheduler.canScheduleExactAlarms(this)
        vm.onAppResumed()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_TODAY, false)) {
            openTodayTrigger.intValue += 1
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        canExactAlarmsState.value = AlarmScheduler.canScheduleExactAlarms(this)
        if (intent?.getBooleanExtra(EXTRA_OPEN_TODAY, false) == true) {
            openTodayTrigger.intValue += 1
        }

        setContent {
            val state by vm.uiState.collectAsStateWithLifecycle()
            var activeScreen by remember { mutableStateOf(ActiveScreen.CALENDAR) }
            var quickAddBsDate by remember { mutableStateOf<np.com.miti.patro.domain.model.BsDate?>(null) }
            var hasCalendarPermission by remember {
                mutableStateOf(GoogleCalendarSyncHelper.hasCalendarPermissions(this))
            }
            val canScheduleExactAlarms by canExactAlarmsState
            val openTodayTick by openTodayTrigger

            var pendingEnablePersistentNotif by remember { mutableStateOf(false) }

            val notifPermLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (granted && pendingEnablePersistentNotif) {
                    vm.setPersistentTodayNotification(true)
                } else if (granted) {
                    PersistentTodayNotificationManager.syncAsync(this@MainActivity)
                }
                pendingEnablePersistentNotif = false
            }

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
                    vm.previewBackupFromUri(uri)
                }
            }

            LaunchedEffect(openTodayTick) {
                if (openTodayTick > 0) {
                    activeScreen = ActiveScreen.CALENDAR
                    vm.jumpToToday()
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
                    } else {
                        PersistentTodayNotificationManager.syncAsync(this@MainActivity)
                    }
                } else {
                    PersistentTodayNotificationManager.syncAsync(this@MainActivity)
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
                                    onUndoDeleteEvent = vm::undoDeleteEvent,
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
                                    canScheduleExactAlarms = canScheduleExactAlarms,
                                    onRequestExactAlarms = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            val intent = Intent(
                                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                                Uri.parse("package:$packageName")
                                            )
                                            try {
                                                startActivity(intent)
                                            } catch (_: Exception) {
                                                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                                            }
                                        }
                                    },
                                    onBackToCalendar = { activeScreen = ActiveScreen.CALENDAR },
                                    onSelectTheme = vm::setThemeMode,
                                    onSelectStrategy = vm::setMissingDayStrategy,
                                    onSelectNepaliScriptMode = vm::setNepaliScriptMode,
                                    onToggleGregorianInGrid = vm::setShowGregorianSubDate,
                                    onToggleAdDatesInEnglish = vm::setAdDatesInEnglish,
                                    onToggleTithiInGrid = vm::setShowTithiInGrid,
                                    onTogglePublicFestivals = vm::setShowPublicFestivals,
                                    onTogglePersistentNotification = { enable ->
                                        if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                            !NotificationHelper.hasNotificationPermission(this@MainActivity)
                                        ) {
                                            pendingEnablePersistentNotif = true
                                            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            vm.setPersistentTodayNotification(enable)
                                        }
                                    },
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

                    val pendingImport = state.pendingBackupImport
                    if (pendingImport != null) {
                        val ne = state.settings.useDevanagariEverywhere
                        val countStr = BsCalendarData.formatNumber(pendingImport.events.size, ne)
                        val existingStr = BsCalendarData.formatNumber(state.allEvents.size, ne)
                        Dialog(onDismissRequest = vm::dismissBackupImportPreview) {
                            PlaintextBox(
                                header = AppStrings.backupImportHeader(ne),
                                headerColor = palette.orange
                            ) {
                                Text(
                                    text = if (ne) {
                                        "ब्याकअप फाइलमा $countStr कार्यक्रम भेटियो (हाल $existingStr वटा सेभ छन्)।"
                                    } else {
                                        "Found $countStr event(s) in backup ($existingStr currently saved)."
                                    },
                                    color = palette.foreground,
                                    fontFamily = MitiMonospaceFont,
                                    fontSize = 12.5.sp,
                                    lineHeight = 18.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                AsciiHorizontalRule()
                                Spacer(modifier = Modifier.height(6.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    BracketButton(
                                        label = AppStrings.mergeBackupBtn(ne),
                                        highlighted = true,
                                        textColor = palette.green,
                                        haptic = MitiHaptic.CONFIRM,
                                        onClick = { vm.confirmBackupImport(merge = true) }
                                    )
                                    BracketButton(
                                        label = AppStrings.replaceBackupBtn(ne),
                                        textColor = palette.orange,
                                        haptic = MitiHaptic.CONFIRM,
                                        onClick = { vm.confirmBackupImport(merge = false) }
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        BracketButton(
                                            label = AppStrings.cancelBtn(ne),
                                            textColor = palette.red,
                                            onClick = vm::dismissBackupImportPreview
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
