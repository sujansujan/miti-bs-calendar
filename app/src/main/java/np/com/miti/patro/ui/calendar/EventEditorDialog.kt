package np.com.miti.patro.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import np.com.miti.patro.data.local.EventEntity
import np.com.miti.patro.domain.engine.BsCalendarData
import np.com.miti.patro.domain.model.BsDate
import np.com.miti.patro.domain.model.EventColor
import np.com.miti.patro.domain.model.MissingDayStrategy
import np.com.miti.patro.ui.components.AsciiHorizontalRule
import np.com.miti.patro.ui.components.BracketButton
import np.com.miti.patro.ui.components.PlaintextTextInput
import np.com.miti.patro.ui.components.PlaintextToggleRow
import np.com.miti.patro.ui.theme.AppStrings
import np.com.miti.patro.ui.theme.LocalMitiPalette

@Composable
fun EventEditorDialog(
    anchorBsDate: BsDate,
    existingEvent: EventEntity? = null,
    defaultStrategy: MissingDayStrategy,
    defaultReminderHour: Int,
    defaultReminderMinute: Int,
    defaultReminderDaysBefore: Int,
    useDevanagari: Boolean,
    onDismiss: () -> Unit,
    onSave: (EventEntity) -> Unit
) {
    val palette = LocalMitiPalette.current
    val ne = useDevanagari

    var title by remember { mutableStateOf(existingEvent?.title ?: "") }
    var notes by remember { mutableStateOf(existingEvent?.notes ?: "") }
    var bsMonth by remember { mutableIntStateOf(existingEvent?.bsMonth ?: anchorBsDate.month) }
    var bsDay by remember { mutableIntStateOf(existingEvent?.bsDay ?: anchorBsDate.day) }
    var selectedColor by remember {
        mutableStateOf(
            existingEvent?.resolvedColor
                ?: if (existingEvent?.isPersonalHoliday == true) EventColor.RED else EventColor.GREEN
        )
    }
    var isPersonalHoliday by remember { mutableStateOf(existingEvent?.isPersonalHoliday ?: false) }
    var missingDayStrategy by remember {
        mutableStateOf(existingEvent?.missingDayStrategy ?: defaultStrategy)
    }
    var reminderEnabled by remember { mutableStateOf(existingEvent?.reminderEnabled ?: true) }
    var reminderHour by remember { mutableIntStateOf(existingEvent?.reminderHour ?: defaultReminderHour) }
    var reminderMinute by remember { mutableIntStateOf(existingEvent?.reminderMinute ?: defaultReminderMinute) }
    var reminderDaysBefore by remember {
        mutableIntStateOf(existingEvent?.reminderDaysBefore ?: defaultReminderDaysBefore)
    }

    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.background)
                .border(1.dp, palette.foreground)
                .padding(12.dp)
                .verticalScroll(scrollState)
        ) {
            Text(
                text = if (existingEvent == null) {
                    AppStrings.newEventHeader(ne)
                } else {
                    AppStrings.editEventHeader(ne)
                },
                color = palette.purple,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            AsciiHorizontalRule()
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = AppStrings.bsAnchorDateLabel(ne),
                color = palette.comment,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BracketButton(label = "-", onClick = {
                        bsMonth = if (bsMonth > 1) bsMonth - 1 else 12
                    })
                    Text(
                        text = BsCalendarData.monthName(bsMonth, ne),
                        color = palette.cyan,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    BracketButton(label = "+", onClick = {
                        bsMonth = if (bsMonth < 12) bsMonth + 1 else 1
                    })
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BracketButton(label = "-", onClick = {
                        bsDay = if (bsDay > 1) bsDay - 1 else 32
                    })
                    Text(
                        text = "${ if (ne) "गते" else "Day" } ${BsCalendarData.formatNumber(bsDay, ne)}",
                        color = palette.cyan,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    BracketButton(label = "+", onClick = {
                        bsDay = if (bsDay < 32) bsDay + 1 else 1
                    })
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            PlaintextTextInput(
                label = AppStrings.eventTitleLabel(ne),
                value = title,
                onValueChange = { title = it },
                placeholder = AppStrings.eventTitlePlaceholder(ne),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            PlaintextTextInput(
                label = AppStrings.eventNotesLabel(ne),
                value = notes,
                onValueChange = { notes = it },
                placeholder = AppStrings.eventNotesPlaceholder(ne),
                singleLine = false
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Event Color Picker (8 colors in a 4x2 plaintext grid)
            Text(
                text = AppStrings.eventColorLabel(ne),
                color = palette.comment,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            val allColors = EventColor.values()
            for (row in 0 until 2) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (col in 0 until 4) {
                        val colorOption = allColors[row * 4 + col]
                        val isSelected = selectedColor == colorOption
                        val swatchColor = colorOption.composeColor(palette)
                        val labelText = colorOption.label(ne)
                        Text(
                            text = if (isSelected) "[*● $labelText*]" else "[● $labelText]",
                            color = swatchColor,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 10.sp,
                            modifier = Modifier
                                .weight(1f)
                                .background(if (isSelected) palette.selection else palette.surface)
                                .border(1.dp, if (isSelected) swatchColor else palette.comment)
                                .clickable { selectedColor = colorOption }
                                .padding(vertical = 5.dp, horizontal = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            PlaintextToggleRow(
                label = AppStrings.markHolidayLabel(ne),
                checked = isPersonalHoliday,
                onCheckedChange = { checked ->
                    isPersonalHoliday = checked
                    if (checked && selectedColor == EventColor.GREEN) {
                        selectedColor = EventColor.RED
                    }
                }
            )

            Spacer(modifier = Modifier.height(6.dp))
            AsciiHorizontalRule()
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = AppStrings.missingDayRuleHeader(ne),
                color = palette.comment,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(4.dp))

            MissingDayStrategy.values().forEach { strategy ->
                val selected = missingDayStrategy == strategy
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { missingDayStrategy = strategy }
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
                        text = strategy.label(ne),
                        color = if (selected) palette.foreground else palette.comment,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            AsciiHorizontalRule()
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = AppStrings.reminderHeader(ne),
                color = palette.comment,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            PlaintextToggleRow(
                label = AppStrings.enableReminderLabel(ne),
                checked = reminderEnabled,
                onCheckedChange = { reminderEnabled = it }
            )

            if (reminderEnabled) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AppStrings.reminderTimingLabel(ne),
                        color = palette.foreground,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(
                            0 to AppStrings.sameDay(ne),
                            1 to AppStrings.oneDayBefore(ne),
                            2 to AppStrings.twoDaysBefore(ne),
                            7 to AppStrings.sevenDaysBefore(ne)
                        ).forEach { (days, label) ->
                            BracketButton(
                                label = label,
                                highlighted = reminderDaysBefore == days,
                                onClick = { reminderDaysBefore = days }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = AppStrings.hourLabel(ne),
                            color = palette.foreground,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                        BracketButton(label = "-", onClick = {
                            reminderHour = (reminderHour + 23) % 24
                        })
                        Text(
                            text = BsCalendarData.formatNumberPadded(reminderHour, ne),
                            color = palette.cyan,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        BracketButton(label = "+", onClick = {
                            reminderHour = (reminderHour + 1) % 24
                        })
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = AppStrings.minuteLabel(ne),
                            color = palette.foreground,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                        BracketButton(label = "-15", onClick = {
                            reminderMinute = (reminderMinute + 45) % 60
                        })
                        Text(
                            text = BsCalendarData.formatNumberPadded(reminderMinute, ne),
                            color = palette.cyan,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        BracketButton(label = "+15", onClick = {
                            reminderMinute = (reminderMinute + 15) % 60
                        })
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            AsciiHorizontalRule()
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                BracketButton(
                    label = AppStrings.cancelBtn(ne),
                    onClick = onDismiss,
                    textColor = palette.comment
                )
                Spacer(modifier = Modifier.padding(horizontal = 6.dp))
                BracketButton(
                    label = AppStrings.saveBtn(ne),
                    onClick = {
                        val trimmedTitle = title.trim()
                        if (trimmedTitle.isNotEmpty()) {
                            onSave(
                                EventEntity(
                                    id = existingEvent?.id ?: 0L,
                                    title = trimmedTitle,
                                    note = notes.trim(),
                                    bsMonth = bsMonth,
                                    bsDay = bsDay,
                                    startBsYear = existingEvent?.startBsYear ?: anchorBsDate.year,
                                    isPersonalHoliday = isPersonalHoliday,
                                    colorKey = selectedColor.key,
                                    reminderEnabled = reminderEnabled,
                                    reminderDaysBefore = reminderDaysBefore,
                                    reminderHour = reminderHour,
                                    reminderMinute = reminderMinute
                                )
                            )
                        }
                    },
                    textColor = palette.green,
                    highlighted = title.isNotBlank()
                )
            }
        }
    }
}
