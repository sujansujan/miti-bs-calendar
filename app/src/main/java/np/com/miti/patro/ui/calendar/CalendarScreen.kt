package np.com.miti.patro.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import np.com.miti.patro.data.local.EventEntity
import np.com.miti.patro.data.repository.ResolvedEventInstance
import np.com.miti.patro.domain.engine.BsCalendarData
import np.com.miti.patro.domain.model.BsCalendarDay
import np.com.miti.patro.domain.model.BsDate
import np.com.miti.patro.domain.model.Paksha
import np.com.miti.patro.ui.components.AsciiHorizontalRule
import np.com.miti.patro.ui.components.AsciiTableHorizontalBorder
import np.com.miti.patro.ui.components.BracketButton
import np.com.miti.patro.ui.components.PlaintextBox
import np.com.miti.patro.ui.theme.AppStrings
import np.com.miti.patro.ui.theme.LocalMitiPalette
import java.time.format.TextStyle
import java.util.Locale

/**
 * Main monthly BS Calendar screen inspired by https://plaintextsports.com/
 *
 * - Literal `-------` horizontal lines (`AsciiTableHorizontalBorder`) and `|` column pipes
 * - No app title or theme badge on the calendar screen
 * - Supports Nepali on Calendar tab only (`calNe`) vs Nepali everywhere (`uiNe`)
 * - Renders user-selected `EventColor` on cell markers and event lists
 */
@Composable
fun CalendarScreen(
    state: CalendarUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit,
    onJumpToMonthYear: (Int, Int) -> Unit,
    onJumpToToday: () -> Unit,
    onSelectDate: (BsDate) -> Unit,
    onSaveEvent: (EventEntity) -> Unit,
    onDeleteEvent: (EventEntity) -> Unit,
    onSyncEventToGCal: (EventEntity) -> Unit,
    onDismissStatusBanner: () -> Unit,
    onOpenConverter: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val palette = LocalMitiPalette.current
    // calNe controls the Calendar tab's own content; uiNe controls global chrome & dialogs
    val calNe = state.settings.useDevanagari
    val uiNe = state.settings.useDevanagariEverywhere
    val scrollState = rememberScrollState()

    var showEventDialog by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<EventEntity?>(null) }
    var showJumpDialog by remember { mutableStateOf(false) }

    val adRangeHeader = remember(state.daysInMonthGrid) {
        val nonNullDays = state.daysInMonthGrid.filterNotNull()
        if (nonNullDays.isEmpty()) ""
        else {
            val firstAd = nonNullDays.first().adDate
            val lastAd = nonNullDays.last().adDate
            val m1 = firstAd.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase()
            val m2 = lastAd.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase()
            if (firstAd.year == lastAd.year) {
                if (m1 == m2) "$m1 ${firstAd.year}" else "$m1-$m2 ${firstAd.year}"
            } else {
                "$m1 ${firstAd.year} - $m2 ${lastAd.year}"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        // Top Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BracketButton(label = "<<", onClick = onPreviousYear)
                BracketButton(label = "<", onClick = onPreviousMonth)
                BracketButton(
                    label = AppStrings.todayBtn(calNe),
                    onClick = onJumpToToday,
                    textColor = palette.cyan
                )
                BracketButton(label = ">", onClick = onNextMonth)
                BracketButton(label = ">>", onClick = onNextYear)
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BracketButton(
                    label = AppStrings.addEventBtn(calNe),
                    onClick = {
                        editingEvent = null
                        showEventDialog = true
                    },
                    textColor = palette.green
                )
                BracketButton(
                    label = AppStrings.convertBtn(uiNe),
                    onClick = onOpenConverter,
                    textColor = palette.cyan
                )
                BracketButton(
                    label = AppStrings.cfgBtn(uiNe),
                    onClick = onOpenSettings,
                    textColor = palette.purple
                )
            }
        }

        // Optional status banner (e.g., when [+GCAL (BS->AD)] is tapped on an event)
        if (state.statusBannerMessage != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface)
                    .border(1.dp, palette.green)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = state.statusBannerMessage,
                    color = palette.green,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                BracketButton(
                    label = "X",
                    onClick = onDismissStatusBanner,
                    textColor = palette.comment
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        AsciiTableHorizontalBorder()

        // Month & Year Banner (Tappable to open Quick Jump)
        val monthTitle = BsCalendarData.monthName(state.viewingMonth, calNe)
        val yearTitle = BsCalendarData.formatNumber(state.viewingYear, calNe)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showJumpDialog = true }
                .padding(vertical = 4.dp, horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (calNe) {
                    "$yearTitle $monthTitle बि.सं. [छान्नुहोस्]"
                } else {
                    "${monthTitle.uppercase()} $yearTitle BS [JUMP]"
                },
                color = palette.foreground,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                text = adRangeHeader,
                color = palette.comment,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        }

        AsciiTableHorizontalBorder()

        // Weekday Header Row with literal | pipes
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "|",
                color = palette.comment,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
            for (dow in 0..6) {
                val isSat = dow == 6
                Text(
                    text = BsCalendarData.weekdayShort(dow, calNe),
                    color = if (isSat) palette.red else palette.comment,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 3.dp)
                )
                Text(
                    text = "|",
                    color = palette.comment,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }
        }

        AsciiTableHorizontalBorder()

        // Monthly Grid Rows separated by literal ------- borders and | pipes
        val weeks = state.daysInMonthGrid.chunked(7)
        weeks.forEach { week ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                Text(
                    text = "|",
                    color = palette.comment,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                week.forEach { cell ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        if (cell == null) {
                            EmptyDayCell()
                        } else {
                            val dayEvents = state.eventsByDayInMonth[cell.bsDate.day].orEmpty()
                            val isSelected = cell.bsDate == state.selectedBsDate
                            BsDayCell(
                                day = cell,
                                events = dayEvents,
                                isSelected = isSelected,
                                showTithi = state.settings.showTithiInGrid,
                                showAd = state.settings.showGregorianInGrid,
                                showFestivals = state.settings.showPublicFestivals,
                                useDevanagari = calNe,
                                onClick = { onSelectDate(cell.bsDate) }
                            )
                        }
                    }
                    Text(
                        text = "|",
                        color = palette.comment,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }
            AsciiTableHorizontalBorder()
        }

        // Compact Legend Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = AppStrings.legendHoliday(calNe),
                color = palette.red,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
            Text(
                text = AppStrings.legendEvent(calNe),
                color = palette.green,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
            Text(
                text = AppStrings.legendToday(calNe),
                color = palette.cyan,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Selected Day Detail Box
        val selectedCell = state.daysInMonthGrid.filterNotNull().find {
            it.bsDate == state.selectedBsDate
        }
        if (selectedCell != null) {
            val selectedDayEvents = state.eventsByDayInMonth[selectedCell.bsDate.day].orEmpty()
            SelectedDayDetailBox(
                day = selectedCell,
                events = selectedDayEvents,
                showFestivals = state.settings.showPublicFestivals,
                useDevanagari = calNe,
                onAddEvent = {
                    editingEvent = null
                    showEventDialog = true
                },
                onEditEvent = { ev ->
                    editingEvent = ev
                    showEventDialog = true
                },
                onDeleteEvent = onDeleteEvent,
                onSyncEventToGCal = onSyncEventToGCal
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Month Schedule Box (All Repeating Events + Festivals for the month)
        MonthEventsAndFestivalsBox(
            year = state.viewingYear,
            month = state.viewingMonth,
            days = state.daysInMonthGrid.filterNotNull(),
            eventsByDay = state.eventsByDayInMonth,
            showFestivals = state.settings.showPublicFestivals,
            useDevanagari = calNe,
            onSelectDate = onSelectDate
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showEventDialog) {
        EventEditorDialog(
            anchorBsDate = state.selectedBsDate,
            existingEvent = editingEvent,
            defaultStrategy = state.settings.defaultMissingDayStrategy,
            defaultReminderHour = state.settings.defaultReminderHour,
            defaultReminderMinute = state.settings.defaultReminderMinute,
            defaultReminderDaysBefore = state.settings.defaultReminderDaysBefore,
            useDevanagari = uiNe,
            onDismiss = {
                showEventDialog = false
                editingEvent = null
            },
            onSave = { entity ->
                onSaveEvent(entity)
                showEventDialog = false
                editingEvent = null
            }
        )
    }

    if (showJumpDialog) {
        YearMonthJumpDialog(
            currentYear = state.viewingYear,
            currentMonth = state.viewingMonth,
            useDevanagari = uiNe,
            onDismiss = { showJumpDialog = false },
            onConfirm = { y, m ->
                onJumpToMonthYear(y, m)
                showJumpDialog = false
            }
        )
    }
}

@Composable
private fun EmptyDayCell() {
    val palette = LocalMitiPalette.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .background(palette.background)
    )
}

@Composable
private fun BsDayCell(
    day: BsCalendarDay,
    events: List<ResolvedEventInstance>,
    isSelected: Boolean,
    showTithi: Boolean,
    showAd: Boolean,
    showFestivals: Boolean,
    useDevanagari: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalMitiPalette.current
    val hasFestivalHoliday = showFestivals && day.festivals.any { it.isPublicHoliday }
    val hasPersonalHoliday = events.any { it.event.isPersonalHoliday }
    val isHoliday = day.isSaturday || hasFestivalHoliday || hasPersonalHoliday
    val primaryEventColor = events.firstOrNull()?.event?.resolvedColor?.composeColor(palette)

    val bgColor = when {
        isSelected -> palette.selection
        day.isToday -> palette.surface
        else -> palette.background
    }

    val dayNumberColor = when {
        isHoliday -> palette.red
        day.isToday -> palette.cyan
        primaryEventColor != null -> primaryEventColor
        else -> palette.foreground
    }

    val dayNumberStr = BsCalendarData.formatNumber(day.bsDate.day, useDevanagari)
    val formattedDayText = when {
        day.isToday -> "[$dayNumberStr]"
        isHoliday -> "$dayNumberStr*"
        else -> dayNumberStr
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Row: BS Day Number + AD Day Number
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = formattedDayText,
                color = dayNumberColor,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (day.isToday || isSelected || isHoliday || events.isNotEmpty()) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                },
                fontSize = 12.sp,
                maxLines = 1
            )
            if (showAd) {
                Text(
                    text = day.adDate.dayOfMonth.toString(),
                    color = palette.comment,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    maxLines = 1
                )
            }
        }

        // Middle Row: User Event Marker (+N in chosen color) or Festival dot
        if (events.isNotEmpty()) {
            val eventMarkerColor = primaryEventColor ?: palette.green
            val countStr = BsCalendarData.formatNumber(events.size, useDevanagari)
            val firstTitle = events.first().event.title
            Text(
                text = "+$countStr ${firstTitle.take(4)}",
                color = eventMarkerColor,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 8.sp,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        } else if (showFestivals && day.festivals.isNotEmpty()) {
            val f = day.festivals.first()
            val fTitle = if (useDevanagari) f.nepaliTitle else f.englishTitle
            Text(
                text = fTitle.take(6),
                color = if (f.isPublicHoliday) palette.red else palette.orange,
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }

        // Bottom Row: Daily Tithi (Aunsi through Purnima)
        if (showTithi) {
            val tithiColor = when {
                day.tithi.isPurnima -> palette.yellow
                day.tithi.isAunsi -> palette.purple
                else -> palette.comment
            }
            val tithiLabel = if (useDevanagari) {
                day.tithi.nepaliName.take(6)
            } else {
                day.tithi.shortLabel
            }
            Text(
                text = tithiLabel,
                color = tithiColor,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (day.tithi.isPurnima || day.tithi.isAunsi) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                },
                fontSize = 8.sp,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
private fun SelectedDayDetailBox(
    day: BsCalendarDay,
    events: List<ResolvedEventInstance>,
    showFestivals: Boolean,
    useDevanagari: Boolean,
    onAddEvent: () -> Unit,
    onEditEvent: (EventEntity) -> Unit,
    onDeleteEvent: (EventEntity) -> Unit,
    onSyncEventToGCal: (EventEntity) -> Unit
) {
    val palette = LocalMitiPalette.current
    val ne = useDevanagari
    val weekdayName = BsCalendarData.weekdayFull(day.dayOfWeek, ne)
    val bsFull = "${day.bsDate.formatHuman(ne)} ($weekdayName)"

    PlaintextBox(header = "${AppStrings.selectedDayHeader(ne)} — $bsFull") {
        // Row 1: Gregorian (AD) Date
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = AppStrings.gregorianLabel(ne),
                color = palette.comment,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
            Text(
                text = "${day.adDate} (${day.adDate.dayOfWeek.name})",
                color = palette.foreground,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Row 2: Lunar Tithi & Paksha
        val pakshaName = if (ne) day.tithi.paksha.nepaliName else day.tithi.paksha.englishName
        val tithiName = if (ne) day.tithi.nepaliName else day.tithi.englishName
        val pakshaSymbol = when {
            day.tithi.isPurnima -> "●"
            day.tithi.isAunsi -> "○"
            day.tithi.paksha == Paksha.SHUKLA -> "◐+"
            else -> "◑-"
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = AppStrings.tithiLabel(ne),
                color = palette.comment,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
            Text(
                text = "$pakshaSymbol $tithiName ($pakshaName)",
                color = if (day.tithi.isPurnima || day.tithi.isAunsi) palette.yellow else palette.cyan,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Row 3: Holiday Status
        val hasFestHoliday = showFestivals && day.festivals.any { it.isPublicHoliday }
        val hasPersonalHoliday = events.any { it.event.isPersonalHoliday }
        val statusText = when {
            day.isSaturday -> AppStrings.publicHolidaySaturday(ne)
            hasFestHoliday -> AppStrings.publicHolidayFestival(ne)
            hasPersonalHoliday -> AppStrings.personalEventHoliday(ne)
            else -> AppStrings.workingDay(ne)
        }
        val statusColor = if (day.isSaturday || hasFestHoliday || hasPersonalHoliday) {
            palette.red
        } else {
            palette.green
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = AppStrings.holidayStatusLabel(ne),
                color = palette.comment,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
            Text(
                text = statusText,
                color = statusColor,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }

        // Festivals List
        if (showFestivals && day.festivals.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            AsciiHorizontalRule()
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = AppStrings.festivalsHeader(ne),
                color = palette.orange,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            day.festivals.forEach { fest ->
                val title = if (ne) {
                    "${fest.nepaliTitle} (${fest.englishTitle})"
                } else {
                    "${fest.englishTitle} (${fest.nepaliTitle})"
                }
                val tag = if (fest.isPublicHoliday) " ${AppStrings.publicHolidayTag(ne)}" else ""
                Text(
                    text = " * $title$tag",
                    color = if (fest.isPublicHoliday) palette.red else palette.foreground,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }
        }

        // Repeating BS Events on this day
        Spacer(modifier = Modifier.height(6.dp))
        AsciiHorizontalRule()
        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = AppStrings.eventsOnDayHeader(ne),
            color = palette.green,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(4.dp))

        if (events.isEmpty()) {
            Text(
                text = AppStrings.noEventsOnDay(ne),
                color = palette.comment,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        } else {
            events.forEach { instance ->
                val evColor = instance.event.resolvedColor.composeColor(palette)
                val colorName = instance.event.resolvedColor.label(ne)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .background(palette.background)
                        .border(1.dp, evColor)
                        .padding(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "● ${instance.event.title} [$colorName]",
                            color = evColor,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    val anchorMonthName = BsCalendarData.monthName(instance.event.bsMonth, ne)
                    val anchorDayStr = BsCalendarData.formatNumber(instance.event.bsDay, ne)
                    val repeatNote = if (ne) {
                        "प्रत्येक बि.सं. वर्ष $anchorMonthName $anchorDayStr मा दोहोरिने"
                    } else {
                        "Repeats every BS year on $anchorMonthName $anchorDayStr"
                    }
                    Text(
                        text = repeatNote,
                        color = palette.comment,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )

                    if (instance.wasAdjustedForShortMonth) {
                        val adjNote = if (ne) {
                            "[नोट: यो वर्ष महिनामा कम दिन भएकोले ${instance.event.bsDay} गतेबाट सारिएको]"
                        } else {
                            "[Adjusted from day ${instance.event.bsDay} due to shorter BS month this year]"
                        }
                        Text(
                            text = adjNote,
                            color = palette.orange,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }

                    if (instance.event.notes.isNotBlank()) {
                        Text(
                            text = instance.event.notes,
                            color = palette.foreground,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        BracketButton(
                            label = AppStrings.gcalExportBtn(ne),
                            onClick = { onSyncEventToGCal(instance.event) },
                            textColor = palette.cyan
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        BracketButton(
                            label = AppStrings.editBtn(ne),
                            onClick = { onEditEvent(instance.event) },
                            textColor = palette.yellow
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        BracketButton(
                            label = AppStrings.delBtn(ne),
                            onClick = { onDeleteEvent(instance.event) },
                            textColor = palette.red
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        BracketButton(
            label = AppStrings.addEventOnDayBtn(ne),
            onClick = onAddEvent,
            textColor = palette.green,
            highlighted = true
        )
    }
}

@Composable
private fun MonthEventsAndFestivalsBox(
    year: Int,
    month: Int,
    days: List<BsCalendarDay>,
    eventsByDay: Map<Int, List<ResolvedEventInstance>>,
    showFestivals: Boolean,
    useDevanagari: Boolean,
    onSelectDate: (BsDate) -> Unit
) {
    val palette = LocalMitiPalette.current
    val ne = useDevanagari
    val monthName = BsCalendarData.monthName(month, ne)
    val yearStr = BsCalendarData.formatNumber(year, ne)

    PlaintextBox(header = AppStrings.monthScheduleHeader(ne, monthName, yearStr)) {
        val notableDays = days.filter { d ->
            val hasEv = !eventsByDay[d.bsDate.day].isNullOrEmpty()
            val hasFest = showFestivals && d.festivals.isNotEmpty()
            hasEv || hasFest
        }

        if (notableDays.isEmpty()) {
            Text(
                text = AppStrings.noMonthItems(ne),
                color = palette.comment,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        } else {
            notableDays.forEach { d ->
                val dayNum = BsCalendarData.formatNumberPadded(d.bsDate.day, ne)
                val wd = BsCalendarData.weekdayShort(d.dayOfWeek, ne)
                val dayEvents = eventsByDay[d.bsDate.day].orEmpty()

                dayEvents.forEach { ev ->
                    val evColor = ev.event.resolvedColor.composeColor(palette)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectDate(d.bsDate) }
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$dayNum ($wd) | ● ${ev.event.title}",
                            color = evColor,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = d.adDate.toString(),
                            color = palette.comment,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                }

                if (showFestivals) {
                    d.festivals.forEach { f ->
                        val title = if (ne) f.nepaliTitle else f.englishTitle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectDate(d.bsDate) }
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$dayNum ($wd) | * $title",
                                color = if (f.isPublicHoliday) palette.red else palette.orange,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = d.adDate.toString(),
                                color = palette.comment,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
