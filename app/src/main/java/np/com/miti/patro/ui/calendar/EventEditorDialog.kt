package np.com.miti.patro.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import np.com.miti.patro.data.local.EventEntity
import np.com.miti.patro.domain.engine.BsCalendarData
import np.com.miti.patro.domain.model.BsDate
import np.com.miti.patro.domain.model.MissingDayStrategy
import np.com.miti.patro.ui.components.BracketButton
import np.com.miti.patro.ui.components.PlaintextBox
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
                "ADD REPEATING BS EVENT"
            } else {
                "EDIT REPEATING BS EVENT"
            },
            headerColor = palette.purple,
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            // 1. Title input
            Text(
                text = "Title (required):",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = palette.foreground
            )
            Spacer(modifier = Modifier.height(4.dp))
            BasicTextField(
                value = title,
                onValueChange = {
                    title = it
                    showError = false
                },
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = palette.foreground
                ),
                cursorBrush = SolidColor(palette.purple),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface)
                    .border(1.dp, if (showError) palette.red else palette.comment)
                    .padding(8.dp)
            )
            if (showError) {
                Text(
                    text = "Please enter an event title.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = palette.red
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. BS Month & Day selector (repeats every BS year)
            Text(
                text = "Repeats Every BS Year On:",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = palette.cyan
            )
            Spacer(modifier = Modifier.height(4.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (m in 1..12) {
                    val mLabel = BsCalendarData.getShortMonthName(m, useDevanagari)
                    val selected = (m == bsMonth)
                    Text(
                        text = if (selected) "[$mLabel]" else " $mLabel ",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp,
                        color = if (selected) palette.purple else palette.foreground,
                        modifier = Modifier
                            .background(if (selected) palette.selection else palette.surface)
                            .border(0.5.dp, if (selected) palette.purple else palette.comment)
                            .clickable { bsMonth = m }
                            .padding(horizontal = 4.dp, vertical = 3.dp)
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
                        text = "BS Day:",
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
                        text = "%02d".format(bsDay),
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
                        text = "From:",
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
                        text = startBsYear.toString(),
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
                Text(
                    text = "Note: BS month lengths vary (29–32 days). In years where ${BsCalendarData.getMonthName(bsMonth, false)} has fewer than $bsDay days, rule '${missingDayStrategy.labelEn}' applies.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = palette.orange
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Note input
            Text(
                text = "Note (optional):",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = palette.foreground
            )
            Spacer(modifier = Modifier.height(4.dp))
            BasicTextField(
                value = note,
                onValueChange = { note = it },
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = palette.foreground
                ),
                cursorBrush = SolidColor(palette.purple),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface)
                    .border(1.dp, palette.comment)
                    .padding(8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Mark as personal holiday toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isPersonalHoliday = !isPersonalHoliday }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mark as Holiday (Red Date):",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = palette.foreground
                )
                Text(
                    text = if (isPersonalHoliday) "[X] YES" else "[ ] NO",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (isPersonalHoliday) palette.red else palette.comment
                )
            }

            // 5. Offline Notification / Reminder toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { reminderEnabled = !reminderEnabled }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Offline Notification:",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = palette.foreground
                )
                Text(
                    text = if (reminderEnabled) "[X] ON" else "[ ] OFF",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (reminderEnabled) palette.green else palette.comment
                )
            }

            if (reminderEnabled) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Notify:",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = palette.comment
                    )
                    listOf(0 to "Same day", 1 to "1d prior", 3 to "3d prior").forEach { (days, label) ->
                        val selected = (reminderDaysBefore == days)
                        BracketButton(
                            text = if (selected) "[$label]" else label,
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
                        text = "Time (24h):",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = palette.comment
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BracketButton(
                            text = "[-H]",
                            onClick = { reminderHour = (reminderHour + 23) % 24 },
                            color = palette.cyan
                        )
                        Text(
                            text = "%02d:%02d".format(reminderHour, reminderMinute),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = palette.pink
                        )
                        BracketButton(
                            text = "[+H]",
                            onClick = { reminderHour = (reminderHour + 1) % 24 },
                            color = palette.cyan
                        )
                        BracketButton(
                            text = "[+15m]",
                            onClick = { reminderMinute = (reminderMinute + 15) % 60 },
                            color = palette.cyan
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 6. Save / Cancel buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                BracketButton(
                    text = "[Cancel]",
                    onClick = onDismiss,
                    color = palette.comment
                )
                BracketButton(
                    text = "[Save BS Event]",
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
