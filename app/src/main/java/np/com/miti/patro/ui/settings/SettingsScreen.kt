package np.com.miti.patro.ui.settings

import androidx.compose.foundation.background
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
import np.com.miti.patro.data.preferences.ThemeMode
import np.com.miti.patro.data.preferences.UserPreferences
import np.com.miti.patro.domain.engine.BsCalendarData
import np.com.miti.patro.domain.model.MissingDayStrategy
import np.com.miti.patro.ui.components.AsciiHorizontalRule
import np.com.miti.patro.ui.components.PlaintextBox
import np.com.miti.patro.ui.components.PlaintextBracketButton
import np.com.miti.patro.ui.components.PlaintextToggleRow
import np.com.miti.patro.ui.theme.AppStrings
import np.com.miti.patro.ui.theme.LocalMitiPalette

/**
 * Plaintext Settings Screen.
 * - Theme controls (System / Alucard Light / Dracula Dark) appear ONLY here.
 * - Full Nepali language & Devanagari support across every single label when enabled.
 * - Google Calendar Sync controls (auto-sync toggle, years-ahead selector, manual sync trigger).
 */
@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    statusBannerMessage: String?,
    onBack: () -> Unit,
    onSelectTheme: (ThemeMode) -> Unit,
    onToggleNepaliScript: (Boolean) -> Unit,
    onToggleGregorianSubDate: (Boolean) -> Unit,
    onToggleTithiInGrid: (Boolean) -> Unit,
    onSelectMissingDayStrategy: (MissingDayStrategy) -> Unit,
    onChangeDefaultReminderTime: (hour: Int, minute: Int) -> Unit,
    onToggleGoogleCalendarSync: (Boolean) -> Unit,
    onChangeGoogleSyncYearsAhead: (Int) -> Unit,
    onSyncAllToGoogleCalendarNow: () -> Unit,
    onDismissStatusBanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalMitiPalette.current
    val ne = preferences.useNepaliScript
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        // Top Header Bar
        AsciiHorizontalRule()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.surfaceElevated)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlaintextBracketButton(
                label = AppStrings.backBtn(ne),
                onClick = onBack,
                textColor = palette.accentCyan
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = AppStrings.settingsTitle(ne),
                    color = palette.foreground,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = AppStrings.appMetadataSubtitle(ne),
                    color = palette.comment,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }
        }
        AsciiHorizontalRule()

        Spacer(modifier = Modifier.height(10.dp))

        // Status Banner (if Google Calendar sync ran)
        if (statusBannerMessage != null) {
            PlaintextBox(headerTitle = if (ne) "सूचना" else "STATUS") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = statusBannerMessage,
                        color = palette.accentGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f)
                    )
                    PlaintextBracketButton(
                        label = "[X]",
                        onClick = onDismissStatusBanner,
                        textColor = palette.comment
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Section 1: Theme Selection (Alucard for Light, Dracula for Dark)
        PlaintextBox(
            headerTitle = AppStrings.sec1ThemeTitle(ne),
            headerRight = palette.name
        ) {
            Text(
                text = AppStrings.activePaletteDesc(ne),
                color = palette.comment,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            ThemeMode.entries.forEach { mode ->
                val isSelected = preferences.themeMode == mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectTheme(mode) }
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isSelected) "(*) " else "( ) ",
                        color = if (isSelected) palette.accentGreen else palette.comment,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = mode.label(ne),
                        color = if (isSelected) palette.foreground else palette.comment,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Section 2: Calendar Display & Language
        PlaintextBox(headerTitle = AppStrings.sec2DisplayTitle(ne)) {
            PlaintextToggleRow(
                label = AppStrings.useNepaliScriptToggle(ne),
                checked = preferences.useNepaliScript,
                onToggle = onToggleNepaliScript
            )
            AsciiHorizontalRule()
            PlaintextToggleRow(
                label = AppStrings.showGregorianSubDateToggle(ne),
                checked = preferences.showGregorianSubDate,
                onToggle = onToggleGregorianSubDate
            )
            AsciiHorizontalRule()
            PlaintextToggleRow(
                label = AppStrings.showTithiInGridToggle(ne),
                checked = preferences.showTithiInGrid,
                onToggle = onToggleTithiInGrid
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Section 3: Default Short-Month Rule for Repeating BS Events
        PlaintextBox(headerTitle = AppStrings.sec3MissingDayTitle(ne)) {
            Text(
                text = AppStrings.missingDayRuleDesc(ne),
                color = palette.comment,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            MissingDayStrategy.entries.forEach { strategy ->
                val isSelected = preferences.defaultMissingDayStrategy == strategy
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectMissingDayStrategy(strategy) }
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = if (isSelected) "(*) " else "( ) ",
                        color = if (isSelected) palette.accentGreen else palette.comment,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Column {
                        Text(
                            text = strategy.label(ne),
                            color = palette.foreground,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                        Text(
                            text = strategy.description(ne),
                            color = palette.comment,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Section 4: Default Reminder Time
        PlaintextBox(headerTitle = AppStrings.sec4ReminderTitle(ne)) {
            val formattedTime = BsCalendarData.formatTime24h(
                preferences.defaultReminderHour,
                preferences.defaultReminderMinute,
                ne
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = AppStrings.defaultAlarmTimeLabel(ne, formattedTime),
                    color = palette.foreground,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PlaintextBracketButton(
                    label = AppStrings.hourMinusBtn(ne),
                    onClick = {
                        onChangeDefaultReminderTime(
                            (preferences.defaultReminderHour + 23) % 24,
                            preferences.defaultReminderMinute
                        )
                    }
                )
                PlaintextBracketButton(
                    label = AppStrings.hourPlusBtn(ne),
                    onClick = {
                        onChangeDefaultReminderTime(
                            (preferences.defaultReminderHour + 1) % 24,
                            preferences.defaultReminderMinute
                        )
                    }
                )
                PlaintextBracketButton(
                    label = AppStrings.minMinusBtn(ne),
                    onClick = {
                        onChangeDefaultReminderTime(
                            preferences.defaultReminderHour,
                            (preferences.defaultReminderMinute + 45) % 60
                        )
                    }
                )
                PlaintextBracketButton(
                    label = AppStrings.minPlusBtn(ne),
                    onClick = {
                        onChangeDefaultReminderTime(
                            preferences.defaultReminderHour,
                            (preferences.defaultReminderMinute + 15) % 60
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Section 5: Google Calendar Sync
        PlaintextBox(headerTitle = AppStrings.sec5GoogleSyncTitle(ne)) {
            Text(
                text = AppStrings.googleSyncDesc(ne),
                color = palette.comment,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            AsciiHorizontalRule()

            PlaintextToggleRow(
                label = AppStrings.autoSyncToggle(ne),
                checked = preferences.syncToGoogleCalendar,
                onToggle = onToggleGoogleCalendarSync
            )

            AsciiHorizontalRule()
            Spacer(modifier = Modifier.height(4.dp))

            val yearsStr = BsCalendarData.formatNumber(preferences.googleSyncYearsAhead, ne)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = AppStrings.syncHorizonLabel(ne, yearsStr),
                    color = palette.foreground,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                Row {
                    listOf(3, 5, 10).forEach { yr ->
                        val yrLabel = BsCalendarData.formatNumber(yr, ne)
                        val active = preferences.googleSyncYearsAhead == yr
                        PlaintextBracketButton(
                            label = if (active) "*${yrLabel}y*" else "[${yrLabel}y]",
                            onClick = { onChangeGoogleSyncYearsAhead(yr) },
                            textColor = if (active) palette.accentGreen else palette.comment
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            PlaintextBracketButton(
                label = AppStrings.syncNowBtn(ne),
                onClick = onSyncAllToGoogleCalendarNow,
                textColor = palette.accentCyan,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Section 6: Offline Data & Verification Info
        PlaintextBox(headerTitle = AppStrings.sec6DataInfoTitle(ne)) {
            Text(
                text = AppStrings.dataCoverageLine1(ne),
                color = palette.foreground,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
            Text(
                text = AppStrings.dataCoverageLine2(ne),
                color = palette.comment,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
            Text(
                text = AppStrings.dataCoverageLine3(ne),
                color = palette.comment,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
            Text(
                text = AppStrings.dataCoverageLine4(ne),
                color = palette.accentGreen,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
