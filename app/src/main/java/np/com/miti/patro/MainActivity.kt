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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import np.com.miti.patro.ui.calendar.CalendarScreen
import np.com.miti.patro.ui.calendar.CalendarViewModel
import np.com.miti.patro.ui.settings.SettingsScreen
import np.com.miti.patro.ui.theme.MitiTheme

class MainActivity : ComponentActivity() {

    private val viewModel: CalendarViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val state by viewModel.uiState.collectAsState()
            var showSettings by rememberSaveable { mutableStateOf(false) }

            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = { /* User choice respected offline */ }
            )

            MitiTheme(themeMode = state.settings.themeMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                ) {
                    if (showSettings) {
                        BackHandler { showSettings = false }
                        SettingsScreen(
                            settings = state.settings,
                            onBack = { showSettings = false },
                            onSelectThemeMode = viewModel::updateThemeMode,
                            onSelectMissingDayStrategy = viewModel::updateMissingDayStrategy,
                            onToggleDevanagari = viewModel::updateUseDevanagari,
                            onToggleGregorianSubDate = viewModel::updateShowGregorianSubDate,
                            onToggleTithiInGrid = viewModel::updateShowTithiInGrid,
                            onChangeDefaultReminderTime = viewModel::updateDefaultReminderTime
                        )
                    } else {
                        CalendarScreen(
                            state = state,
                            onPrevMonth = viewModel::prevMonth,
                            onNextMonth = viewModel::nextMonth,
                            onPrevYear = viewModel::prevYear,
                            onNextYear = viewModel::nextYear,
                            onJumpToYearMonth = viewModel::jumpToYearMonth,
                            onJumpToToday = viewModel::jumpToToday,
                            onSelectDay = viewModel::selectDay,
                            onSaveEvent = viewModel::saveEvent,
                            onDeleteEvent = viewModel::deleteEvent,
                            onOpenSettings = { showSettings = true },
                            onRequestNotificationPermissionIfNeeded = {
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
                        )
                    }
                }
            }
        }
    }
}
