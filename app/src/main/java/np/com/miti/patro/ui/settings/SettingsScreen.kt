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
import np.com.miti.patro.domain.model.MissingDayStrategy
import np.com.miti.patro.ui.components.BracketButton
import np.com.miti.patro.ui.components.PlaintextBox
import np.com.miti.patro.ui.theme.LocalMitiPalette

/**
 * Settings screen — the ONLY screen in the application where theme controls,
 * theme names (Alucard / Dracula), and global calendar preferences appear.
 */
@Composable
fun SettingsScreen(
    settings: UserSettings,
    onBack: () -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onSelectMissingDayStrategy: (MissingDayStrategy) -> Unit,
    onToggleDevanagari: (Boolean) -> Unit,
    onToggleGregorianSubDate: (Boolean) -> Unit,
    onToggleTithiInGrid: (Boolean) -> Unit,
    onChangeDefaultReminderTime: (Int, Int) -> Unit
) {
    val palette = LocalMitiPalette.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        // Top navigation bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BracketButton(
                text = "< Calendar",
                onClick = onBack,
                color = palette.cyan,
                isHighlighted = true
            )
            Text(
                text = "SETTINGS",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = palette.foreground
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 1. Theme Palette Selection (System: Alucard Light / Dracula Dark)
        PlaintextBox(
            borderColor = palette.purple,
            headerText = "THEME PALETTE (ALUCARD / DRACULA)",
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
                        text = if (selected) "(*) ${mode.label}" else "( ) ${mode.label}",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp,
                        color = if (selected) palette.purple else palette.foreground
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Active surface: ${if (palette.isDark) "Dracula (#282A36)" else "Alucard (#FFFBEB)"}",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = palette.comment
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Variable BS Month Length Handling (Missing Day Strategy)
        PlaintextBox(
            borderColor = palette.cyan,
            headerText = "VARIABLE BS MONTH LENGTH RULE (DAY 30–32)",
            headerColor = palette.cyan
        ) {
            Text(
                text = "BS months vary between 29 and 32 days depending on the year. Choose how repeating events on day 30, 31, or 32 behave when an upcoming year's month is shorter:",
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
                        text = if (selected) "(*) ${strategy.labelEn}" else "( ) ${strategy.labelEn}",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp,
                        color = if (selected) palette.cyan else palette.foreground
                    )
                    Text(
                        text = "    ${strategy.descriptionEn}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = palette.comment
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Display & Script Preferences
        PlaintextBox(
            borderColor = palette.green,
            headerText = "CALENDAR DISPLAY & SCRIPT",
            headerColor = palette.green
        ) {
            SettingToggleRow(
                label = "Devanagari Script & Numerals (नेपाली)",
                checked = settings.useDevanagari,
                onToggle = { onToggleDevanagari(!settings.useDevanagari) }
            )
            SettingToggleRow(
                label = "Show Tithi in Every Day Cell",
                checked = settings.showTithiInGrid,
                onToggle = { onToggleTithiInGrid(!settings.showTithiInGrid) }
            )
            SettingToggleRow(
                label = "Show Gregorian (AD) Sub-Date",
                checked = settings.showGregorianSubDate,
                onToggle = { onToggleGregorianSubDate(!settings.showGregorianSubDate) }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Default Offline Reminder Time
        PlaintextBox(
            borderColor = palette.pink,
            headerText = "DEFAULT OFFLINE REMINDER TIME",
            headerColor = palette.pink
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "New Event Alarm Time:",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = palette.foreground
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BracketButton(
                        text = "[-H]",
                        onClick = {
                            onChangeDefaultReminderTime(
                                (settings.defaultReminderHour + 23) % 24,
                                settings.defaultReminderMinute
                            )
                        },
                        color = palette.cyan
                    )
                    Text(
                        text = "%02d:%02d".format(settings.defaultReminderHour, settings.defaultReminderMinute),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = palette.pink
                    )
                    BracketButton(
                        text = "[+H]",
                        onClick = {
                            onChangeDefaultReminderTime(
                                (settings.defaultReminderHour + 1) % 24,
                                settings.defaultReminderMinute
                            )
                        },
                        color = palette.cyan
                    )
                    BracketButton(
                        text = "[+15m]",
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
    }
}

@Composable
private fun SettingToggleRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    val palette = LocalMitiPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = palette.foreground,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = if (checked) "[X] ON" else "[ ] OFF",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = if (checked) palette.green else palette.comment
        )
    }
}
