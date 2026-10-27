package np.com.miti.patro.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import np.com.miti.patro.ui.components.AsciiHorizontalRule
import np.com.miti.patro.ui.components.BracketButton
import np.com.miti.patro.ui.components.PlaintextBox
import np.com.miti.patro.ui.components.PlaintextToggleRow
import np.com.miti.patro.ui.theme.AppStrings
import np.com.miti.patro.ui.theme.LocalMitiPalette

/**
 * Settings screen — the ONLY screen in the app that displays the app name ("Miti")
 * and theme names (Alucard Light / Dracula Dark), per user design specification.
 */
@Composable
fun SettingsScreen(
    settings: UserSettings,
    statusBannerMessage: String?,
    onDismissBanner: () -> Unit,
    onBack: () -> Unit,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onMissingDayStrategyChanged: (MissingDayStrategy) -> Unit,
    onShowTithiChanged: (Boolean) -> Unit,
    onShowGregorianChanged: (Boolean) -> Unit,
    onShowPublicFestivalsChanged: (Boolean) -> Unit,
    onNepaliScriptModeChanged: (NepaliScriptMode) -> Unit,
    onDefaultReminderTimeChanged: (Int, Int) -> Unit,
    onDefaultReminderDaysBeforeChanged: (Int) -> Unit,
    onSyncToGoogleCalendarChanged: (Boolean) -> Unit,
    onGoogleSyncYearsAheadChanged: (Int) -> Unit,
    onSyncAllToGoogleCalendarNow: () -> Unit
) {
    val palette = LocalMitiPalette.current
    // Settings uses Devanagari only when EVERYWHERE mode is selected
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
                onClick = onBack,
                textColor = palette.cyan
            )
            Text(
                text = if (palette.isDark) "THEME: DRACULA (DARK)" else "THEME: ALUCARD (LIGHT)",
                color = palette.purple,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = AppStrings.settingsTopHeader(ne),
            color = palette.foreground,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        AsciiHorizontalRule()

        if (statusBannerMessage != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface)
                    .border(1.dp, palette.cyan)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = statusBannerMessage,
                    color = palette.cyan,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f)
                )
                BracketButton(label = "X", onClick = onDismissBanner, textColor = palette.comment)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 1. Theme Selection
        PlaintextBox(header = AppStrings.themeSectionHeader(ne)) {
            val modes = listOf(
                ThemeMode.SYSTEM to AppStrings.themeSystem(ne),
                ThemeMode.ALUCARD to AppStrings.themeAlucard(ne),
                ThemeMode.DRACULA to AppStrings.themeDracula(ne)
            )
            modes.forEach { (mode, label) ->
                val selected = settings.themeMode == mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onThemeModeChanged(mode) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selected) "(*) " else "( ) ",
                        color = if (selected) palette.cyan else palette.comment,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = label,
                        color = if (selected) palette.foreground else palette.comment,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Missing Day Rule for Short BS Months
        PlaintextBox(header = AppStrings.missingDaySectionHeader(ne)) {
            Text(
                text = AppStrings.missingDaySectionNote(ne),
                color = palette.comment,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            MissingDayStrategy.values().forEach { strategy ->
                val selected = settings.defaultMissingDayStrategy == strategy
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onMissingDayStrategyChanged(strategy) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selected) "(*) " else "( ) ",
                        color = if (selected) palette.cyan else palette.comment,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = strategy.label(ne),
                        color = if (selected) palette.foreground else palette.comment,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Language Scope & Calendar Display Options
        PlaintextBox(header = AppStrings.displaySectionHeader(ne)) {
            Text(
                text = AppStrings.languageScopeLabel(ne),
                color = palette.orange,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(4.dp))

            val scriptOptions = listOf(
                NepaliScriptMode.ENGLISH to AppStrings.langEnglishEverywhere(ne),
                NepaliScriptMode.CALENDAR_TAB_ONLY to AppStrings.langCalendarTabOnly(ne),
                NepaliScriptMode.EVERYWHERE to AppStrings.langNepaliEverywhere(ne)
            )
            scriptOptions.forEach { (mode, label) ->
                val selected = settings.nepaliScriptMode == mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNepaliScriptModeChanged(mode) }
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selected) "(*) " else "( ) ",
                        color = if (selected) palette.cyan else palette.comment,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = label,
                        color = if (selected) palette.foreground else palette.comment,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            AsciiHorizontalRule()
            Spacer(modifier = Modifier.height(4.dp))

            PlaintextToggleRow(
                label = AppStrings.showTithiToggle(ne),
                checked = settings.showTithiInGrid,
                onCheckedChange = onShowTithiChanged
            )
            PlaintextToggleRow(
                label = AppStrings.showAdToggle(ne),
                checked = settings.showGregorianInGrid,
                onCheckedChange = onShowGregorianChanged
            )
            PlaintextToggleRow(
                label = AppStrings.showFestivalsToggle(ne),
                checked = settings.showPublicFestivals,
                onCheckedChange = onShowPublicFestivalsChanged
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Default Notification Reminder Settings
        PlaintextBox(header = AppStrings.reminderSectionHeader(ne)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = AppStrings.defaultHourLabel(ne),
                    color = palette.foreground,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BracketButton(label = "-", onClick = {
                        onDefaultReminderTimeChanged(
                            (settings.defaultReminderHour + 23) % 24,
                            settings.defaultReminderMinute
                        )
                    })
                    val hh = BsCalendarData.formatNumberPadded(settings.defaultReminderHour, ne)
                    val mm = BsCalendarData.formatNumberPadded(settings.defaultReminderMinute, ne)
                    Text(
                        text = "$hh:$mm",
                        color = palette.cyan,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    BracketButton(label = "+", onClick = {
                        onDefaultReminderTimeChanged(
                            (settings.defaultReminderHour + 1) % 24,
                            settings.defaultReminderMinute
                        )
                    })
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = AppStrings.defaultDaysBeforeLabel(ne),
                    color = palette.foreground,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        0 to AppStrings.sameDay(ne),
                        1 to AppStrings.oneDayBefore(ne),
                        2 to AppStrings.twoDaysBefore(ne)
                    ).forEach { (days, label) ->
                        BracketButton(
                            label = label,
                            highlighted = settings.defaultReminderDaysBefore == days,
                            onClick = { onDefaultReminderDaysBeforeChanged(days) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 5. Google Calendar BS->AD Multi-Year Sync
        PlaintextBox(header = AppStrings.gcalSectionHeader(ne)) {
            PlaintextToggleRow(
                label = AppStrings.gcalAutoSyncToggle(ne),
                checked = settings.syncToGoogleCalendar,
                onCheckedChange = onSyncToGoogleCalendarChanged
            )

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = AppStrings.gcalYearsAheadLabel(ne),
                    color = palette.foreground,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(5, 15, 30).forEach { yrs ->
                        val label = BsCalendarData.formatNumber(yrs, ne) + (if (ne) " वर्ष" else "y")
                        BracketButton(
                            label = label,
                            highlighted = settings.googleSyncYearsAhead == yrs,
                            onClick = { onGoogleSyncYearsAheadChanged(yrs) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            BracketButton(
                label = AppStrings.gcalSyncNowBtn(ne),
                onClick = onSyncAllToGoogleCalendarNow,
                textColor = palette.green,
                highlighted = true
            )

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = AppStrings.gcalPermissionNote(ne),
                color = palette.comment,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 6. Data Accuracy & Official Verification Info
        PlaintextBox(header = AppStrings.verificationSectionHeader(ne)) {
            Text(
                text = AppStrings.verificationBody(ne),
                color = palette.comment,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
