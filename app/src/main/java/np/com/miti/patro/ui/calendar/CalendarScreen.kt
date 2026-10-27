package np.com.miti.patro.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import np.com.miti.patro.data.local.EventEntity
import np.com.miti.patro.data.repository.ResolvedEventInstance
import np.com.miti.patro.domain.engine.BsCalendarData
import np.com.miti.patro.domain.model.BsCalendarDay
import np.com.miti.patro.domain.model.BsDate
import np.com.miti.patro.ui.components.BracketButton
import np.com.miti.patro.ui.components.PlaintextBox
import np.com.miti.patro.ui.theme.LocalMitiPalette

/**
 * Main Monthly BS Calendar screen inspired by https://plaintextsports.com/
 *
 * Strictly minimal: no app title, no theme badge, no decorative chrome.
 * Everything shown is functional BS calendar, tithi, festival, or repeating event data.
 */
@Composable
fun CalendarScreen(
    state: CalendarMonthUiState,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onPrevYear: () -> Unit,
    onNextYear: () -> Unit,
    onJumpToYearMonth: (Int, Int) -> Unit,
    onJumpToToday: () -> Unit,
    onSelectDay: (BsDate) -> Unit,
    onSaveEvent: (EventEntity) -> Unit,
    onDeleteEvent: (EventEntity) -> Unit,
    onOpenSettings: () -> Unit,
    onRequestNotificationPermissionIfNeeded: () -> Unit
) {
    val palette = LocalMitiPalette.current
    val useDevanagari = state.settings.useDevanagari
    val scrollState = rememberScrollState()

    var showJumpDialog by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<EventEntity?>(null) }
    var showEventEditor by remember { mutableStateOf(false) }

    val prevMonthIndex = if (state.visibleMonth == 1) 12 else state.visibleMonth - 1
    val nextMonthIndex = if (state.visibleMonth == 12) 1 else state.visibleMonth + 1
    val prevMonthShort = BsCalendarData.getShortMonthName(prevMonthIndex, useDevanagari)
    val nextMonthShort = BsCalendarData.getShortMonthName(nextMonthIndex, useDevanagari)
    val currentMonthFull = BsCalendarData.getMonthName(state.visibleMonth, useDevanagari)
    val currentYearStr = BsCalendarData.formatNumber(state.visibleYear, useDevanagari)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        // 1. Plaintext Sports style Month/Year navigation bar:
        //    < Ash   2083 Kartik   Man >
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, palette.comment)
                .background(palette.surface)
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BracketButton(
                text = "< $prevMonthShort",
                onClick = onPrevMonth,
                color = palette.cyan
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { showJumpDialog = true }
                    .padding(horizontal = 6.dp)
            ) {
                Text(
                    text = "$currentYearStr $currentMonthFull",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = palette.foreground
                )
                if (state.settings.showGregorianSubDate) {
                    Text(
                        text = state.adMonthSpanLabel,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = palette.comment
                    )
                }
            }

            BracketButton(
                text = "$nextMonthShort >",
                onClick = onNextMonth,
                color = palette.cyan
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 2. Compact plaintext action bar: <<Yr | [Today] | [+Event] | [Cfg] | Yr>>
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BracketButton(
                    text = "<<Yr",
                    onClick = onPrevYear,
                    color = palette.comment
                )
                BracketButton(
                    text = "Yr>>",
                    onClick = onNextYear,
                    color = palette.comment
                )
                if (state.selectedDate != state.todayBsDate ||
                    state.visibleYear != state.todayBsDate.year ||
                    state.visibleMonth != state.todayBsDate.month
                ) {
                    BracketButton(
                        text = if (useDevanagari) "[आज]" else "[Today]",
                        onClick = onJumpToToday,
                        color = palette.yellow
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                BracketButton(
                    text = if (useDevanagari) "[+पर्व/कार्य]" else "[+Event]",
                    onClick = {
                        editingEvent = null
                        showEventEditor = true
                    },
                    color = palette.green,
                    isHighlighted = true
                )
                BracketButton(
                    text = if (useDevanagari) "[सेटिङ]" else "[Cfg]",
                    onClick = onOpenSettings,
                    color = palette.purple
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 3. Monthly BS Calendar Tabular Grid (7 columns, Sunday..Saturday)
        MonthlyBsGrid(
            state = state,
            onSelectDay = onSelectDay
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Selected Day Box Score Card (Tithi, AD Date, Festivals, Repeating Events)
        state.selectedDayModel?.let { selectedDay ->
            val dayEvents = state.eventsByDay[selectedDay.bsDate.day].orEmpty()
            SelectedDayDetailBox(
                day = selectedDay,
                dayEvents = dayEvents,
                useDevanagari = useDevanagari,
                onAddEventForDay = {
                    editingEvent = null
                    showEventEditor = true
                },
                onEditEvent = { instance ->
                    editingEvent = instance.entity
                    showEventEditor = true
                },
                onDeleteEvent = { instance ->
                    onDeleteEvent(instance.entity)
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 5. Month Summary: Repeating Events & Major Festivals in this BS Month
        MonthEventsAndFestivalsBox(
            state = state,
            onSelectDay = onSelectDay,
            onEditEvent = { entity ->
                editingEvent = entity
                showEventEditor = true
            }
        )
    }

    if (showJumpDialog) {
        YearMonthJumpDialog(
            initialYear = state.visibleYear,
            initialMonth = state.visibleMonth,
            useDevanagari = useDevanagari,
            onDismiss = { showJumpDialog = false },
            onConfirm = { y, m ->
                showJumpDialog = false
                onJumpToYearMonth(y, m)
            }
        )
    }

    if (showEventEditor) {
        EventEditorDialog(
            initialBsDate = state.selectedDate,
            existingEvent = editingEvent,
            defaultReminderHour = state.settings.defaultReminderHour,
            defaultReminderMinute = state.settings.defaultReminderMinute,
            missingDayStrategy = state.settings.missingDayStrategy,
            useDevanagari = useDevanagari,
            onDismiss = { showEventEditor = false },
            onSave = { entity ->
                showEventEditor = false
                if (entity.reminderEnabled) {
                    onRequestNotificationPermissionIfNeeded()
                }
                onSaveEvent(entity)
            }
        )
    }
}

@Composable
private fun MonthlyBsGrid(
    state: CalendarMonthUiState,
    onSelectDay: (BsDate) -> Unit
) {
    val palette = LocalMitiPalette.current
    val useDevanagari = state.settings.useDevanagari
    val weekdays = if (useDevanagari) BsCalendarData.WEEKDAY_SHORT_NE else BsCalendarData.WEEKDAY_SHORT_EN

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, palette.comment)
            .background(palette.surface)
    ) {
        // Weekday header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.selection.copy(alpha = 0.5f))
        ) {
            weekdays.forEachIndexed { idx, label ->
                val isSaturdayCol = idx == 6
                Text(
                    text = label,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = if (isSaturdayCol) palette.red else palette.foreground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .border(0.5.dp, palette.comment.copy(alpha = 0.6f))
                        .padding(vertical = 5.dp)
                )
            }
        }

        val totalCells = state.leadingEmptyCells + state.daysInMonth
        val rows = (totalCells + 6) / 7

        for (rowIdx in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (colIdx in 0 until 7) {
                    val cellIndex = rowIdx * 7 + colIdx
                    val dayIndex = cellIndex - state.leadingEmptyCells
                    if (dayIndex in 0 until state.days.size) {
                        val dayModel = state.days[dayIndex]
                        val dayEvents = state.eventsByDay[dayModel.bsDate.day].orEmpty()
                        val isSelected = dayModel.bsDate == state.selectedDate
                        BsDayCell(
                            day = dayModel,
                            events = dayEvents,
                            isSelected = isSelected,
                            useDevanagari = useDevanagari,
                            showTithi = state.settings.showTithiInGrid,
                            showAdSubDate = state.settings.showGregorianSubDate,
                            onClick = { onSelectDay(dayModel.bsDate) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(58.dp)
                                .background(palette.background.copy(alpha = 0.45f))
                                .border(0.5.dp, palette.comment.copy(alpha = 0.35f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BsDayCell(
    day: BsCalendarDay,
    events: List<ResolvedEventInstance>,
    isSelected: Boolean,
    useDevanagari: Boolean,
    showTithi: Boolean,
    showAdSubDate: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalMitiPalette.current
    val hasPersonalHoliday = events.any { it.entity.isPersonalHoliday }
    val isHoliday = day.isPublicHoliday || hasPersonalHoliday

    val cellBg = when {
        isSelected -> palette.selection
        day.isToday -> palette.yellow.copy(alpha = 0.16f)
        else -> palette.background
    }

    val cellBorderColor = when {
        isSelected -> palette.purple
        day.isToday -> palette.yellow
        else -> palette.comment.copy(alpha = 0.45f)
    }

    val borderWidth = if (isSelected || day.isToday) 1.5.dp else 0.5.dp

    val dayNumColor = when {
        isHoliday -> palette.red
        day.isToday -> palette.yellow
        else -> palette.foreground
    }

    val bsDayText = BsCalendarData.formatNumber(day.bsDate.day, useDevanagari)
    val tithiLabel = if (useDevanagari) day.tithi.shortLabelNe else day.tithi.shortLabelEn

    val tithiColor = when {
        day.tithi.isPurnima -> palette.orange
        day.tithi.isAunsi -> palette.purple
        else -> palette.cyan
    }

    Column(
        modifier = modifier
            .height(58.dp)
            .background(cellBg)
            .border(borderWidth, cellBorderColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 3.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top row: BS day number + optional AD day
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = bsDayText,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = dayNumColor
            )
            if (showAdSubDate) {
                Text(
                    text = day.adDate.dayOfMonth.toString(),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = palette.comment
                )
            }
        }

        // Middle row: Event (*) and Festival (!) markers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val markerBuilder = StringBuilder()
            if (events.isNotEmpty()) {
                markerBuilder.append("*".repeat(events.size.coerceAtMost(3)))
            }
            if (day.festivals.isNotEmpty()) {
                markerBuilder.append("!")
            }
            if (markerBuilder.isNotEmpty()) {
                Text(
                    text = markerBuilder.toString(),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = if (events.isNotEmpty()) palette.green else palette.orange
                )
            }
        }

        // Bottom row: Tithi short label + phase icon for Aunsi/Purnima
        if (showTithi) {
            val prefix = when {
                day.tithi.isPurnima -> "○"
                day.tithi.isAunsi -> "●"
                else -> ""
            }
            Text(
                text = "$prefix$tithiLabel",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = tithiColor,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SelectedDayDetailBox(
    day: BsCalendarDay,
    dayEvents: List<ResolvedEventInstance>,
    useDevanagari: Boolean,
    onAddEventForDay: () -> Unit,
    onEditEvent: (ResolvedEventInstance) -> Unit,
    onDeleteEvent: (ResolvedEventInstance) -> Unit
) {
    val palette = LocalMitiPalette.current
    val monthName = BsCalendarData.getMonthName(day.bsDate.month, useDevanagari)
    val dayStr = BsCalendarData.formatNumber(day.bsDate.day, useDevanagari, padWidth = 2)
    val yearStr = BsCalendarData.formatNumber(day.bsDate.year, useDevanagari)
    val weekdayIdx = day.dayOfWeek.value % 7
    val weekdayName = if (useDevanagari) {
        BsCalendarData.WEEKDAY_FULL_NE[weekdayIdx]
    } else {
        BsCalendarData.WEEKDAY_FULL_EN[weekdayIdx]
    }

    val pakshaText = if (useDevanagari) day.tithi.paksha.nepaliName else day.tithi.paksha.englishName
    val tithiText = if (useDevanagari) day.tithi.nepaliName else day.tithi.englishName

    PlaintextBox(
        borderColor = palette.purple,
        headerText = "$yearStr $monthName $dayStr  •  $weekdayName",
        headerColor = if (day.isPublicHoliday) palette.red else palette.foreground
    ) {
        // Row 1: Tithi + AD date
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${day.tithi.phaseSymbol} $pakshaText • $tithiText",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (day.tithi.isPurnima || day.tithi.isAunsi) palette.orange else palette.cyan
            )
            Text(
                text = "AD ${day.adDate}",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = palette.comment
            )
        }

        if (day.isSaturday) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (useDevanagari) "[शनिबार सार्वजनिक बिदा]" else "[Saturday Public Holiday]",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = palette.red
            )
        }

        // Festivals on this day
        if (day.festivals.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            day.festivals.forEach { fest ->
                val festLabel = if (useDevanagari) fest.nameNe else fest.nameEn
                val tag = if (fest.isPublicHoliday) {
                    if (useDevanagari) "[बिदा] " else "[HOLIDAY] "
                } else {
                    "[FEST] "
                }
                Text(
                    text = "$tag$festLabel",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (fest.isPublicHoliday) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 12.sp,
                    color = if (fest.isPublicHoliday) palette.red else palette.orange
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Repeating user events on this day
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (useDevanagari) "दोहोरिने कार्यहरू (BS):" else "Repeating BS Events:",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = palette.green
            )
            BracketButton(
                text = if (useDevanagari) "[+थप्नुहोस्]" else "[+Add]",
                onClick = onAddEventForDay,
                color = palette.green
            )
        }

        if (dayEvents.isEmpty()) {
            Text(
                text = if (useDevanagari) "यस मितिमा कुनै व्यक्तिगत कार्य छैन।" else "No repeating events on this BS date.",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = palette.comment
            )
        } else {
            dayEvents.forEach { instance ->
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, palette.comment.copy(alpha = 0.6f))
                        .background(palette.background)
                        .padding(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val clampTag = if (instance.isClampedOrRolled) {
                            " [~BS day ${instance.entity.bsDay}]"
                        } else {
                            ""
                        }
                        val holidayTag = if (instance.entity.isPersonalHoliday) " [HOLIDAY]" else ""
                        Text(
                            text = "* ${instance.entity.title}$clampTag$holidayTag",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (instance.entity.isPersonalHoliday) palette.red else palette.green,
                            modifier = Modifier.weight(1f)
                        )
                        Row {
                            BracketButton(
                                text = "[Edit]",
                                onClick = { onEditEvent(instance) },
                                color = palette.cyan
                            )
                            BracketButton(
                                text = "[Del]",
                                onClick = { onDeleteEvent(instance) },
                                color = palette.red
                            )
                        }
                    }

                    if (instance.entity.note.isNotBlank()) {
                        Text(
                            text = instance.entity.note,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = palette.foreground
                        )
                    }

                    val remText = if (instance.entity.reminderEnabled) {
                        val timeStr = "%02d:%02d".format(instance.entity.reminderHour, instance.entity.reminderMinute)
                        val offsetStr = if (instance.entity.reminderDaysBefore == 0) {
                            "same day"
                        } else {
                            "${instance.entity.reminderDaysBefore}d before"
                        }
                        "Alarm: $timeStr ($offsetStr) • Since ${instance.entity.startBsYear} BS"
                    } else {
                        "Alarm: off • Since ${instance.entity.startBsYear} BS"
                    }
                    Text(
                        text = remText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = palette.comment
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthEventsAndFestivalsBox(
    state: CalendarMonthUiState,
    onSelectDay: (BsDate) -> Unit,
    onEditEvent: (EventEntity) -> Unit
) {
    val palette = LocalMitiPalette.current
    val useDevanagari = state.settings.useDevanagari
    val monthName = BsCalendarData.getMonthName(state.visibleMonth, useDevanagari)

    val monthRepeatingEvents = state.eventsByDay.entries
        .sortedBy { it.key }
        .flatMap { (_, list) -> list }

    val monthFestivals = state.days.filter { it.festivals.isNotEmpty() }

    PlaintextBox(
        borderColor = palette.comment,
        headerText = if (useDevanagari) "$monthName महिनाका कार्य र पर्वहरू" else "$monthName Schedule (BS Events & Festivals)",
        headerColor = palette.foreground
    ) {
        if (monthRepeatingEvents.isEmpty() && monthFestivals.isEmpty()) {
            Text(
                text = "No events or festivals recorded for this month.",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = palette.comment
            )
        }

        if (monthRepeatingEvents.isNotEmpty()) {
            Text(
                text = if (useDevanagari) "व्यक्तिगत दोहोरिने कार्यहरू:" else "Repeating BS Events:",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = palette.green
            )
            Spacer(modifier = Modifier.height(2.dp))
            monthRepeatingEvents.forEach { item ->
                val dNum = BsCalendarData.formatNumber(item.effectiveBsDate.day, useDevanagari, padWidth = 2)
                val clampedSuffix = if (item.isClampedOrRolled) " (from day ${item.entity.bsDay})" else ""
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectDay(item.effectiveBsDate) }
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$dNum | * ${item.entity.title}$clampedSuffix",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = if (item.entity.isPersonalHoliday) palette.red else palette.green,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "[Edit]",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = palette.cyan,
                        modifier = Modifier.clickable { onEditEvent(item.entity) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (monthFestivals.isNotEmpty()) {
            Text(
                text = if (useDevanagari) "प्रमुख चाडपर्व र बिदाहरू:" else "Festivals & Holidays:",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = palette.orange
            )
            Spacer(modifier = Modifier.height(2.dp))
            monthFestivals.forEach { dayModel ->
                val dNum = BsCalendarData.formatNumber(dayModel.bsDate.day, useDevanagari, padWidth = 2)
                val tithiName = if (useDevanagari) dayModel.tithi.nepaliName else dayModel.tithi.englishName
                val names = dayModel.festivals.joinToString(", ") {
                    if (useDevanagari) it.nameNe else it.nameEn
                }
                val isHol = dayModel.festivals.any { it.isPublicHoliday }
                Text(
                    text = "$dNum | $names ($tithiName)",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = if (isHol) palette.red else palette.foreground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectDay(dayModel.bsDate) }
                        .padding(vertical = 2.dp)
                )
            }
        }
    }
}
