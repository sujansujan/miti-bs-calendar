package np.com.miti.patro.ui.calendar

import androidx.compose.foundation.background
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
import np.com.miti.patro.ui.components.AsciiHorizontalRule
import np.com.miti.patro.ui.components.AsciiTableHorizontalBorder
import np.com.miti.patro.ui.components.BracketButton
import np.com.miti.patro.ui.components.PlaintextBox
import np.com.miti.patro.ui.theme.AppStrings
import np.com.miti.patro.ui.theme.LocalMitiPalette

/**
 * Main Monthly BS Calendar screen inspired by https://plaintextsports.com/
 *
 * - Strict monospace ASCII table with literal `+-------------+` (`-------`) horizontal borders
 *   and `|` vertical column pipes.
 * - Selected date rendered as `[09]`, today as `*09*`.
 * - Zero app name or theme indicator on the calendar screen.
 * - 100% Nepali language & Devanagari numerals across every UI element when enabled.
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
    onExportEventToGCal: (ResolvedEventInstance) -> Unit,
    onOpenSettings: () -> Unit,
    onRequestNotificationPermissionIfNeeded: () -> Unit
) {
    val palette = LocalMitiPalette.current
    val ne = state.settings.useDevanagari
    val scrollState = rememberScrollState()

    var showJumpDialog by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<EventEntity?>(null) }
    var showEventEditor by remember { mutableStateOf(false) }

    val prevMonthIndex = if (state.visibleMonth == 1) 12 else state.visibleMonth - 1
    val nextMonthIndex = if (state.visibleMonth == 12) 1 else state.visibleMonth + 1
    val prevMonthShort = BsCalendarData.getShortMonthName(prevMonthIndex, ne)
    val nextMonthShort = BsCalendarData.getShortMonthName(nextMonthIndex, ne)
    val currentMonthFull = BsCalendarData.getMonthName(state.visibleMonth, ne)
    val currentYearStr = BsCalendarData.formatNumber(state.visibleYear, ne)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        // 1. Plaintext Sports style Month/Year navigation bar framed by literal `-------` rules
        AsciiHorizontalRule()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.surface)
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BracketButton(
                text = "[< $prevMonthShort]",
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
                    text = if (ne) "[$currentMonthFull $currentYearStr v]" else "[${currentMonthFull.uppercase()} $currentYearStr v]",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = palette.foreground
                )
                if (state.settings.showGregorianSubDate) {
                    Text(
                        text = "(${state.adMonthSpanLabel})",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = palette.comment
                    )
                }
            }

            BracketButton(
                text = "[$nextMonthShort >]",
                onClick = onNextMonth,
                color = palette.cyan
            )
        }

        // 2. Compact plaintext action bar: [<<YR] [YR>>] [TODAY] ... [+EVENT] [CFG]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.surface)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BracketButton(
                    text = AppStrings.prevYearBtn(ne),
                    onClick = onPrevYear,
                    color = palette.comment
                )
                BracketButton(
                    text = AppStrings.nextYearBtn(ne),
                    onClick = onNextYear,
                    color = palette.comment
                )
                BracketButton(
                    text = AppStrings.todayBtn(ne),
                    onClick = onJumpToToday,
                    color = palette.green
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                BracketButton(
                    text = AppStrings.addEventTopBtn(ne),
                    onClick = {
                        editingEvent = null
                        showEventEditor = true
                    },
                    color = palette.green,
                    isHighlighted = true
                )
                BracketButton(
                    text = AppStrings.settingsBtn(ne),
                    onClick = onOpenSettings,
                    color = palette.purple
                )
            }
        }

        // 3. Monthly BS Calendar Tabular Grid (7 columns with literal `+-------------+` & `|` borders)
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
                useDevanagari = ne,
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
                },
                onExportEventToGCal = onExportEventToGCal
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

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showJumpDialog) {
        YearMonthJumpDialog(
            initialYear = state.visibleYear,
            initialMonth = state.visibleMonth,
            useDevanagari = ne,
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
            useDevanagari = ne,
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
    val ne = state.settings.useDevanagari
    val weekdays = if (ne) BsCalendarData.WEEKDAY_SHORT_NE else BsCalendarData.WEEKDAY_SHORT_EN

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.surface)
    ) {
        AsciiTableHorizontalBorder(columns = 7)

        // Weekday header row framed by `|` pipes
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.selection.copy(alpha = 0.45f))
                .padding(vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            weekdays.forEachIndexed { idx, label ->
                val isSaturdayCol = idx == 6
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "|",
                        color = palette.comment,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                    Text(
                        text = label,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (isSaturdayCol) palette.red else palette.foreground,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Text(
                text = "|",
                color = palette.comment,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        }

        AsciiTableHorizontalBorder(columns = 7)

        val totalCells = state.leadingEmptyCells + state.daysInMonth
        val rows = (totalCells + 6) / 7

        for (rowIdx in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (colIdx in 0 until 7) {
                    val cellIndex = rowIdx * 7 + colIdx
                    val dayIndex = cellIndex - state.leadingEmptyCells
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "|",
                            color = palette.comment,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                        if (dayIndex in 0 until state.days.size) {
                            val dayModel = state.days[dayIndex]
                            val dayEvents = state.eventsByDay[dayModel.bsDate.day].orEmpty()
                            val isSelected = dayModel.bsDate == state.selectedDate
                            BsDayCell(
                                day = dayModel,
                                events = dayEvents,
                                isSelected = isSelected,
                                useDevanagari = ne,
                                showTithi = state.settings.showTithiInGrid,
                                showAdSubDate = state.settings.showGregorianSubDate,
                                onClick = { onSelectDay(dayModel.bsDate) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(60.dp)
                                    .background(palette.background)
                            )
                        }
                    }
                }
                Text(
                    text = "|",
                    color = palette.comment,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }
            AsciiTableHorizontalBorder(columns = 7)
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
        else -> palette.surface
    }

    val dayNumColor = when {
        isHoliday -> palette.red
        day.isToday -> palette.green
        else -> palette.foreground
    }

    val rawBsDay = BsCalendarData.formatNumber(day.bsDate.day, useDevanagari, padWidth = 2)
    val bsDayText = when {
        isSelected -> "[$rawBsDay]"
        day.isToday -> "*$rawBsDay*"
        else -> rawBsDay
    }
    val tithiLabel = if (useDevanagari) day.tithi.shortLabelNe else day.tithi.shortLabelEn

    val tithiColor = when {
        day.tithi.isPurnima -> palette.orange
        day.tithi.isAunsi -> palette.pink
        else -> palette.purple
    }

    Column(
        modifier = modifier
            .height(60.dp)
            .background(cellBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top row: BS day number (`[09]` when selected, `*09*` when today) + optional AD day
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = bsDayText,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = if (isSelected || day.isToday) 11.sp else 13.sp,
                lineHeight = 13.sp,
                color = dayNumColor,
                maxLines = 1
            )
            if (showAdSubDate) {
                Text(
                    text = BsCalendarData.formatNumber(day.adDate.dayOfMonth, useDevanagari),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    lineHeight = 9.sp,
                    color = palette.comment,
                    maxLines = 1
                )
            }
        }

        // Middle row: Event (+N) and Festival (*) plaintext markers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (day.festivals.isNotEmpty()) {
                Text(
                    text = "*",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    lineHeight = 10.sp,
                    color = if (day.isPublicHoliday) palette.red else palette.orange
                )
            }
            if (events.isNotEmpty()) {
                val countStr = BsCalendarData.formatNumber(events.size, useDevanagari)
                Text(
                    text = "+$countStr",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    lineHeight = 10.sp,
                    color = palette.cyan
                )
            }
        }

        // Bottom row: Tithi short label + phase icon for Aunsi/Purnima
        if (showTithi) {
            val prefix = when {
                day.tithi.isPurnima -> "●"
                day.tithi.isAunsi -> "○"
                else -> ""
            }
            Text(
                text = "$prefix$tithiLabel",
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp,
                lineHeight = 9.sp,
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
    onDeleteEvent: (ResolvedEventInstance) -> Unit,
    onExportEventToGCal: (ResolvedEventInstance) -> Unit
) {
    val palette = LocalMitiPalette.current
    val ne = useDevanagari
    val monthName = BsCalendarData.getMonthName(day.bsDate.month, ne)
    val dayStr = BsCalendarData.formatNumber(day.bsDate.day, ne, padWidth = 2)
    val yearStr = BsCalendarData.formatNumber(day.bsDate.year, ne)
    val weekdayIdx = day.dayOfWeek.value % 7
    val weekdayName = if (ne) {
        BsCalendarData.WEEKDAY_FULL_NE[weekdayIdx]
    } else {
        BsCalendarData.WEEKDAY_FULL_EN[weekdayIdx]
    }

    val pakshaText = if (ne) day.tithi.paksha.nepaliName else day.tithi.paksha.englishName
    val tithiText = if (ne) day.tithi.nepaliName else day.tithi.englishName
    val adFormatted = BsCalendarData.formatAdDate(day.adDate, ne)

    PlaintextBox(
        borderColor = palette.purple,
        headerText = "$yearStr $monthName $dayStr • $weekdayName ($adFormatted AD)",
        headerColor = if (day.isPublicHoliday) palette.red else palette.purple
    ) {
        // Row 1: Tithi + Saturday badge
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
                color = if (day.tithi.isPurnima || day.tithi.isAunsi) palette.orange else palette.cyan,
                modifier = Modifier.weight(1f)
            )
            if (day.isSaturday) {
                Text(
                    text = AppStrings.saturdayHolidayBadge(ne),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = palette.red
                )
            }
        }

        // Festivals on this day
        if (day.festivals.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            day.festivals.forEach { fest ->
                val festLabel = if (ne) fest.nameNe else fest.nameEn
                val tag = if (fest.isPublicHoliday) {
                    AppStrings.publicHolidayTag(ne)
                } else {
                    AppStrings.festivalTag(ne)
                }
                Text(
                    text = "$tag $festLabel",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (fest.isPublicHoliday) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 12.sp,
                    color = if (fest.isPublicHoliday) palette.red else palette.orange
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        AsciiHorizontalRule()
        Spacer(modifier = Modifier.height(6.dp))

        // Repeating user events on this day
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = AppStrings.repeatingEventsHeader(ne),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = palette.green,
                modifier = Modifier.weight(1f)
            )
            BracketButton(
                text = AppStrings.addRepeatingBsEventBtn(ne),
                onClick = onAddEventForDay,
                color = palette.green
            )
        }

        if (dayEvents.isEmpty()) {
            Text(
                text = AppStrings.noRepeatingEventsMsg(ne),
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
                        .background(palette.background)
                        .padding(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val origDayStr = BsCalendarData.formatNumber(instance.entity.bsDay, ne)
                        val clampTag = if (instance.isClampedOrRolled) {
                            AppStrings.clampedDayTag(ne, origDayStr)
                        } else {
                            ""
                        }
                        val holidayTag = if (instance.entity.isPersonalHoliday) {
                            AppStrings.personalHolidayTag(ne)
                        } else {
                            ""
                        }
                        Text(
                            text = "+ ${instance.entity.title}$clampTag$holidayTag",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (instance.entity.isPersonalHoliday) palette.red else palette.green,
                            modifier = Modifier.weight(1f)
                        )
                        Row {
                            BracketButton(
                                text = AppStrings.exportGCalBtn(ne),
                                onClick = { onExportEventToGCal(instance) },
                                color = palette.green
                            )
                            BracketButton(
                                text = AppStrings.editBtn(ne),
                                onClick = { onEditEvent(instance) },
                                color = palette.cyan
                            )
                            BracketButton(
                                text = AppStrings.deleteBtn(ne),
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

                    val timeStr = BsCalendarData.formatTime24h(
                        instance.entity.reminderHour,
                        instance.entity.reminderMinute,
                        ne
                    )
                    val startYearStr = BsCalendarData.formatNumber(instance.entity.startBsYear, ne)
                    val remText = AppStrings.alarmSummary(
                        ne = ne,
                        enabled = instance.entity.reminderEnabled,
                        timeStr = timeStr,
                        daysBefore = instance.entity.reminderDaysBefore,
                        startYearStr = startYearStr
                    )
                    Text(
                        text = remText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = palette.comment
                    )
                    AsciiHorizontalRule()
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
    val ne = state.settings.useDevanagari
    val monthName = BsCalendarData.getMonthName(state.visibleMonth, ne)
    val yearStr = BsCalendarData.formatNumber(state.visibleYear, ne)

    val monthRepeatingEvents = state.eventsByDay.entries
        .sortedBy { it.key }
        .flatMap { (_, list) -> list }

    val monthFestivals = state.days.filter { it.festivals.isNotEmpty() }

    PlaintextBox(
        borderColor = palette.comment,
        headerText = AppStrings.monthScheduleHeader(ne, monthName, yearStr),
        headerColor = palette.foreground
    ) {
        if (monthRepeatingEvents.isEmpty() && monthFestivals.isEmpty()) {
            Text(
                text = AppStrings.noMonthScheduleMsg(ne),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = palette.comment
            )
        }

        if (monthRepeatingEvents.isNotEmpty()) {
            Text(
                text = AppStrings.personalRepeatingSubheader(ne),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = palette.green
            )
            Spacer(modifier = Modifier.height(2.dp))
            monthRepeatingEvents.forEach { item ->
                val dNum = BsCalendarData.formatNumber(item.effectiveBsDate.day, ne, padWidth = 2)
                val origDayStr = BsCalendarData.formatNumber(item.entity.bsDay, ne)
                val clampedSuffix = if (item.isClampedOrRolled) {
                    AppStrings.fromDaySuffix(ne, origDayStr)
                } else {
                    ""
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectDay(item.effectiveBsDate) }
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$dNum | + ${item.entity.title}$clampedSuffix",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = if (item.entity.isPersonalHoliday) palette.red else palette.green,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = AppStrings.editBtn(ne),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = palette.cyan,
                        modifier = Modifier.clickable { onEditEvent(item.entity) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            AsciiHorizontalRule()
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (monthFestivals.isNotEmpty()) {
            Text(
                text = AppStrings.festivalsSubheader(ne),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = palette.orange
            )
            Spacer(modifier = Modifier.height(2.dp))
            monthFestivals.forEach { dayModel ->
                val dNum = BsCalendarData.formatNumber(dayModel.bsDate.day, ne, padWidth = 2)
                val tithiName = if (ne) dayModel.tithi.nepaliName else dayModel.tithi.englishName
                val names = dayModel.festivals.joinToString(", ") {
                    if (ne) it.nameNe else it.nameEn
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
