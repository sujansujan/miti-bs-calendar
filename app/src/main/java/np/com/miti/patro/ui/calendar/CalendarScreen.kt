package np.com.miti.patro.ui.calendar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import np.com.miti.patro.data.local.EventEntity
import np.com.miti.patro.data.repository.ResolvedEventInstance
import np.com.miti.patro.domain.engine.BsCalendarData
import np.com.miti.patro.domain.engine.BsDateConverter
import np.com.miti.patro.domain.engine.NepaliFestivals
import np.com.miti.patro.domain.engine.TithiCalculator
import np.com.miti.patro.domain.model.BsCalendarDay
import np.com.miti.patro.domain.model.BsDate
import np.com.miti.patro.domain.model.MissingDayStrategy
import np.com.miti.patro.ui.components.AsciiTableHorizontalBorder
import np.com.miti.patro.ui.components.BracketButton
import np.com.miti.patro.ui.components.CellArtworkMotif
import np.com.miti.patro.ui.components.FestivalArtResolver
import np.com.miti.patro.ui.components.FestivalCellWatermark
import np.com.miti.patro.ui.components.PlaintextBox
import np.com.miti.patro.ui.components.PlaintextTextInput
import np.com.miti.patro.ui.theme.AppStrings
import np.com.miti.patro.ui.theme.LocalMitiPalette
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

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
    val calNe = state.settings.useDevanagari
    val uiNe = state.settings.useDevanagariEverywhere
    val scrollState = rememberScrollState()

    var showEventDialog by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<EventEntity?>(null) }
    var showJumpDialog by remember { mutableStateOf(false) }
    var dragAccumulated by remember { mutableFloatStateOf(0f) }

    val adRangeHeader = remember(state.daysInMonthGrid, calNe) {
        val nonNullDays = state.daysInMonthGrid.filterNotNull()
        if (nonNullDays.isEmpty()) ""
        else {
            val firstAd = nonNullDays.first().adDate
            val lastAd = nonNullDays.last().adDate
            val m1 = if (calNe) BsCalendarData.getAdMonthName(firstAd.monthValue, true) else firstAd.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase()
            val m2 = if (calNe) BsCalendarData.getAdMonthName(lastAd.monthValue, true) else lastAd.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase()
            val y1 = BsCalendarData.formatNumber(firstAd.year, calNe)
            val y2 = BsCalendarData.formatNumber(lastAd.year, calNe)
            if (firstAd.year == lastAd.year) {
                if (m1 == m2) "$m1 $y1" else "$m1-$m2 $y1"
            } else {
                "$m1 $y1 - $m2 $y2"
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
        // Minimal Top Navigation Bar: [<] [TODAY] [>]   [+] [BS↔AD] [CFG]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BracketButton(label = "<", onClick = onPreviousMonth)
                BracketButton(
                    label = AppStrings.todayBtn(calNe),
                    onClick = onJumpToToday,
                    textColor = palette.cyan
                )
                BracketButton(label = ">", onClick = onNextMonth)
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BracketButton(
                    label = "+",
                    onClick = {
                        editingEvent = null
                        showEventDialog = true
                    },
                    textColor = palette.green,
                    highlighted = true
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
                    fontSize = 12.sp,
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

        // Month & Year Header (Tappable to open Year/Month Jump Dialog)
        val monthTitle = BsCalendarData.monthName(state.viewingMonth, calNe)
        val yearTitle = BsCalendarData.formatNumber(state.viewingYear, calNe)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showJumpDialog = true }
                .padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (calNe) "$monthTitle $yearTitle" else "${monthTitle.uppercase()} $yearTitle",
                color = palette.foreground,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                text = adRangeHeader,
                color = palette.comment,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
        }

        AsciiTableHorizontalBorder()

        // Swipeable Calendar Table Container (swipe left/right to change BS month)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(state.viewingYear, state.viewingMonth) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragAccumulated = 0f },
                        onHorizontalDrag = { _, dragAmount -> dragAccumulated += dragAmount },
                        onDragEnd = {
                            if (dragAccumulated > 90f) {
                                onPreviousMonth()
                            } else if (dragAccumulated < -90f) {
                                onNextMonth()
                            }
                            dragAccumulated = 0f
                        }
                    )
                }
        ) {
            // Weekday Header Row
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
                    fontSize = 12.sp
                )
                for (dow in 0..6) {
                    val isSat = dow == 6
                    Text(
                        text = BsCalendarData.weekdayShort(dow, calNe),
                        color = if (isSat) palette.red else palette.comment,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 4.dp)
                    )
                    Text(
                        text = "|",
                        color = palette.comment,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            }

            AsciiTableHorizontalBorder()

            // Monthly Grid Rows (Tap to highlight; Long-press to quick-add event on that date)
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
                        fontSize = 12.sp,
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
                                    onClick = { onSelectDate(cell.bsDate) },
                                    onLongClick = {
                                        if (state.selectedBsDate != cell.bsDate) {
                                            onSelectDate(cell.bsDate)
                                        }
                                        editingEvent = null
                                        showEventDialog = true
                                    }
                                )
                            }
                        }
                        Text(
                            text = "|",
                            color = palette.comment,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }
                AsciiTableHorizontalBorder()
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Minimal Today / Highlighted Date View
        val selectedCell = state.selectedBsDate?.let { sel ->
            state.daysInMonthGrid.filterNotNull().find { it.bsDate == sel }
        }
        val activeDay = selectedCell ?: state.todayDay
        val activeDayEvents = if (selectedCell != null) {
            state.eventsByDayInMonth[selectedCell.bsDate.day].orEmpty()
        } else {
            state.todayEvents
        }

        MinimalTodayStrip(
            day = activeDay,
            todayAd = state.todayAd,
            isSelectedCell = (selectedCell != null),
            events = activeDayEvents,
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

        Spacer(modifier = Modifier.height(10.dp))

        // Monthly Event & Festival List + Collapsible Year-Wide Search [?]
        MonthEventsAndFestivalsBox(
            year = state.viewingYear,
            month = state.viewingMonth,
            todayBs = state.todayBs,
            todayAd = state.todayAd,
            days = state.daysInMonthGrid.filterNotNull(),
            eventsByDay = state.eventsByDayInMonth,
            allEvents = state.allEvents,
            missingDayStrategy = state.settings.missingDayStrategy,
            showFestivals = state.settings.showPublicFestivals,
            useDevanagari = calNe,
            onSelectDate = onSelectDate
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showEventDialog) {
        // Default date is the highlighted selection if present; otherwise Today's BS date
        EventEditorDialog(
            anchorBsDate = state.effectiveAnchorBsDate,
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
            .height(68.dp)
            .background(palette.background)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BsDayCell(
    day: BsCalendarDay,
    events: List<ResolvedEventInstance>,
    isSelected: Boolean,
    showTithi: Boolean,
    showAd: Boolean,
    showFestivals: Boolean,
    useDevanagari: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
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
    val formattedDayText = if (day.isToday) "[$dayNumberStr]" else dayNumberStr
    val artworkMotif = remember(day.festivals, events, showFestivals) {
        FestivalArtResolver.resolveMotif(day.festivals, events, showFestivals)
    }
    val artworkTint = when (artworkMotif) {
        CellArtworkMotif.SHRADDHA -> palette.purple
        CellArtworkMotif.BIRTHDAY -> primaryEventColor ?: palette.cyan
        else -> if (isHoliday) palette.red else palette.orange
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .background(bgColor)
            .then(
                if (isSelected) Modifier.border(1.5.dp, palette.cyan)
                else Modifier
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 3.dp, vertical = 3.dp)
    ) {
        if (artworkMotif != null) {
            FestivalCellWatermark(
                motif = artworkMotif,
                tint = artworkTint,
                size = 24.dp,
                alpha = if (palette.isDark) 0.30f else 0.24f,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 1.dp, bottom = 1.dp)
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
        // Top Row: Large BS Day Number (16.sp) + AD Day Number (10.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = formattedDayText,
                color = dayNumberColor,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                lineHeight = 16.sp,
                maxLines = 1
            )
            if (showAd) {
                Text(
                    text = BsCalendarData.formatNumber(day.adDate.dayOfMonth, useDevanagari),
                    color = palette.comment,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    lineHeight = 10.sp,
                    maxLines = 1
                )
            }
        }

        // Middle Row: User Event or Festival Title (10.sp)
        if (events.isNotEmpty()) {
            val eventMarkerColor = primaryEventColor ?: palette.green
            val firstTitle = events.first().event.title
            Text(
                text = "●${firstTitle.take(5)}",
                color = eventMarkerColor,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                lineHeight = 11.sp,
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
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                lineHeight = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }

        // Bottom Row: Daily Tithi (10.sp) — highlights Purnima, Aunsi, and Ekadashi
        if (showTithi) {
            val tithiColor = when {
                day.tithi.isPurnima -> palette.yellow
                day.tithi.isAunsi -> palette.purple
                day.tithi.isEkadashi -> palette.green
                else -> palette.comment
            }
            val tithiLabel = if (useDevanagari) {
                day.tithi.shortLabelNe
            } else {
                day.tithi.shortLabel
            }
            Text(
                text = tithiLabel,
                color = tithiColor,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (day.tithi.isPurnima || day.tithi.isAunsi || day.tithi.isEkadashi) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                },
                fontSize = 10.sp,
                lineHeight = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
        }
    }
}

/**
 * Minimal Today / Selected Date strip:
 * Shows BS date, AD date, Lunar Tithi, relative day delta, and any festivals/events on that day.
 */
@Composable
private fun MinimalTodayStrip(
    day: BsCalendarDay,
    todayAd: LocalDate,
    isSelectedCell: Boolean,
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
    val wdHeader = if (ne) BsCalendarData.weekdayFull(day.dayOfWeek, true) else BsCalendarData.weekdayShort(day.dayOfWeek, false)
    val daysDiff = ChronoUnit.DAYS.between(todayAd, day.adDate)
    val relLabel = AppStrings.formatRelativeDays(daysDiff, ne)
    val badge = if (day.isToday) {
        AppStrings.todayTag(ne)
    } else if (isSelectedCell) {
        "${AppStrings.selectedTag(ne)} ($relLabel)"
    } else {
        AppStrings.todayTag(ne)
    }
    val tithiName = if (ne) day.tithi.nepaliName else day.tithi.englishName
    val pakshaShort = if (ne) day.tithi.paksha.nepaliName else day.tithi.paksha.shortCode
    val totalDaysInMonth = BsCalendarData.daysInMonth(day.bsDate.year, day.bsDate.month)
    val monthBoundaryMarker = when (day.bsDate.day) {
        1 -> " • ${AppStrings.sankrantiTag(ne)}"
        totalDaysInMonth -> " • ${AppStrings.masantaTag(ne)}"
        else -> ""
    }
    val adDisplay = if (ne) BsCalendarData.formatAdDate(day.adDate, true) else day.adDate.toString()
    val headerText = if (ne) {
        "$badge • ${day.bsDate.formatHuman(true)}, $wdHeader"
    } else {
        "$badge • ${day.bsDate.formatHuman(false)} ($wdHeader)"
    }

    PlaintextBox(
        header = headerText,
        headerColor = if (day.isToday) palette.cyan else palette.purple
    ) {
        val stripMotif = remember(day.festivals, events, showFestivals) {
            FestivalArtResolver.resolveMotif(day.festivals, events, showFestivals)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${day.tithi.phaseSymbol} $tithiName ($pakshaShort)$monthBoundaryMarker  |  $adDisplay",
                color = palette.foreground,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
            if (stripMotif != null) {
                FestivalCellWatermark(
                    motif = stripMotif,
                    tint = if (day.isPublicHoliday) palette.red else palette.orange,
                    size = 24.dp,
                    alpha = 0.75f,
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
            BracketButton(
                label = "+",
                onClick = onAddEvent,
                textColor = palette.green,
                highlighted = true
            )
        }

        if (showFestivals && day.festivals.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            day.festivals.forEach { fest ->
                val title = if (ne) fest.nepaliTitle else fest.englishTitle
                Text(
                    text = "* $title",
                    color = if (fest.isPublicHoliday) palette.red else palette.orange,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        if (events.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            events.forEach { instance ->
                val evColor = instance.event.resolvedColor.composeColor(palette)
                val evBadge = AppStrings.formatYearAndCountdownBadge(daysDiff, instance.yearsElapsed, ne)
                val shraddhaPrefix = if (instance.event.isShraddha) {
                    val tInfo = TithiCalculator.buildTithiInfo(instance.event.shraddhaTithi)
                    val tShort = if (ne) tInfo.shortLabelNe else tInfo.shortLabelEn
                    "[${AppStrings.shraddhaTag(ne)}: ${tInfo.paksha.shortCode}-$tShort] "
                } else ""
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .background(palette.background)
                        .border(1.dp, evColor)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "● ${instance.event.title} $evBadge",
                            color = evColor,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        if (shraddhaPrefix.isNotEmpty() || instance.event.notes.isNotBlank()) {
                            Text(
                                text = "$shraddhaPrefix${instance.event.notes}".trim(),
                                color = palette.comment,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        BracketButton(
                            label = AppStrings.gcalExportBtn(ne),
                            onClick = { onSyncEventToGCal(instance.event) },
                            textColor = palette.cyan
                        )
                        BracketButton(
                            label = AppStrings.editBtn(ne),
                            onClick = { onEditEvent(instance.event) },
                            textColor = palette.yellow
                        )
                        BracketButton(
                            label = AppStrings.delBtn(ne),
                            onClick = { onDeleteEvent(instance.event) },
                            textColor = palette.red
                        )
                    }
                }
            }
        }
    }
}

private data class YearSearchItem(
    val bsDate: BsDate,
    val adDate: LocalDate,
    val label: String,
    val badge: String,
    val color: Color,
    val isPast: Boolean,
    val isToday: Boolean
)

/**
 * Monthly Event & Festival List + Collapsible Year-Wide Search (`[?]`):
 * - Past events (d.bsDate < todayBs) are grayed out
 * - Today's events (d.bsDate == todayBs) are highlighted with selection background & cyan border
 * - Shows inline countdown (`in 4d` / `3d ago`) and year count (`#5`)
 * - Tapping `[?]` opens a 1-line search box that searches across all 12 months of `year`
 */
@Composable
private fun MonthEventsAndFestivalsBox(
    year: Int,
    month: Int,
    todayBs: BsDate,
    todayAd: LocalDate,
    days: List<BsCalendarDay>,
    eventsByDay: Map<Int, List<ResolvedEventInstance>>,
    allEvents: List<EventEntity>,
    missingDayStrategy: MissingDayStrategy,
    showFestivals: Boolean,
    useDevanagari: Boolean,
    onSelectDate: (BsDate) -> Unit
) {
    val palette = LocalMitiPalette.current
    val ne = useDevanagari
    val monthName = BsCalendarData.monthName(month, ne)
    val yearStr = BsCalendarData.formatNumber(year, ne)

    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    PlaintextBox(header = AppStrings.monthScheduleHeader(ne, monthName, yearStr)) {
        // Minimal collapsible search toggle row at top-right of the box
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showSearch) {
                PlaintextTextInput(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = AppStrings.searchPlaceholder(ne),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                BracketButton(
                    label = "X",
                    onClick = {
                        searchQuery = ""
                        showSearch = false
                    },
                    textColor = palette.red
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
                BracketButton(
                    label = "?",
                    onClick = { showSearch = true },
                    textColor = palette.cyan
                )
            }
        }

        val trimmedQuery = searchQuery.trim()
        val pastGray = palette.comment.copy(alpha = 0.55f)

        if (showSearch && trimmedQuery.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            val searchMatches = remember(trimmedQuery, year, allEvents, showFestivals, ne, todayAd) {
                val results = mutableListOf<YearSearchItem>()
                // 1. Search all user events resolved in `year`
                for (ev in allEvents) {
                    if (year < ev.startBsYear) continue
                    val matchesText = ev.title.contains(trimmedQuery, ignoreCase = true) ||
                        ev.note.contains(trimmedQuery, ignoreCase = true) ||
                        (ev.isShraddha && ("shraddha".contains(trimmedQuery, ignoreCase = true) || "श्राद्ध".contains(trimmedQuery)))
                    if (!matchesText) continue

                    val occ = BsDateConverter.resolveEventOccurrence(ev, year, missingDayStrategy) ?: continue
                    val ad = BsDateConverter.bsToAd(occ.targetDate)
                    val daysDiff = ChronoUnit.DAYS.between(todayAd, ad)
                    val yrs = (year - ev.startBsYear).coerceAtLeast(0)
                    val mShort = BsCalendarData.getShortMonthName(occ.targetDate.month, ne)
                    val dPad = BsCalendarData.formatNumberPadded(occ.targetDate.day, ne)
                    val shraddhaTag = if (ev.isShraddha) " (${AppStrings.shraddhaTag(ne)})" else ""
                    results.add(
                        YearSearchItem(
                            bsDate = occ.targetDate,
                            adDate = ad,
                            label = "$mShort $dPad | ● ${ev.title}$shraddhaTag",
                            badge = AppStrings.formatYearAndCountdownBadge(daysDiff, yrs, ne),
                            color = ev.resolvedColor.composeColor(palette),
                            isPast = occ.targetDate < todayBs,
                            isToday = occ.targetDate == todayBs
                        )
                    )
                }

                // 2. Search festivals across all 12 months of `year`
                if (showFestivals) {
                    for (m in 1..12) {
                        val dim = BsCalendarData.daysInMonth(year, m)
                        val firstAd = BsDateConverter.bsToAd(year, m, 1)
                        val mShort = BsCalendarData.getShortMonthName(m, ne)
                        for (d in 1..dim) {
                            val bs = BsDate(year, m, d)
                            val ad = firstAd.plusDays((d - 1).toLong())
                            val tithi = TithiCalculator.calculateTithi(bs, ad)
                            val fests = NepaliFestivals.getFestivalsForDay(bs, tithi)
                            for (f in fests) {
                                if (f.englishTitle.contains(trimmedQuery, ignoreCase = true) ||
                                    f.nepaliTitle.contains(trimmedQuery, ignoreCase = true)
                                ) {
                                    val daysDiff = ChronoUnit.DAYS.between(todayAd, ad)
                                    val dPad = BsCalendarData.formatNumberPadded(d, ne)
                                    val title = if (ne) f.nepaliTitle else f.englishTitle
                                    results.add(
                                        YearSearchItem(
                                            bsDate = bs,
                                            adDate = ad,
                                            label = "$mShort $dPad | * $title",
                                            badge = "[${AppStrings.formatRelativeDays(daysDiff, ne)}]",
                                            color = if (f.isPublicHoliday) palette.red else palette.orange,
                                            isPast = bs < todayBs,
                                            isToday = bs == todayBs
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                results.sortedBy { it.bsDate }
            }

            if (searchMatches.isEmpty()) {
                Text(
                    text = AppStrings.noSearchMatches(ne),
                    color = palette.comment,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                )
            } else {
                searchMatches.forEach { item ->
                    val itemColor = if (item.isPast) pastGray else item.color
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectDate(item.bsDate)
                                showSearch = false
                                searchQuery = ""
                            }
                            .padding(horizontal = 2.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${item.label} ${item.badge}",
                            color = itemColor,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (item.isToday || !item.isPast) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        } else {
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
                    fontSize = 13.sp
                )
            } else {
                notableDays.forEach { d ->
                    val isPast = d.bsDate < todayBs
                    val isToday = d.bsDate == todayBs
                    val daysDiff = ChronoUnit.DAYS.between(todayAd, d.adDate)
                    val dayNum = BsCalendarData.formatNumberPadded(d.bsDate.day, ne)
                    val wd = BsCalendarData.weekdayShort(d.dayOfWeek, ne)
                    val dayEvents = eventsByDay[d.bsDate.day].orEmpty()

                    val rowModifier = if (isToday) {
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .background(palette.selection)
                            .border(1.dp, palette.cyan)
                            .clickable { onSelectDate(d.bsDate) }
                            .padding(horizontal = 6.dp, vertical = 5.dp)
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelectDate(d.bsDate) }
                            .padding(horizontal = 2.dp, vertical = 4.dp)
                    }

                    dayEvents.forEach { ev ->
                        val activeColor = ev.event.resolvedColor.composeColor(palette)
                        val itemColor = if (isPast) pastGray else activeColor
                        val badge = AppStrings.formatYearAndCountdownBadge(daysDiff, ev.yearsElapsed, ne)
                        val shraddhaSuffix = if (ev.event.isShraddha) " (${AppStrings.shraddhaTag(ne)})" else ""
                        Row(
                            modifier = rowModifier,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$dayNum $wd | ● ${ev.event.title}$shraddhaSuffix $badge",
                                color = itemColor,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isToday || !isPast) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (showFestivals) {
                        val festDeltaBadge = "[${AppStrings.formatRelativeDays(daysDiff, ne)}]"
                        d.festivals.forEach { f ->
                            val title = if (ne) f.nepaliTitle else f.englishTitle
                            val itemColor = when {
                                isPast -> pastGray
                                isToday -> palette.cyan
                                f.isPublicHoliday -> palette.red
                                else -> palette.orange
                            }
                            Row(
                                modifier = rowModifier,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$dayNum $wd | * $title $festDeltaBadge",
                                    color = itemColor,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
