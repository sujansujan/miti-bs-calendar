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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import np.com.miti.patro.data.local.EventEntity
import np.com.miti.patro.domain.engine.BsCalendarData
import np.com.miti.patro.domain.engine.BsDateConverter
import np.com.miti.patro.domain.engine.TithiCalculator
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

    val initialTithiNumber = remember(anchorBsDate, existingEvent) {
        if (existingEvent != null && existingEvent.isShraddha && existingEvent.shraddhaTithi in 1..30) {
            existingEvent.shraddhaTithi
        } else {
            TithiCalculator.calculateTithi(anchorBsDate).number
        }
    }

    var title by remember { mutableStateOf(existingEvent?.title ?: "") }
    var notes by remember { mutableStateOf(existingEvent?.notes ?: "") }
    var isShraddha by remember { mutableStateOf(existingEvent?.isShraddha ?: false) }
    var bsMonth by remember { mutableIntStateOf(existingEvent?.bsMonth ?: anchorBsDate.month) }
    var bsDay by remember { mutableIntStateOf(existingEvent?.bsDay ?: anchorBsDate.day) }
    var shraddhaTithi by remember { mutableIntStateOf(initialTithiNumber) }
    var selectedColor by remember {
        mutableStateOf(
            existingEvent?.resolvedColor
                ?: if (existingEvent?.isPersonalHoliday == true) EventColor.RED else EventColor.GREEN
        )
    }
    var isPersonalHoliday by remember { mutableStateOf(existingEvent?.isPersonalHoliday ?: false) }
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
                fontSize = 15.sp
            )
            AsciiHorizontalRule()
            Spacer(modifier = Modifier.height(6.dp))

            // Mode Toggle: [*EVENT (GATEY)*]  [SHRADDHA (TITHI)]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BracketButton(
                    label = AppStrings.modeGateyBtn(ne),
                    highlighted = !isShraddha,
                    onClick = { isShraddha = false },
                    textColor = if (!isShraddha) palette.cyan else palette.comment
                )
                BracketButton(
                    label = AppStrings.modeShraddhaBtn(ne),
                    highlighted = isShraddha,
                    onClick = {
                        isShraddha = true
                        // Sync initial Tithi to the currently chosen BS month & day
                        val currentCellBs = BsDate(anchorBsDate.year, bsMonth, bsDay.coerceAtMost(BsCalendarData.daysInMonth(anchorBsDate.year, bsMonth)))
                        shraddhaTithi = TithiCalculator.calculateTithi(currentCellBs).number
                        if (selectedColor == EventColor.GREEN) {
                            selectedColor = EventColor.YELLOW
                        }
                    },
                    textColor = if (isShraddha) palette.yellow else palette.comment
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // BS Month + (Gatey OR Lunar Tithi) Row
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
                        fontSize = 14.sp
                    )
                    BracketButton(label = "+", onClick = {
                        bsMonth = if (bsMonth < 12) bsMonth + 1 else 1
                    })
                }

                if (!isShraddha) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BracketButton(label = "-", onClick = {
                            bsDay = if (bsDay > 1) bsDay - 1 else 32
                        })
                        Text(
                            text = BsCalendarData.formatNumberPadded(bsDay, ne),
                            color = palette.cyan,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        BracketButton(label = "+", onClick = {
                            bsDay = if (bsDay < 32) bsDay + 1 else 1
                        })
                    }
                } else {
                    val tithiInfo = remember(shraddhaTithi) {
                        TithiCalculator.buildTithiInfo(shraddhaTithi)
                    }
                    val pakshaLabel = if (ne) tithiInfo.paksha.nepaliName.take(5) else tithiInfo.paksha.shortCode
                    val tithiName = if (ne) tithiInfo.nepaliName else tithiInfo.englishName
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BracketButton(label = "-", onClick = {
                            shraddhaTithi = if (shraddhaTithi > 1) shraddhaTithi - 1 else 30
                        })
                        Text(
                            text = "${tithiInfo.phaseSymbol} $pakshaLabel-$tithiName",
                            color = palette.yellow,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        BracketButton(label = "+", onClick = {
                            shraddhaTithi = if (shraddhaTithi < 30) shraddhaTithi + 1 else 1
                        })
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            PlaintextTextInput(
                label = AppStrings.eventTitleLabel(ne),
                value = title,
                onValueChange = { title = it.take(100) },
                placeholder = AppStrings.eventTitlePlaceholder(ne, isShraddha),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(6.dp))

            PlaintextTextInput(
                label = AppStrings.eventNotesLabel(ne),
                value = notes,
                onValueChange = { notes = it.take(500) },
                placeholder = AppStrings.eventNotesPlaceholder(ne),
                singleLine = false
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 8-color Swatch Picker (4x2)
            Text(
                text = AppStrings.eventColorLabel(ne),
                color = palette.comment,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
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
                            text = "● $labelText",
                            color = swatchColor,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .weight(1f)
                                .background(if (isSelected) palette.selection else palette.surface)
                                .border(1.dp, if (isSelected) swatchColor else palette.comment)
                                .clickable { selectedColor = colorOption }
                                .padding(vertical = 5.dp, horizontal = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

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

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BracketButton(label = "-", onClick = {
                            reminderHour = (reminderHour + 23) % 24
                        })
                        val hh = BsCalendarData.formatNumberPadded(reminderHour, ne)
                        val mm = BsCalendarData.formatNumberPadded(reminderMinute, ne)
                        Text(
                            text = "$hh:$mm",
                            color = palette.cyan,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.clickable {
                                reminderMinute = (reminderMinute + 15) % 60
                            }
                        )
                        BracketButton(label = "+", onClick = {
                            reminderHour = (reminderHour + 1) % 24
                        })
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            AsciiHorizontalRule()
            Spacer(modifier = Modifier.height(6.dp))

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
                            val resolvedAnchorDay = if (isShraddha) {
                                BsDateConverter.resolveShraddhaOccurrence(
                                    bsMonth = bsMonth,
                                    shraddhaTithi = shraddhaTithi,
                                    targetBsYear = anchorBsDate.year
                                )?.targetDate?.day ?: bsDay
                            } else {
                                bsDay
                            }
                            onSave(
                                EventEntity(
                                    id = existingEvent?.id ?: 0L,
                                    title = trimmedTitle,
                                    note = notes.trim(),
                                    bsMonth = bsMonth,
                                    bsDay = resolvedAnchorDay,
                                    startBsYear = existingEvent?.startBsYear ?: anchorBsDate.year,
                                    isPersonalHoliday = isPersonalHoliday,
                                    colorKey = selectedColor.key,
                                    isShraddha = isShraddha,
                                    shraddhaTithi = if (isShraddha) shraddhaTithi else 0,
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
