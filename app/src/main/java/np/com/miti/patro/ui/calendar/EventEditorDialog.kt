package np.com.miti.patro.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import np.com.miti.patro.domain.engine.BsCalendarData
import np.com.miti.patro.domain.model.BsDate
import np.com.miti.patro.domain.model.BsEvent
import np.com.miti.patro.domain.model.MissingDayStrategy
import np.com.miti.patro.ui.components.AsciiHorizontalRule
import np.com.miti.patro.ui.components.PlaintextBox
import np.com.miti.patro.ui.components.PlaintextBracketButton
import np.com.miti.patro.ui.components.PlaintextTextInput
import np.com.miti.patro.ui.components.PlaintextToggleRow
import np.com.miti.patro.ui.theme.AppStrings
import np.com.miti.patro.ui.theme.LocalMitiPalette

@Composable
fun EventEditorDialog(
    selectedBsDate: BsDate,
    existingEvent: BsEvent?,
    defaultMissingDayStrategy: MissingDayStrategy,
    defaultReminderHour: Int,
    defaultReminderMinute: Int,
    nepaliScript: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        existingId: Long?,
        title: String,
        notes: String,
        bsMonth: Int,
        bsDay: Int,
        originBsYear: Int,
        missingDayStrategy: MissingDayStrategy,
        reminderEnabled: Boolean,
        reminderHour: Int,
        reminderMinute: Int
    ) -> Unit
) {
    val palette = LocalMitiPalette.current
    val ne = nepaliScript

    val bsMonth = existingEvent?.bsMonth ?: selectedBsDate.month
    val bsDay = existingEvent?.bsDay ?: selectedBsDate.day
    val originYear = existingEvent?.originBsYear ?: selectedBsDate.year

    var title by remember { mutableStateOf(existingEvent?.title ?: "") }
    var notes by remember { mutableStateOf(existingEvent?.notes ?: "") }
    var strategy by remember {
        mutableStateOf(existingEvent?.missingDayStrategy ?: defaultMissingDayStrategy)
    }
    var reminderEnabled by remember { mutableStateOf(existingEvent?.reminderEnabled ?: true) }
    var reminderHour by remember {
        mutableIntStateOf(existingEvent?.reminderHour ?: defaultReminderHour)
    }
    var reminderMinute by remember {
        mutableIntStateOf(existingEvent?.reminderMinute ?: defaultReminderMinute)
    }

    val monthName = BsCalendarData.getMonthName(bsMonth, ne)
    val dayStr = BsCalendarData.formatNumber(bsDay, ne)
    val yearStr = BsCalendarData.formatNumber(originYear, ne)

    Dialog(onDismissRequest = onDismiss) {
        PlaintextBox(
            headerTitle = if (existingEvent != null) {
                AppStrings.editBsEventHeader(ne)
            } else {
                AppStrings.newBsEventHeader(ne)
            },
            headerRight = "$monthName $dayStr",
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.background)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = AppStrings.anchorDateLabel(ne, monthName, dayStr, yearStr),
                color = palette.accentPurple,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Text(
                text = AppStrings.repeatsEveryYearInfo(ne, monthName, dayStr),
                color = palette.comment,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Plaintext `> ` Title Input with `-------` rule
            Text(
                text = AppStrings.eventTitleLabel(ne),
                color = palette.foreground,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            PlaintextTextInput(
                value = title,
                onValueChange = { title = it },
                placeholder = AppStrings.eventTitlePlaceholder(ne),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Plaintext `> ` Notes Input with `-------` rule
            Text(
                text = AppStrings.notesLabel(ne),
                color = palette.foreground,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            PlaintextTextInput(
                value = notes,
                onValueChange = { notes = it },
                placeholder = AppStrings.notesPlaceholder(ne),
                singleLine = false
            )

            // Missing-day strategy selector (critical for days 29, 30, 31, 32)
            Spacer(modifier = Modifier.height(10.dp))
            AsciiHorizontalRule()
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = AppStrings.shortMonthHandlingTitle(ne, dayStr),
                color = palette.festivalOrange,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            Text(
                text = AppStrings.shortMonthHandlingDesc(ne, dayStr),
                color = palette.comment,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(4.dp))

            MissingDayStrategy.entries.forEach { item ->
                val selected = strategy == item
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { strategy = item }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = if (selected) "(*) " else "( ) ",
                        color = if (selected) palette.accentGreen else palette.comment,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Column {
                        Text(
                            text = item.label(ne),
                            color = palette.foreground,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                        Text(
                            text = item.description(ne),
                            color = palette.comment,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            AsciiHorizontalRule()
            Spacer(modifier = Modifier.height(6.dp))

            // Reminder Notification Configuration
            Text(
                text = AppStrings.reminderNotificationTitle(ne),
                color = palette.accentCyan,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )

            PlaintextToggleRow(
                label = AppStrings.notifyOnEventDayToggle(ne),
                checked = reminderEnabled,
                onToggle = { reminderEnabled = it }
            )

            if (reminderEnabled) {
                val hourFormatted = BsCalendarData.formatNumber(reminderHour, ne).padStart(2, if (ne) '०' else '0')
                val minFormatted = BsCalendarData.formatNumber(reminderMinute, ne).padStart(2, if (ne) '०' else '0')

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AppStrings.hourLabel(ne, hourFormatted),
                        color = palette.foreground,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                    Row {
                        PlaintextBracketButton(
                            label = "[-]",
                            onClick = { reminderHour = (reminderHour + 23) % 24 }
                        )
                        PlaintextBracketButton(
                            label = "[+]",
                            onClick = { reminderHour = (reminderHour + 1) % 24 }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AppStrings.minuteLabel(ne, minFormatted),
                        color = palette.foreground,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                    Row {
                        PlaintextBracketButton(
                            label = if (ne) "[-१५]" else "[-15]",
                            onClick = { reminderMinute = (reminderMinute + 45) % 60 }
                        )
                        PlaintextBracketButton(
                            label = if (ne) "[+१५]" else "[+15]",
                            onClick = { reminderMinute = (reminderMinute + 15) % 60 }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            AsciiHorizontalRule()
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PlaintextBracketButton(
                    label = AppStrings.cancelBtn(ne),
                    onClick = onDismiss,
                    textColor = palette.comment
                )
                PlaintextBracketButton(
                    label = AppStrings.saveBtn(ne),
                    onClick = {
                        if (title.isNotBlank()) {
                            onSave(
                                existingEvent?.id,
                                title,
                                notes,
                                bsMonth,
                                bsDay,
                                originYear,
                                strategy,
                                reminderEnabled,
                                reminderHour,
                                reminderMinute
                            )
                        }
                    },
                    textColor = if (title.isNotBlank()) palette.accentGreen else palette.comment
                )
            }
        }
    }
}
