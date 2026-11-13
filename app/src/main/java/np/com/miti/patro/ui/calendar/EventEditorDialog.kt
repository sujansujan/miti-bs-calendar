package np.com.miti.patro.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import np.com.miti.patro.domain.engine.TithiCalculator
import np.com.miti.patro.domain.model.BsDate
import np.com.miti.patro.domain.model.EventColor
import np.com.miti.patro.domain.model.MissingDayStrategy
import np.com.miti.patro.ui.components.AsciiHorizontalRule
import np.com.miti.patro.ui.components.BracketButton
import np.com.miti.patro.ui.components.PlaintextBox
import np.com.miti.patro.ui.components.PlaintextTextInput
import np.com.miti.patro.ui.theme.AppStrings
import np.com.miti.patro.ui.theme.LocalMitiPalette

@Composable
fun EventEditorDialog(
    anchorBsDate: BsDate,
    existingEvent: EventEntity?,
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

    val anchorTithi = remember(anchorBsDate) { TithiCalculator.calculateTithi(anchorBsDate).number }

    var title by remember { mutableStateOf(existingEvent?.title ?: "") }
    var notes by remember { mutableStateOf(existingEvent?.notes ?: "") }
    var isShraddha by remember { mutableStateOf(existingEvent?.isShraddha ?: false) }
    var shraddhaTithi by remember {
        mutableIntStateOf(
            if (existingEvent != null && existingEvent.shraddhaTithi in 1..30) {
                existingEvent.shraddhaTithi
            } else {
                anchorTithi
            }
        )
    }
    var selectedColor by remember {
        mutableStateOf(existingEvent?.resolvedColor ?: EventColor.GREEN)
    }
    var isPersonalHoliday by remember {
        mutableStateOf(existingEvent?.isPersonalHoliday ?: false)
    }
    var reminderEnabled by remember {
        mutableStateOf(existingEvent?.reminderEnabled ?: true)
    }
    var reminderDaysBefore by remember {
        mutableIntStateOf(existingEvent?.reminderDaysBefore ?: defaultReminderDaysBefore)
    }
    var reminderHour by remember {
        mutableIntStateOf(existingEvent?.reminderHour ?: defaultReminderHour)
    }
    var reminderMinute by remember {
        mutableIntStateOf(existingEvent?.reminderMinute ?: defaultReminderMinute)
    }

    val effectiveBsDate = if (existingEvent != null) {
        BsDate(existingEvent.originBsYear, existingEvent.bsMonth, existingEvent.bsDay)
    } else {
        anchorBsDate
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.background)
                .border(1.dp, palette.cyan)
                .padding(6.dp)
        ) {
            PlaintextBox(
                header = if (existingEvent == null) {
                    AppStrings.newEventHeader(ne)
                } else {
                    AppStrings.editEventHeader(ne)
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AppStrings.eventModeLabel(ne),
                        color = palette.comment,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        BracketButton(
                            label = AppStrings.modeSolarBs(ne),
                            highlighted = !isShraddha,
                            onClick = { isShraddha = false },
                            textColor = if (!isShraddha) palette.cyan else palette.comment
                        )
                        BracketButton(
                            label = AppStrings.modeShraddhaTithi(ne),
                            highlighted = isShraddha,
                            onClick = { isShraddha = true },
                            textColor = if (isShraddha) palette.orange else palette.comment
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (!isShraddha) {
                    Text(
                        text = "${AppStrings.bsDateFieldLabel(ne)} ${effectiveBsDate.formatHuman(ne)}",
                        color = palette.orange,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                } else {
                    val mName = BsCalendarData.monthName(effectiveBsDate.month, ne)
                    val tInfo = TithiCalculator.buildTithiInfo(shraddhaTithi)
                    val tDisplay = if (ne) {
                        "${tInfo.nepaliName} (${tInfo.paksha.nepaliName})"
                    } else {
                        "${tInfo.englishName} (${tInfo.paksha.shortCode})"
                    }
                    Text(
                        text = "${AppStrings.shraddhaTithiFieldLabel(ne)} $mName • $tDisplay",
                        color = palette.orange,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BracketButton(
                            label = "-Tithi",
                            onClick = {
                                shraddhaTithi = if (shraddhaTithi <= 1) 30 else shraddhaTithi - 1
                            }
                        )
                        Text(
                            text = "${tInfo.phaseSymbol} #${BsCalendarData.formatNumber(shraddhaTithi, ne)}",
                            color = palette.cyan,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        BracketButton(
                            label = "+Tithi",
                            onClick = {
                                shraddhaTithi = if (shraddhaTithi >= 30) 1 else shraddhaTithi + 1
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = AppStrings.titleFieldLabel(ne),
                    color = palette.foreground,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                PlaintextTextInput(
                    value = title,
                    onValueChange = { title = it.take(100) },
                    placeholder = AppStrings.titlePlaceholder(ne)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = AppStrings.notesFieldLabel(ne),
                    color = palette.foreground,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                PlaintextTextInput(
                    value = notes,
                    onValueChange = { notes = it.take(500) },
                    placeholder = AppStrings.notesPlaceholder(ne),
                    singleLine = false
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = AppStrings.colorLabel(ne),
                    color = palette.foreground,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EventColor.entries.forEach { swatch ->
                        val c = swatch.composeColor(palette)
                        val isSelectedSwatch = (swatch == selectedColor)
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .background(c)
                                .border(
                                    width = if (isSelectedSwatch) 2.dp else 1.dp,
                                    color = if (isSelectedSwatch) palette.foreground else palette.comment
                                )
                                .clickable {
                                    selectedColor = swatch
                                    if (swatch == EventColor.RED) {
                                        isPersonalHoliday = true
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelectedSwatch) {
                                Text(
                                    text = "*",
                                    color = palette.background,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                AsciiHorizontalRule()
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BracketButton(
                        label = if (isPersonalHoliday) {
                            "[X] ${AppStrings.personalHolidayCheckbox(ne)}"
                        } else {
                            "[ ] ${AppStrings.personalHolidayCheckbox(ne)}"
                        },
                        highlighted = isPersonalHoliday,
                        onClick = {
                            isPersonalHoliday = !isPersonalHoliday
                            if (isPersonalHoliday && selectedColor == EventColor.GREEN) {
                                selectedColor = EventColor.RED
                            }
                        },
                        textColor = if (isPersonalHoliday) palette.red else palette.foreground
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BracketButton(
                        label = if (reminderEnabled) {
                            "[X] ${AppStrings.reminderCheckbox(ne)}"
                        } else {
                            "[ ] ${AppStrings.reminderCheckbox(ne)}"
                        },
                        highlighted = reminderEnabled,
                        onClick = { reminderEnabled = !reminderEnabled },
                        textColor = if (reminderEnabled) palette.green else palette.comment
                    )
                }

                if (reminderEnabled) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(0, 1, 3, 7).forEach { d ->
                            val label = if (d == 0) {
                                AppStrings.todayTag(ne)
                            } else {
                                "-${BsCalendarData.formatNumber(d, ne)}d"
                            }
                            BracketButton(
                                label = label,
                                highlighted = (reminderDaysBefore == d),
                                onClick = { reminderDaysBefore = d }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${AppStrings.timeLabel(ne)} ${BsCalendarData.formatTime24h(reminderHour, reminderMinute, ne)}",
                            color = palette.cyan,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            BracketButton(
                                label = "-1h",
                                onClick = { reminderHour = (reminderHour + 23) % 24 }
                            )
                            BracketButton(
                                label = "+1h",
                                onClick = { reminderHour = (reminderHour + 1) % 24 }
                            )
                            BracketButton(
                                label = "+15m",
                                onClick = { reminderMinute = (reminderMinute + 15) % 60 }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                AsciiHorizontalRule()
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BracketButton(
                        label = AppStrings.cancelBtn(ne),
                        onClick = onDismiss,
                        textColor = palette.red
                    )
                    BracketButton(
                        label = AppStrings.saveBtn(ne),
                        enabled = title.isNotBlank(),
                        highlighted = title.isNotBlank(),
                        onClick = {
                            val cleanTitle = title.trim().take(100)
                            if (cleanTitle.isNotEmpty()) {
                                val entity = EventEntity(
                                    id = existingEvent?.id ?: 0L,
                                    title = cleanTitle,
                                    note = notes.trim().take(500),
                                    bsMonth = effectiveBsDate.month,
                                    bsDay = effectiveBsDate.day,
                                    startBsYear = effectiveBsDate.year,
                                    isPersonalHoliday = isPersonalHoliday,
                                    colorKey = selectedColor.key,
                                    isShraddha = isShraddha,
                                    shraddhaTithi = if (isShraddha) shraddhaTithi else 0,
                                    reminderEnabled = reminderEnabled,
                                    reminderDaysBefore = reminderDaysBefore,
                                    reminderHour = reminderHour,
                                    reminderMinute = reminderMinute
                                )
                                onSave(entity)
                            }
                        },
                        textColor = palette.green
                    )
                }
            }
        }
    }
}
