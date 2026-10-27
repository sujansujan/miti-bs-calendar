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
 * Settings screen — the ONLY screen in the application where theme controls,
 * theme names (Alucard / Dracula), and global calendar preferences appear.
 * Rendered in 100% Nepali when `settings.useDevanagari` is enabled.
 */
@Composable
fun SettingsScreen(
    settings: UserSettings,
    statusBannerMessage: String?,
    onBack: () -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onSelectMissingDayStrategy: (MissingDayStrategy) -> Unit,
    onToggleDevanagari: (Boolean) -> Unit,
    onToggleGregorianSubDate: (Boolean) -> Unit,
    onToggleTithiInGrid: (Boolean) -> Unit,
    onChangeDefaultReminderTime: (Int, Int) -> Unit,
    onToggleGoogleCalendarSync: (Boolean) -> Unit,
    onChangeGoogleSyncYearsAhead: (Int) -> Unit,
    onSyncAllToGoogleCalendarNow: () -> Unit,
    onDismissStatusBanner: () -> Unit
) {
    val palette = LocalMitiPalette.current
    val ne = settings.useDevanagari
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        // Top navigation bar framed with literal `-------` rules
        AsciiHorizontalRule()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.surface)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BracketButton(
                text = AppStrings.backBtn(ne),
                onClick = onBack,
                color = palette.cyan,
                isHighlighted = true
            )
            Text(
                text = AppStrings.settingsHeaderTitle(ne),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = palette.foreground
            )
        }
        AsciiHorizontalRule()

        Spacer(modifier = Modifier.height(10.dp))

        // Status banner for Google Calendar sync feedback
        if (statusBannerMessage != null) {
            PlaintextBox(
                borderColor = palette.green,
                headerText = AppStrings.syncStatusTitle(ne),
                headerColor = palette.green
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = statusBannerMessage,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = palette.green,
                        modifier = Modifier.weight(1f)
                    )
                    BracketButton(
                        text = "[X]",
                        onClick = onDismissStatusBanner,
                        color = palette.comment
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // 1. Theme Palette Selection (System: Alucard Light / Dracula Dark)
        PlaintextBox(
            borderColor = palette.purple,
            headerText = AppStrings.sec1ThemeHeader(ne),
            headerColor = palette.purple
        ) {
            ThemeMode.entries.forEach { mode ->
                val selected = (settings.themeMode == mode)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectThemeMode(mode) }
                        .padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selected) "(*) ${mode.label(ne)}" else "( ) ${mode.label(ne)}",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp,
                        color = if (selected) palette.purple else palette.foreground
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = AppStrings.activeSurfaceLabel(ne, palette.isDark),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = palette.comment
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Variable BS Month Length Handling (Missing Day Strategy)
        PlaintextBox(
            borderColor = palette.cyan,
            headerText = AppStrings.sec2MissingDayHeader(ne),
            headerColor = palette.cyan
        ) {
            Text(
                text = AppStrings.missingDayRuleIntro(ne),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = palette.comment
            )
            Spacer(modifier = Modifier.height(6.dp))

            MissingDayStrategy.entries.forEach { strategy ->
                val selected = (settings.missingDayStrategy == strategy)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectMissingDayStrategy(strategy) }
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = if (selected) "(*) ${strategy.label(ne)}" else "( ) ${strategy.label(ne)}",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp,
                        color = if (selected) palette.cyan else palette.foreground
                    )
                    Text(
                        text = "    ${strategy.description(ne)}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = palette.comment
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Display & Language Preferences
        PlaintextBox(
            borderColor = palette.green,
            headerText = AppStrings.sec3DisplayHeader(ne),
            headerColor = palette.green
        ) {
            PlaintextToggleRow(
                label = AppStrings.useDevanagariToggle(ne),
                checked = settings.useDevanagari,
                onToggle = onToggleDevanagari
            )
            AsciiHorizontalRule()
            PlaintextToggleRow(
                label = AppStrings.showTithiToggle(ne),
                checked = settings.showTithiInGrid,
                onToggle = onToggleTithiInGrid
            )
            AsciiHorizontalRule()
            PlaintextToggleRow(
                label = AppStrings.showAdSubDateToggle(ne),
                checked = settings.showGregorianSubDate,
                onToggle = onToggleGregorianSubDate
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Default Offline Reminder Time
        PlaintextBox(
            borderColor = palette.pink,
            headerText = AppStrings.sec4DefaultReminderHeader(ne),
            headerColor = palette.pink
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = AppStrings.newEventAlarmTimeLabel(ne),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = palette.foreground,
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BracketButton(
                        text = AppStrings.minusHourBtn(ne),
                        onClick = {
                            onChangeDefaultReminderTime(
                                (settings.defaultReminderHour + 23) % 24,
                                settings.defaultReminderMinute
                            )
                        },
                        color = palette.cyan
                    )
                    Text(
                        text = BsCalendarData.formatTime24h(
                            settings.defaultReminderHour,
                            settings.defaultReminderMinute,
                            ne
                        ),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = palette.pink
                    )
                    BracketButton(
                        text = AppStrings.plusHourBtn(ne),
                        onClick = {
                            onChangeDefaultReminderTime(
                                (settings.defaultReminderHour + 1) % 24,
                                settings.defaultReminderMinute
                            )
                        },
                        color = palette.cyan
                    )
                    BracketButton(
                        text = AppStrings.plus15MinBtn(ne),
                        onClick = {
                            onChangeDefaultReminderTime(
                                settings.defaultReminderHour,
                                (settings.defaultReminderMinute + 15) % 60
                            )
                        },
                        color = palette.cyan
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 5. Google Calendar Sync
        PlaintextBox(
            borderColor = palette.cyan,
            headerText = AppStrings.sec5GoogleSyncHeader(ne),
            headerColor = palette.cyan
        ) {
            Text(
                text = AppStrings.googleSyncExplanation(ne),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = palette.comment
            )
            Spacer(modifier = Modifier.height(6.dp))
            AsciiHorizontalRule()

            PlaintextToggleRow(
                label = AppStrings.autoSyncToggle(ne),
                checked = settings.syncToGoogleCalendar,
                onToggle = onToggleGoogleCalendarSync
            )

            AsciiHorizontalRule()
            Spacer(modifier = Modifier.height(4.dp))

            val yearsStr = BsCalendarData.formatNumber(settings.googleSyncYearsAhead, ne)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = AppStrings.syncHorizonLabel(ne, yearsStr),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = palette.foreground,
                    modifier = Modifier.weight(1f)
                )
                Row {
                    listOf(3, 5, 10).forEach { yr ->
                        val yrLabel = BsCalendarData.formatNumber(yr, ne)
                        val active = (settings.googleSyncYearsAhead == yr)
                        BracketButton(
                            text = if (active) "*${yrLabel}y*" else "[${yrLabel}y]",
                            onClick = { onChangeGoogleSyncYearsAhead(yr) },
                            color = if (active) palette.green else palette.comment,
                            isHighlighted = active
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            BracketButton(
                text = AppStrings.syncNowBtn(ne),
                onClick = onSyncAllToGoogleCalendarNow,
                color = palette.cyan,
                isHighlighted = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
