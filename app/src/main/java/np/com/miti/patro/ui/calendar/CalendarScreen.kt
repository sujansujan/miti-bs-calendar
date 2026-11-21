package np.com.miti.patro.ui.calendar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import np.com.miti.patro.ui.components.AsciiGearIcon
import np.com.miti.patro.ui.components.AsciiHorizontalRule
import np.com.miti.patro.ui.components.AsciiLunarPhaseIcon
import np.com.miti.patro.ui.components.AsciiTableHorizontalBorder
import np.com.miti.patro.ui.components.AsciiVerticalPipe
import np.com.miti.patro.ui.components.BracketButton
import np.com.miti.patro.ui.components.BracketIconButton
import np.com.miti.patro.ui.components.CellArtworkMotif
import np.com.miti.patro.ui.components.FestivalArtResolver
import np.com.miti.patro.ui.components.FestivalCellWatermark
import np.com.miti.patro.ui.components.MitiHaptic
import np.com.miti.patro.ui.components.PlaintextBox
import np.com.miti.patro.ui.components.PlaintextTextInput
import np.com.miti.patro.ui.components.rememberMitiHaptics
import np.com.miti.patro.ui.theme.AppStrings
import np.com.miti.patro.ui.theme.LocalMitiPalette
import np.com.miti.patro.ui.theme.MitiMonospaceFont
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

private const val TOTAL_MONTH_PAGES = (BsCalendarData.MAX_YEAR - BsCalendarData.MIN_YEAR + 1) * 12

private fun monthToPage(year: Int, month: Int): Int {
    return ((year - BsCalendarData.MIN_YEAR) * 12 + (month - 1)).coerceIn(0, TOTAL_MONTH_PAGES - 1)
}

private fun pageToYearMonth(page: Int): Pair<Int, Int> {
    val safePage = page.coerceIn(0, TOTAL_MONTH_PAGES - 1)
    return (BsCalendarData.MIN_YEAR + safePage / 12) to (safePage % 12 + 1)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarScreen(
    state: CalendarUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit,
    onJumpToMonthYear: (Int, Int) -> Unit,
    onJumpToToday: () -> Unit,
    onJumpToDate: (BsDate) -> Unit,
    onPagerMonthChanged: (Int, Int) -> Unit,
    getMonthGridForPage: (Int, Int, BsDate) -> List<BsCalendarDay?>,
    getEventsForMonthPage: (List<EventEntity>, Int, Int, MissingDayStrategy) -> Map<Int, List<ResolvedEventInstance>>,
    onSelectDate: (BsDate) -> Unit,
    onSaveEvent: (EventEntity) -> Unit,
    onDeleteEvent: (EventEntity) -> Unit,
    onUndoDeleteEvent: () -> Unit,
    onSyncEventToGCal: (EventEntity) -> Unit,
    onDismissStatusBanner: () -> Unit,
    onOpenConverter: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val palette = LocalMitiPalette.current
    val haptics = rememberMitiHaptics()
    val coroutineScope = rememberCoroutineScope()
    val calNe = state.settings.useDevanagari
    val uiNe = state.settings.useDevanagariEverywhere
    val scrollState = rememberScrollState()

    var showEventDialog by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<EventEntity?>(null) }
    var showJumpDialog by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(
        initialPage = monthToPage(state.viewingYear, state.viewingMonth),
        pageCount = { TOTAL_MONTH_PAGES }
    )
    val currentViewingYm by rememberUpdatedState(state.viewingYear to state.viewingMonth)

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            val ym = pageToYearMonth(page)
            if (ym != currentViewingYm) {
                haptics(MitiHaptic.TICK)
                onPagerMonthChanged(ym.first, ym.second)
            }
        }
    }

    val targetPage = monthToPage(state.viewingYear, state.viewingMonth)
    LaunchedEffect(targetPage) {
        if (pagerState.currentPage != targetPage) {
            if (abs(pagerState.currentPage - targetPage) == 1) {
                pagerState.animateScrollToPage(targetPage)
            } else {
                pagerState.scrollToPage(targetPage)
            }
        }
    }

    val adNe = calNe && !state.settings.adDatesInEnglish
    val adRangeHeader = remember(state.daysInMonthGrid, adNe) {
        val nonNullDays = state.daysInMonthGrid.filterNotNull()
        if (nonNullDays.isEmpty()) ""
        else {
            val firstAd = nonNullDays.first().adDate
            val lastAd = nonNullDays.last().adDate
            val m1 = if (adNe) BsCalendarData.getAdMonthName(firstAd.monthValue, true) else firstAd.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase()
            val m2 = if (adNe) BsCalendarData.getAdMonthName(lastAd.monthValue, true) else lastAd.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase()
            val y1 = BsCalendarData.formatNumber(firstAd.year, adNe)
            val y2 = BsCalendarData.formatNumber(lastAd.year, adNe)
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
        val todayDateLabel = "${BsCalendarData.getShortMonthName(state.todayBs.month, calNe)} ${BsCalendarData.formatNumber(state.todayBs.day, calNe)}"
        val isAtToday = state.viewingYear == state.todayBs.year &&
            state.viewingMonth == state.todayBs.month &&
            (state.selectedBsDate == null || state.selectedBsDate == state.todayBs)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BracketButton(
                    label = "<<",
                    onClick = onPreviousYear,
                    textColor = palette.comment,
                    compact = true
                )
                BracketButton(
                    label = "<",
                    onClick = onPreviousMonth,
                    textColor = palette.foreground,
                    compact = true
                )
                BracketButton(
                    label = todayDateLabel,
                    onClick = {
                        onJumpToToday()
                        coroutineScope.launch { scrollState.animateScrollTo(0) }
                    },
                    textColor = palette.cyan,
                    backgroundColor = if (isAtToday) Color.Transparent else palette.selection,
                    compact = true,
                    haptic = MitiHaptic.CONFIRM
                )
                BracketButton(
                    label = ">",
                    onClick = onNextMonth,
                    textColor = palette.foreground,
                    compact = true
                )
                BracketButton(
                    label = ">>",
                    onClick = onNextYear,
                    textColor = palette.comment,
                    compact = true
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BracketButton(
                    label = AppStrings.convertBtn(uiNe),
                    onClick = onOpenConverter,
                    textColor = palette.cyan,
                    compact = true
                )
                BracketIconButton(
                    onClick = onOpenSettings,
                    tint = palette.purple,
                    compact = true
                ) { iconColor ->
                    AsciiGearIcon(color = iconColor)
                }
            }
        }

        if (state.statusBannerMessage != null) {
            Spacer(modifier = Modifier.height(6.dp))
            val isUndoBanner = state.undoableDeletedEvent != null
            val bannerColor = if (isUndoBanner) palette.orange else palette.green
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface)
                    .border(1.dp, bannerColor)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = state.statusBannerMessage,
                    color = bannerColor,
                    fontFamily = MitiMonospaceFont,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (isUndoBanner) {
                        BracketButton(
                            label = AppStrings.undoBtn(uiNe),
                            highlighted = true,
                            onClick = onUndoDeleteEvent,
                            textColor = palette.cyan,
                            haptic = MitiHaptic.CONFIRM
                        )
                    }
                    BracketButton(
                        label = "X",
                        onClick = onDismissStatusBanner,
                        textColor = palette.comment
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        AsciiTableHorizontalBorder(columns = 1)

        val monthTitle = BsCalendarData.monthName(state.viewingMonth, calNe)
        val yearTitle = BsCalendarData.formatNumber(state.viewingYear, calNe)
        val hasAdhikMaas = remember(state.daysInMonthGrid) {
            state.daysInMonthGrid.any { it?.tithi?.isAdhikMaas == true }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    haptics(MitiHaptic.TICK)
                    showJumpDialog = true
                }
                .padding(horizontal = 4.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val headerLabel = if (calNe) {
                    "$monthTitle $yearTitle ▾"
                } else {
                    "${monthTitle.uppercase()} $yearTitle ▾"
                }
                Text(
                    text = headerLabel,
                    color = palette.foreground,
                    fontFamily = MitiMonospaceFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (hasAdhikMaas) {
                    Text(
                        text = AppStrings.adhikMaasTag(calNe),
                        color = palette.pink,
                        fontFamily = MitiMonospaceFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }
            if (adRangeHeader.isNotEmpty()) {
                Text(
                    text = adRangeHeader,
                    color = palette.comment,
                    fontFamily = MitiMonospaceFont,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }

        AsciiTableHorizontalBorder(columns = 7)

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsciiVerticalPipe()
                for (dow in 0..6) {
                    val isSat = dow == 6
                    Text(
                        text = BsCalendarData.weekdayShort(dow, calNe),
                        color = if (isSat) palette.red else palette.comment,
                        fontFamily = MitiMonospaceFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 4.dp)
                    )
                    AsciiVerticalPipe()
                }
            }

            AsciiTableHorizontalBorder(columns = 7)

            HorizontalPager(
                state = pagerState,
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                val (pYear, pMonth) = pageToYearMonth(page)
                val isViewingPage = (pYear == state.viewingYear && pMonth == state.viewingMonth)
                val pageGrid = if (isViewingPage) {
                    state.daysInMonthGrid
                } else {
                    remember(pYear, pMonth, state.todayBs) {
                        getMonthGridForPage(pYear, pMonth, state.todayBs)
                    }
                }
                val pageEvents = if (isViewingPage) {
                    state.eventsByDayInMonth
                } else {
                    remember(pYear, pMonth, state.allEvents, state.settings.missingDayStrategy) {
                        getEventsForMonthPage(
                            state.allEvents,
                            pYear,
                            pMonth,
                            state.settings.missingDayStrategy
                        )
                    }
                }

                val leadingNulls = remember(pageGrid) {
                    pageGrid.indexOfFirst { it != null }.let { if (it < 0) 0 else it }
                }
                val currentMonthDays = remember(pYear, pMonth) {
                    BsCalendarData.getDaysInMonth(pYear, pMonth)
                }
                val firstAdOfMonth = remember(pYear, pMonth) {
                    BsDateConverter.bsToAd(pYear, pMonth, 1)
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    val weeks = pageGrid.chunked(7)
                    weeks.forEachIndexed { weekIdx, week ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                        ) {
                            AsciiVerticalPipe()
                            week.forEachIndexed { colIdx, cell ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                ) {
                                    if (cell == null) {
                                        val flatIdx = weekIdx * 7 + colIdx
                                        var neighborBs: BsDate? = null
                                        var neighborAd: LocalDate? = null
                                        if (flatIdx < leadingNulls) {
                                            val prevY = if (pMonth == 1) pYear - 1 else pYear
                                            val prevM = if (pMonth == 1) 12 else pMonth - 1
                                            if (prevY >= BsCalendarData.MIN_YEAR) {
                                                val delta = leadingNulls - flatIdx
                                                val prevDays = BsCalendarData.getDaysInMonth(prevY, prevM)
                                                neighborBs = BsDate(prevY, prevM, (prevDays - delta + 1).coerceIn(1, prevDays))
                                                neighborAd = firstAdOfMonth.minusDays(delta.toLong())
                                            }
                                        } else {
                                            val nextY = if (pMonth == 12) pYear + 1 else pYear
                                            val nextM = if (pMonth == 12) 1 else pMonth + 1
                                            if (nextY <= BsCalendarData.MAX_YEAR) {
                                                val offset = flatIdx - (leadingNulls + currentMonthDays) + 1
                                                val nextDays = BsCalendarData.getDaysInMonth(nextY, nextM)
                                                neighborBs = BsDate(nextY, nextM, offset.coerceIn(1, nextDays))
                                                neighborAd = firstAdOfMonth.plusDays((currentMonthDays - 1 + offset).toLong())
                                            }
                                        }
                                        EmptyDayCell(
                                            neighborBs = neighborBs,
                                            neighborAd = neighborAd,
                                            isSaturday = (colIdx == 6),
                                            showAd = state.settings.showGregorianInGrid,
                                            useDevanagari = calNe,
                                            useDevanagariAd = adNe,
                                            onClick = { target ->
                                                haptics(MitiHaptic.TICK)
                                                onJumpToDate(target)
                                            }
                                        )
                                    } else {
                                        val dayEvents = pageEvents[cell.bsDate.day].orEmpty()
                                        val isSelected = cell.bsDate == state.selectedBsDate
                                        BsDayCell(
                                            day = cell,
                                            events = dayEvents,
                                            isSelected = isSelected,
                                            showTithi = state.settings.showTithiInGrid,
                                            showAd = state.settings.showGregorianInGrid,
                                            showFestivals = state.settings.showPublicFestivals,
                                            useDevanagari = calNe,
                                            useDevanagariAd = adNe,
                                            onClick = {
                                                haptics(MitiHaptic.TICK)
                                                onSelectDate(cell.bsDate)
                                            },
                                            onLongClick = {
                                                haptics(MitiHaptic.LONG_PRESS)
                                                if (state.selectedBsDate != cell.bsDate) {
                                                    onSelectDate(cell.bsDate)
                                                }
                                                editingEvent = null
                                                showEventDialog = true
                                            }
                                        )
                                    }
                                }
                                AsciiVerticalPipe()
                            }
                        }
                        AsciiTableHorizontalBorder(columns = 7)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

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
            useDevanagariAd = adNe,
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
            onSelectDate = onSelectDate,
            onJumpToDate = onJumpToDate
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showEventDialog) {
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
private fun EmptyDayCell(
    neighborBs: BsDate?,
    neighborAd: LocalDate?,
    isSaturday: Boolean,
    showAd: Boolean,
    useDevanagari: Boolean,
    useDevanagariAd: Boolean,
    onClick: (BsDate) -> Unit
) {
    val palette = LocalMitiPalette.current
    val baseBg = if (isSaturday) {
        palette.red.copy(alpha = if (palette.isDark) 0.04f else 0.025f)
    } else {
        palette.background
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(baseBg)
            .then(
                if (neighborBs != null) {
                    Modifier.clickable { onClick(neighborBs) }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 3.dp, vertical = 3.dp)
    ) {
        if (neighborBs != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = BsCalendarData.formatNumber(neighborBs.day, useDevanagari),
                    color = (if (isSaturday) palette.red else palette.foreground).copy(
                        alpha = if (palette.isDark) 0.36f else 0.34f
                    ),
                    fontFamily = MitiMonospaceFont,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    lineHeight = if (useDevanagari) 18.sp else 15.sp,
                    maxLines = 1
                )
                if (showAd && neighborAd != null) {
                    Text(
                        text = BsCalendarData.formatNumber(neighborAd.dayOfMonth, useDevanagariAd),
                        color = palette.foreground.copy(alpha = if (palette.isDark) 0.28f else 0.26f),
                        fontFamily = MitiMonospaceFont,
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
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
    useDevanagariAd: Boolean,
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
        day.isToday -> palette.headerBar
        isHoliday -> palette.red.copy(alpha = if (palette.isDark) 0.08f else 0.055f)
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
        CellArtworkMotif.SHRADDHA -> palette.pink
        CellArtworkMotif.BIRTHDAY -> primaryEventColor ?: palette.cyan
        else -> if (isHoliday) palette.red else palette.orange
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(bgColor)
            .then(
                if (isSelected) Modifier.border(1.5.dp, palette.purple)
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = formattedDayText,
                    color = dayNumberColor,
                    fontFamily = MitiMonospaceFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    lineHeight = if (useDevanagari) 19.sp else 16.sp,
                    maxLines = 1
                )
                if (showAd) {
                    val adTextColor = if (isSelected) {
                        palette.foreground.copy(alpha = if (palette.isDark) 0.80f else 0.75f)
                    } else {
                        palette.comment
                    }
                    Text(
                        text = BsCalendarData.formatNumber(day.adDate.dayOfMonth, useDevanagariAd),
                        color = adTextColor,
                        fontFamily = MitiMonospaceFont,
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        maxLines = 1
                    )
                }
            }

            if (events.isNotEmpty()) {
                val eventMarkerColor = primaryEventColor ?: palette.green
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(eventMarkerColor)
                    )
                    if (showFestivals && day.festivals.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(if (hasFestivalHoliday) palette.red else palette.orange)
                        )
                    }
                    Text(
                        text = events.first().event.title,
                        color = eventMarkerColor,
                        fontFamily = MitiMonospaceFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.5.sp,
                        lineHeight = if (useDevanagari) 13.sp else 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else if (showFestivals && day.festivals.isNotEmpty()) {
                val f = day.festivals.first()
                val fTitle = if (useDevanagari) f.nepaliTitle else f.englishTitle
                Text(
                    text = fTitle,
                    color = if (f.isPublicHoliday) palette.red else palette.orange,
                    fontFamily = MitiMonospaceFont,
                    fontWeight = FontWeight.Medium,
                    fontSize = 9.5.sp,
                    lineHeight = if (useDevanagari) 13.sp else 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (showTithi) {
                val tithiColor = when {
                    day.tithi.isPurnima -> palette.yellow
                    day.tithi.isAunsi -> palette.purple
                    day.tithi.isEkadashi -> palette.green
                    isSelected -> palette.foreground.copy(alpha = if (palette.isDark) 0.82f else 0.78f)
                    else -> palette.comment
                }
                Text(
                    text = day.tithi.gridLabel(useDevanagari),
                    color = tithiColor,
                    fontFamily = MitiMonospaceFont,
                    fontWeight = if (day.tithi.isPurnima || day.tithi.isAunsi || day.tithi.isEkadashi) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Normal
                    },
                    fontSize = 9.5.sp,
                    lineHeight = if (useDevanagari) 13.sp else 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MinimalTodayStrip(
    day: BsCalendarDay,
    todayAd: LocalDate,
    isSelectedCell: Boolean,
    events: List<ResolvedEventInstance>,
    showFestivals: Boolean,
    useDevanagari: Boolean,
    useDevanagariAd: Boolean,
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
    val totalDaysInMonth = BsCalendarData.daysInMonth(day.bsDate.year, day.bsDate.month)
    val monthBoundaryMarker = when (day.bsDate.day) {
        1 -> " • ${AppStrings.sankrantiTag(ne)}"
        totalDaysInMonth -> " • ${AppStrings.masantaTag(ne)}"
        else -> ""
    }
    val adDisplay = BsCalendarData.formatAdDate(day.adDate, useDevanagariAd)
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
        val tithiSpanDisplay = remember(day.adDate, day.tithi.number, ne) {
            TithiCalculator.calculateTithiTimeSpan(day.adDate, day.tithi.number)
                .formatDisplay(day.adDate, ne)
        }
        var showPanchanga by remember { mutableStateOf(false) }
        val panchanga = remember(day.adDate, day.tithi, showPanchanga) {
            if (showPanchanga) TithiCalculator.calculateDailyPanchanga(day.adDate, day.tithi) else null
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AsciiLunarPhaseIcon(tithiNumber = day.tithi.number)
                    Text(
                        text = day.tithi.stripDisplayWithoutSymbol(ne),
                        color = palette.foreground,
                        fontFamily = MitiMonospaceFont,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        lineHeight = if (ne) 19.sp else 16.sp
                    )
                }
                Text(
                    text = tithiSpanDisplay,
                    color = palette.cyan,
                    fontFamily = MitiMonospaceFont,
                    fontSize = 11.5.sp,
                    lineHeight = if (ne) 16.sp else 14.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "$adDisplay$monthBoundaryMarker",
                        color = palette.comment,
                        fontFamily = MitiMonospaceFont,
                        fontSize = 12.sp,
                        lineHeight = if (ne) 17.sp else 15.sp
                    )
                    BracketButton(
                        label = "${AppStrings.panchangaBtn(ne)} ${if (showPanchanga) "▴" else "▾"}",
                        onClick = { showPanchanga = !showPanchanga },
                        textColor = palette.purple,
                        compact = true
                    )
                }
            }
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

        if (panchanga != null) {
            Spacer(modifier = Modifier.height(4.dp))
            AsciiHorizontalRule()
            Spacer(modifier = Modifier.height(3.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "${AppStrings.sunriseSunsetLabel(ne)} ${panchanga.formatSunriseSunset(ne)} NST",
                    color = palette.orange,
                    fontFamily = MitiMonospaceFont,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp
                )
                Text(
                    text = "${AppStrings.nakshatraLabel(ne)} ${if (ne) panchanga.nakshatraNe else panchanga.nakshatraEn}",
                    color = palette.cyan,
                    fontFamily = MitiMonospaceFont,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp
                )
                Text(
                    text = "${AppStrings.nepalSambatLabel(ne)} ${panchanga.formatNepalSambat(ne)}",
                    color = palette.purple,
                    fontFamily = MitiMonospaceFont,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp
                )
            }
        }

        if (showFestivals && day.festivals.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            day.festivals.forEach { fest ->
                val title = if (ne) fest.nepaliTitle else fest.englishTitle
                Text(
                    text = "* $title",
                    color = if (fest.isPublicHoliday) palette.red else palette.orange,
                    fontFamily = MitiMonospaceFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    lineHeight = if (ne) 19.sp else 16.sp
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
                    val amantaMonth = BsDateConverter.getShraddhaLunarMonth(instance.event)
                    val purnimantaMonth = TithiCalculator.amantaToPurnimantaMonth(
                        amantaMonth,
                        instance.event.shraddhaTithi
                    )
                    val mName = BsCalendarData.getMonthName(purnimantaMonth, ne)
                    if (ne) {
                        "[${AppStrings.shraddhaTag(true)}: $mName ${tInfo.paksha.nepaliName} ${tInfo.nepaliName}] "
                    } else {
                        "[${AppStrings.shraddhaTag(false)}: $mName ${tInfo.paksha.shortCode}-${tInfo.shortLabelEn}] "
                    }
                } else ""
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .background(palette.background)
                        .border(1.dp, evColor)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    val repeatTag = if (!instance.event.isRepeating) " [${AppStrings.oneTimeTag(ne)}]" else ""
                    Text(
                        text = "● ${instance.event.title}$repeatTag $evBadge",
                        color = evColor,
                        fontFamily = MitiMonospaceFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        lineHeight = if (ne) 20.sp else 18.sp
                    )
                    if (shraddhaPrefix.isNotEmpty() || instance.event.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$shraddhaPrefix${instance.event.notes}".trim(),
                            color = palette.comment,
                            fontFamily = MitiMonospaceFont,
                            fontSize = 12.sp,
                            lineHeight = if (ne) 17.sp else 15.sp
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End)
                    ) {
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
                            textColor = palette.red,
                            haptic = MitiHaptic.CONFIRM
                        )
                    }
                }
            }
        }
    }
}

private enum class ScheduleSearchFilter {
    ALL,
    EVENTS,
    HOLIDAYS
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
    onSelectDate: (BsDate) -> Unit,
    onJumpToDate: (BsDate) -> Unit
) {
    val palette = LocalMitiPalette.current
    val haptics = rememberMitiHaptics()
    val ne = useDevanagari
    val monthName = BsCalendarData.monthName(month, ne)
    val yearStr = BsCalendarData.formatNumber(year, ne)

    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchFilter by remember { mutableStateOf(ScheduleSearchFilter.ALL) }
    var showMyEventsAgenda by remember { mutableStateOf(false) }

    val boxHeader = if (showMyEventsAgenda && !showSearch) {
        AppStrings.myEventsHeader(ne)
    } else {
        AppStrings.monthScheduleHeader(ne, monthName, yearStr)
    }

    PlaintextBox(
        header = boxHeader,
        headerColor = palette.purple,
        trailingHeader = {
            if (showSearch) {
                BracketButton(
                    label = "X",
                    onClick = {
                        searchQuery = ""
                        searchFilter = ScheduleSearchFilter.ALL
                        showSearch = false
                    },
                    textColor = palette.red
                )
            } else {
                BracketButton(
                    label = "?",
                    onClick = { showSearch = true },
                    textColor = palette.cyan
                )
            }
        }
    ) {
        if (showSearch) {
            PlaintextTextInput(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = AppStrings.searchPlaceholder(ne),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                BracketButton(
                    label = AppStrings.filterAll(ne),
                    highlighted = searchFilter == ScheduleSearchFilter.ALL,
                    onClick = { searchFilter = ScheduleSearchFilter.ALL },
                    textColor = if (searchFilter == ScheduleSearchFilter.ALL) palette.cyan else palette.comment,
                    compact = true
                )
                BracketButton(
                    label = AppStrings.filterEvents(ne),
                    highlighted = searchFilter == ScheduleSearchFilter.EVENTS,
                    onClick = { searchFilter = ScheduleSearchFilter.EVENTS },
                    textColor = if (searchFilter == ScheduleSearchFilter.EVENTS) palette.green else palette.comment,
                    compact = true
                )
                BracketButton(
                    label = AppStrings.filterHolidays(ne),
                    highlighted = searchFilter == ScheduleSearchFilter.HOLIDAYS,
                    onClick = { searchFilter = ScheduleSearchFilter.HOLIDAYS },
                    textColor = if (searchFilter == ScheduleSearchFilter.HOLIDAYS) palette.red else palette.comment,
                    compact = true
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BracketButton(
                    label = AppStrings.monthTabBtn(ne),
                    highlighted = !showMyEventsAgenda,
                    onClick = { showMyEventsAgenda = false },
                    textColor = if (!showMyEventsAgenda) palette.cyan else palette.comment,
                    compact = true
                )
                val myCount = BsCalendarData.formatNumber(allEvents.size, ne)
                BracketButton(
                    label = "${AppStrings.myEventsTabBtn(ne)} ($myCount)",
                    highlighted = showMyEventsAgenda,
                    onClick = { showMyEventsAgenda = true },
                    textColor = if (showMyEventsAgenda) palette.green else palette.comment,
                    compact = true
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        val trimmedQuery = searchQuery.trim()
        val pastGray = palette.comment.copy(alpha = 0.55f)
        val shouldRunSearch = showSearch && (trimmedQuery.isNotEmpty() || searchFilter != ScheduleSearchFilter.ALL)

        if (shouldRunSearch) {
            val searchMatches = remember(trimmedQuery, searchFilter, year, allEvents, showFestivals, ne, todayAd) {
                val results = mutableListOf<YearSearchItem>()
                val includeEvents = searchFilter != ScheduleSearchFilter.HOLIDAYS
                val includeFestivals = showFestivals && searchFilter != ScheduleSearchFilter.EVENTS
                val onlyPublicHolidays = searchFilter == ScheduleSearchFilter.HOLIDAYS

                if (includeEvents || onlyPublicHolidays) {
                    for (ev in allEvents) {
                        if (onlyPublicHolidays && !ev.isPersonalHoliday) continue
                        if (year < ev.startBsYear) continue
                        val matchesText = trimmedQuery.isEmpty() ||
                            ev.title.contains(trimmedQuery, ignoreCase = true) ||
                            ev.note.contains(trimmedQuery, ignoreCase = true) ||
                            (ev.isShraddha && ("shraddha".contains(trimmedQuery, ignoreCase = true) || "श्राद्ध".contains(trimmedQuery)))
                        if (!matchesText) continue

                        val occurrences = BsDateConverter.resolveEventOccurrencesInBsYear(ev, year, missingDayStrategy)
                        for (occ in occurrences) {
                            val ad = BsDateConverter.bsToAd(occ.targetDate)
                            val daysDiff = ChronoUnit.DAYS.between(todayAd, ad)
                            val yrs = (year - ev.startBsYear).coerceAtLeast(0)
                            val mShort = BsCalendarData.getShortMonthName(occ.targetDate.month, ne)
                            val dPad = BsCalendarData.formatNumberPadded(occ.targetDate.day, ne)
                            val shraddhaTag = if (ev.isShraddha) " (${AppStrings.shraddhaTag(ne)})" else ""
                            val onceTag = if (!ev.isRepeating) " [${AppStrings.oneTimeTag(ne)}]" else ""
                            results.add(
                                YearSearchItem(
                                    bsDate = occ.targetDate,
                                    adDate = ad,
                                    label = "$mShort $dPad | ● ${ev.title}$shraddhaTag$onceTag",
                                    badge = AppStrings.formatYearAndCountdownBadge(daysDiff, yrs, ne),
                                    color = ev.resolvedColor.composeColor(palette),
                                    isPast = occ.targetDate < todayBs,
                                    isToday = occ.targetDate == todayBs
                                )
                            )
                        }
                    }
                }

                if (includeFestivals) {
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
                                if (onlyPublicHolidays && !f.isPublicHoliday) continue
                                val matchesText = trimmedQuery.isEmpty() ||
                                    f.englishTitle.contains(trimmedQuery, ignoreCase = true) ||
                                    f.nepaliTitle.contains(trimmedQuery, ignoreCase = true)
                                if (matchesText) {
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
                    fontFamily = MitiMonospaceFont,
                    fontSize = 13.sp
                )
            } else {
                searchMatches.forEach { item ->
                    val itemColor = if (item.isPast) pastGray else item.color
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptics(MitiHaptic.TICK)
                                onJumpToDate(item.bsDate)
                                showSearch = false
                                searchQuery = ""
                                searchFilter = ScheduleSearchFilter.ALL
                            }
                            .padding(horizontal = 2.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${item.label} ${item.badge}",
                            color = itemColor,
                            fontFamily = MitiMonospaceFont,
                            fontWeight = if (item.isToday || !item.isPast) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        } else if (!showSearch && showMyEventsAgenda) {
            val upcomingItems = remember(allEvents, todayBs, todayAd, missingDayStrategy, ne) {
                val list = mutableListOf<YearSearchItem>()
                val candidateYears = listOf(todayBs.year, (todayBs.year + 1).coerceAtMost(BsCalendarData.MAX_YEAR)).distinct()
                for (ev in allEvents) {
                    var picked: Pair<BsDate, LocalDate>? = null
                    if (!ev.isRepeating) {
                        val occ = BsDateConverter.resolveEventOccurrence(ev, ev.startBsYear, missingDayStrategy)
                        if (occ != null) {
                            picked = occ.targetDate to BsDateConverter.bsToAd(occ.targetDate)
                        }
                    } else {
                        for (candY in candidateYears) {
                            if (candY < ev.startBsYear) continue
                            val occs = BsDateConverter.resolveEventOccurrencesInBsYear(ev, candY, missingDayStrategy)
                            val future = occs.firstOrNull { it.targetDate >= todayBs }
                            if (future != null) {
                                picked = future.targetDate to BsDateConverter.bsToAd(future.targetDate)
                                break
                            }
                        }
                    }
                    if (picked != null) {
                        val (bs, ad) = picked
                        val daysDiff = ChronoUnit.DAYS.between(todayAd, ad)
                        val yrs = (bs.year - ev.startBsYear).coerceAtLeast(0)
                        val mShort = BsCalendarData.getShortMonthName(bs.month, ne)
                        val dPad = BsCalendarData.formatNumberPadded(bs.day, ne)
                        val yStr = BsCalendarData.formatNumber(bs.year, ne)
                        val shraddhaTag = if (ev.isShraddha) " (${AppStrings.shraddhaTag(ne)})" else ""
                        val onceTag = if (!ev.isRepeating) " [${AppStrings.oneTimeTag(ne)}]" else ""
                        list.add(
                            YearSearchItem(
                                bsDate = bs,
                                adDate = ad,
                                label = "$yStr $mShort $dPad | ● ${ev.title}$shraddhaTag$onceTag",
                                badge = AppStrings.formatYearAndCountdownBadge(daysDiff, yrs, ne),
                                color = ev.resolvedColor.composeColor(palette),
                                isPast = bs < todayBs,
                                isToday = bs == todayBs
                            )
                        )
                    }
                }
                list.sortedWith(compareBy<YearSearchItem> { it.isPast }.thenBy { it.bsDate })
            }

            if (upcomingItems.isEmpty()) {
                Text(
                    text = AppStrings.noSavedEvents(ne),
                    color = palette.comment,
                    fontFamily = MitiMonospaceFont,
                    fontSize = 13.sp
                )
            } else {
                upcomingItems.forEach { item ->
                    val itemColor = if (item.isPast) pastGray else item.color
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptics(MitiHaptic.TICK)
                                onJumpToDate(item.bsDate)
                            }
                            .padding(horizontal = 2.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${item.label} ${item.badge}",
                            color = itemColor,
                            fontFamily = MitiMonospaceFont,
                            fontWeight = if (item.isToday || !item.isPast) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.5.sp,
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
                    fontFamily = MitiMonospaceFont,
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
                            .clickable {
                                haptics(MitiHaptic.TICK)
                                onSelectDate(d.bsDate)
                            }
                            .padding(horizontal = 6.dp, vertical = 5.dp)
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptics(MitiHaptic.TICK)
                                onSelectDate(d.bsDate)
                            }
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
                                fontFamily = MitiMonospaceFont,
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
                                    fontFamily = MitiMonospaceFont,
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
