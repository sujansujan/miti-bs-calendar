package np.com.miti.patro.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import np.com.miti.patro.data.local.EventEntity
import np.com.miti.patro.domain.engine.BsCalendarData
import np.com.miti.patro.domain.model.BsDate
import np.com.miti.patro.domain.model.MissingDayStrategy
import np.com.miti.patro.ui.components.AsciiHorizontalRule
import np.com.miti.patro.ui.components.BracketButton
import np.com.miti.patro.ui.components.PlaintextBox
import np.com.miti.patro.ui.components.PlaintextTextInput
import np.com.miti.patro.ui.components.PlaintextToggleRow
import np.com.miti.patro.ui.theme.AppStrings
import np.com.miti.patro.ui.theme.LocalMitiPalette

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EventEditorDialog(
    initialBsDate: BsDate,
    existingEvent: EventEntity?,
    defaultReminderHour: Int,
    defaultReminderMinute: Int,
    missingDayStrategy: MissingDayStrategy,
    useDevanagari: Boolean,
    onDismiss: () -> Unit,
    onSave: (EventEntity) -> Unit
) {
    val palette = LocalMitiPalette.current
    val ne = useDevanagari

    var title by remember { mutableStateOf(existingEvent?.title ?: "") }
    var note by remember { mutableStateOf(existingEvent?.note ?: "") }
    var bsMonth by remember { mutableIntStateOf(existingEvent?.bsMonth ?: initialBsDate.month) }
    var bsDay by remember { mutableIntStateOf(existingEvent?.bsDay ?: initialBsDate.day) }
    var startBsYear by remember { mutableIntStateOf(existingEvent?.startBsYear ?: initialBsDate.year) }
    var isPersonalHoliday by remember { mutableStateOf(existingEvent?.isPersonalHoliday ?: false) }
    var reminderEnabled by remember { mutableStateOf(existingEvent?.reminderEnabled ?: true) }
    var reminderDaysBefore by remember { mutableIntStateOf(existingEvent?.reminderDaysBefore ?: 0) }
    var reminderHour by remember { mutableIntStateOf(existingEvent?.reminderHour ?: defaultReminderHour) }
    var reminderMinute by remember { mutableIntStateOf(existingEvent?.reminderMinute ?: defaultReminderMinute) }
    var showError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        PlaintextBox(
            borderColor = palette.purple,
            backgroundColor = palette.background,
            headerText = if (existingEvent == null) {
                AppStrings.addEventDialogHeader(ne)
            } else {
                AppStrings.editEventDialogHeader(ne)
            },
            headerColor = palette.purple,
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            // 1. Plaintext `> ` Title input with `-------` rule
            Text(
                text = AppStrings.eventTitleLabel(ne),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = palette.foreground
            )
            Spacer(modifier = Modifier.height(4.dp))
            PlaintextTextInput(
                value = title,
                onValueChange = {
                    title = it
                    showError = false
                },
                placeholder = AppStrings.eventTitlePlaceholder(ne),
                isError = showError,
                singleLine = true
            )
            if (showError) {
                Text(
                    text = AppStrings.eventTitleError(ne),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = palette.red
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            AsciiHorizontalRule()
            Spacer(modifier = Modifier.height(6.dp))

            // 2. BS Month & Day selector (repeats every BS year)
            Text(
                text = AppStrings.repeatsEveryBsYearOn(ne),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = palette.cyan
            )
            Spacer(modifier = Modifier.height(4.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                for (m in 1..12) {
                    val mLabel = BsCalendarData.getShortMonthName(m, ne)
                    val selected = (m == bsMonth)
                    BracketButton(
                        text = if (selected) "*$mLabel*" else "[$mLabel]",
                        onClick = { bsMonth = m },
                        color = if (selected) palette.purple else palette.foreground,
                        isHighlighted = selected
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Day stepper (1..32) + Start BS Year stepper
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = AppStrings.bsDayLabel(ne),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = palette.foreground
                    )
                    BracketButton(
                        text = "[-]",
                        onClick = { bsDay = if (bsDay > 1) bsDay - 1 else 32 },
                        color = palette.cyan
                    )
                    Text(
                        text = BsCalendarData.formatNumber(bsDay, ne, padWidth = 2),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = palette.yellow
                    )
                    BracketButton(
                        text = "[+]",
                        onClick = { bsDay = if (bsDay < 32) bsDay + 1 else 1 },
                        color = palette.cyan
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = AppStrings.fromYearLabel(ne),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = palette.comment
                    )
                    BracketButton(
                        text = "[-]",
                        onClick = {
                            startBsYear = (startBsYear - 1).coerceAtLeast(BsCalendarData.MIN_YEAR)
                        },
                        color = palette.comment
                    )
                    Text(
                        text = BsCalendarData.formatNumber(startBsYear, ne),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = palette.foreground
                    )
                    BracketButton(
                        text = "[+]",
                        onClick = {
                            startBsYear = (startBsYear + 1).coerceAtMost(BsCalendarData.MAX_YEAR)
                        },
                        color = palette.comment
                    )
                }
            }

            if (bsDay >= 30) {
                Spacer(modifier = Modifier.height(4.dp))
                val mName = BsCalendarData.getMonthName(bsMonth, ne)
                val dStr = BsCalendarData.formatNumber(bsDay, ne)
                val ruleLabel = missingDayStrategy.label(ne)
                Text(
                    text = AppStrings.shortMonthNote(ne, mName, dStr, ruleLabel),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = palette.orange
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            AsciiHorizontalRule()
            Spacer(modifier = Modifier.height(6.dp))

            // 3. Plaintext `> ` Note input with `-------` rule
            Text(
                text = AppStrings.notesLabel(ne),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = palette.foreground
            )
            Spacer(modifier = Modifier.height(4.dp))
            PlaintextTextInput(
                value = note,
                onValueChange = { note = it },
                placeholder = AppStrings.notesPlaceholder(ne),
                singleLine = false
            )

            Spacer(modifier = Modifier.height(8.dp))
            AsciiHorizontalRule()
            Spacer(modifier = Modifier.height(4.dp))

            // 4. Mark as personal holiday toggle (`[x]` / `[ ]`)
            PlaintextToggleRow(
                label = AppStrings.markAsHolidayToggle(ne),
                checked = isPersonalHoliday,
                onToggle = { isPersonalHoliday = it }
            )

            // 5. Offline Notification / Reminder toggle (`[x]` / `[ ]`)
            PlaintextToggleRow(
                label = AppStrings.offlineNotificationToggle(ne),
                checked = reminderEnabled,
                onToggle = { reminderEnabled = it }
            )

            if (reminderEnabled) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AppStrings.notifyWhenLabel(ne),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = palette.comment
                    )
                    listOf(0, 1, 3).forEach { days ->
                        val label = AppStrings.notifyOffsetOption(ne, days)
                        val selected = (reminderDaysBefore == days)
                        BracketButton(
                            text = if (selected) "*$label*" else "[$label]",
                            onClick = { reminderDaysBefore = days },
                            color = if (selected) palette.pink else palette.foreground,
                            isHighlighted = selected
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AppStrings.time24hLabel(ne),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = palette.comment
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BracketButton(
                            text = AppStrings.minusHourBtn(ne),
                            onClick = { reminderHour = (reminderHour + 23) % 24 },
                            color = palette.cyan
                        )
                        Text(
                            text = BsCalendarData.formatTime24h(reminderHour, reminderMinute, ne),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = palette.pink
                        )
                        BracketButton(
                            text = AppStrings.plusHourBtn(ne),
                            onClick = { reminderHour = (reminderHour + 1) % 24 },
                            color = palette.cyan
                        )
                        BracketButton(
                            text = AppStrings.plus15MinBtn(ne),
                            onClick = { reminderMinute = (reminderMinute + 15) % 60 },
                            color = palette.cyan
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            AsciiHorizontalRule()
            Spacer(modifier = Modifier.height(8.dp))

            // 6. Save / Cancel buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BracketButton(
                    text = AppStrings.cancelBtn(ne),
                    onClick = onDismiss,
                    color = palette.comment
                )
                BracketButton(
                    text = AppStrings.saveEventBtn(ne),
                    onClick = {
                        val trimmed = title.trim()
                        if (trimmed.isEmpty()) {
                            showError = true
                        } else {
                            onSave(
                                EventEntity(
                                    id = existingEvent?.id ?: 0L,
                                    title = trimmed,
                                    note = note.trim(),
                                    bsMonth = bsMonth,
                                    bsDay = bsDay,
                                    startBsYear = startBsYear,
                                    isPersonalHoliday = isPersonalHoliday,
                                    reminderEnabled = reminderEnabled,
                                    reminderDaysBefore = reminderDaysBefore,
                                    reminderHour = reminderHour,
                                    reminderMinute = reminderMinute
                                )
                            )
                        }
                    },
                    color = palette.green,
                    isHighlighted = true
                )
            }
        }
    }
}
