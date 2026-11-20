package np.com.miti.patro.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import np.com.miti.patro.data.preferences.NepaliScriptMode
import np.com.miti.patro.data.preferences.ThemeMode
import np.com.miti.patro.data.preferences.UserSettings
import np.com.miti.patro.domain.engine.BsCalendarData
import np.com.miti.patro.domain.model.MissingDayStrategy
import np.com.miti.patro.ui.components.BracketButton
import np.com.miti.patro.ui.components.PlaintextBox
import np.com.miti.patro.ui.components.PlaintextToggleRow
import np.com.miti.patro.ui.theme.AppStrings
import np.com.miti.patro.ui.theme.LocalMitiPalette

@Composable
fun SettingsScreen(
    settings: UserSettings,
    hasCalendarPermission: Boolean,
    onBackToCalendar: () -> Unit,
    onSelectTheme: (ThemeMode) -> Unit,
    onSelectStrategy: (MissingDayStrategy) -> Unit,
    onSelectNepaliScriptMode: (NepaliScriptMode) -> Unit,
    onToggleGregorianInGrid: (Boolean) -> Unit,
    onToggleTithiInGrid: (Boolean) -> Unit,
    onTogglePublicFestivals: (Boolean) -> Unit,
    onUpdateDefaultReminderTime: (Int, Int) -> Unit,
    onUpdateDefaultReminderDaysBefore: (Int) -> Unit,
    onToggleGoogleCalendarSync: (Boolean) -> Unit,
    onUpdateGoogleSyncYearsAhead: (Int) -> Unit,
    onSyncAllEventsToGoogleNow: () -> Unit,
    onExportBackupJson: () -> Unit,
    onImportBackupJson: () -> Unit
) {
    val palette = LocalMitiPalette.current
    val ne = settings.useDevanagariEverywhere
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BracketButton(
                label = AppStrings.backToCalendar(ne),
                onClick = onBackToCalendar,
                textColor = palette.cyan
            )
            Text(
                text = AppStrings.settingsTopHeader(ne),
                color = palette.foreground,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        PlaintextBox(header = AppStrings.themeSectionHeader(ne)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ThemeMode.entries.forEach { mode ->
                    BracketButton(
                        label = mode.label,
                        highlighted = (settings.themeMode == mode),
                        onClick = { onSelectTheme(mode) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        PlaintextBox(header = AppStrings.scriptSectionHeader(ne)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                NepaliScriptMode.entries.forEach { mode ->
                    BracketButton(
                        label = if (ne) mode.labelNe else mode.labelEn,
                        highlighted = (settings.nepaliScriptMode == mode),
                        onClick = { onSelectNepaliScriptMode(mode) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        PlaintextBox(header = AppStrings.gridDisplayHeader(ne)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                PlaintextToggleRow(
                    label = AppStrings.showAdToggle(ne),
                    checked = settings.showGregorianSubDate,
                    onToggle = { onToggleGregorianInGrid(!settings.showGregorianSubDate) }
                )
                PlaintextToggleRow(
                    label = AppStrings.showTithiToggle(ne),
                    checked = settings.showTithiInGrid,
                    onToggle = { onToggleTithiInGrid(!settings.showTithiInGrid) }
                )
                PlaintextToggleRow(
                    label = AppStrings.showFestivalsToggle(ne),
                    checked = settings.showPublicFestivals,
                    onToggle = { onTogglePublicFestivals(!settings.showPublicFestivals) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        PlaintextBox(header = AppStrings.missingDayHeader(ne)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                MissingDayStrategy.entries.forEach { strat ->
                    BracketButton(
                        label = strat.displayLabel,
                        highlighted = (settings.missingDayStrategy == strat),
                        onClick = { onSelectStrategy(strat) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        PlaintextBox(header = AppStrings.defaultReminderHeader(ne)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = BsCalendarData.formatTime24h(settings.defaultReminderHour, settings.defaultReminderMinute, ne),
                    color = palette.cyan,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    BracketButton("-1h", onClick = {
                        onUpdateDefaultReminderTime((settings.defaultReminderHour + 23) % 24, settings.defaultReminderMinute)
                    })
                    BracketButton("+1h", onClick = {
                        onUpdateDefaultReminderTime((settings.defaultReminderHour + 1) % 24, settings.defaultReminderMinute)
                    })
                    BracketButton("+15m", onClick = {
                        onUpdateDefaultReminderTime(settings.defaultReminderHour, (settings.defaultReminderMinute + 15) % 60)
                    })
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(0, 1, 3, 7).forEach { d ->
                    val label = if (d == 0) AppStrings.todayTag(ne) else "-${BsCalendarData.formatNumber(d, ne)}d"
                    BracketButton(
                        label = label,
                        highlighted = (settings.defaultReminderDaysBefore == d),
                        onClick = { onUpdateDefaultReminderDaysBefore(d) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        PlaintextBox(header = AppStrings.gcalSyncHeader(ne)) {
            PlaintextToggleRow(
                label = AppStrings.autoSyncToggle(ne),
                checked = settings.syncToGoogleCalendar,
                onToggle = { onToggleGoogleCalendarSync(!settings.syncToGoogleCalendar) }
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${AppStrings.yearsAheadLabel(ne)} ${BsCalendarData.formatNumber(settings.googleSyncYearsAhead, ne)}",
                    color = palette.foreground,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(5, 15, 30).forEach { yrs ->
                        BracketButton(
                            label = "${BsCalendarData.formatNumber(yrs, ne)}y",
                            highlighted = (settings.googleSyncYearsAhead == yrs),
                            onClick = { onUpdateGoogleSyncYearsAhead(yrs) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            BracketButton(
                label = AppStrings.syncAllNowBtn(ne),
                onClick = onSyncAllEventsToGoogleNow,
                textColor = palette.cyan
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        PlaintextBox(header = AppStrings.backupSectionHeader(ne)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BracketButton(
                    label = AppStrings.exportBackupBtn(ne),
                    onClick = onExportBackupJson,
                    textColor = palette.green
                )
                BracketButton(
                    label = AppStrings.importBackupBtn(ne),
                    onClick = onImportBackupJson,
                    textColor = palette.orange
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
