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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import np.com.miti.patro.data.preferences.UserPreferences
import np.com.miti.patro.domain.engine.BsCalendarData
import np.com.miti.patro.domain.model.BsDate
import np.com.miti.patro.domain.model.BsEvent
import np.com.miti.patro.domain.model.DayCellInfo
import np.com.miti.patro.domain.model.MonthGridState
import np.com.miti.patro.domain.model.ResolvedBsEvent
import np.com.miti.patro.ui.components.AsciiHorizontalRule
import np.com.miti.patro.ui.components.AsciiTableHorizontalBorder
import np.com.miti.patro.ui.components.PlaintextBox
import np.com.miti.patro.ui.components.PlaintextBracketButton
import np.com.miti.patro.ui.theme.AppStrings
import np.com.miti.patro.ui.theme.LocalMitiPalette
import java.time.LocalDate

/**
 * Main Bikram Sambat Calendar Screen.
 * Inspired by https://plaintextsports.com/:
 * - Strict monospace ASCII table with literal `+-------+` horizontal borders and `|` column borders.
 * - Selected date rendered as `[09]`, today as `*09*`.
 * - Zero app name or theme badge on the calendar screen.
 * - Full Nepali language localization across every single UI element when selected.
 */
@Composable
fun CalendarScreen(
    uiState: CalendarUiState,
    onSelectDate: (BsDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onJumpToToday: () -> Unit,
    onOpenYearMonthPicker: () -> Unit,
    onOpenAddEvent: () -> Unit,
    onOpenEditEvent: (BsEvent) -> Unit,
    onDeleteEvent: (BsEvent) -> Unit,
    onExportEventToGCal: (ResolvedBsEvent, LocalDate) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalMitiPalette.current
    val prefs = uiState.preferences
    val ne = prefs.useNepaliScript
    val grid = uiState.monthGrid
    val scrollState = rememberScrollState()

    val selectedCell = grid.days.firstOrNull { it.bsDate == uiState.selectedBsDate }
        ?: grid.days.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        // 1. Top Navigation Bar (Bracketed plaintext controls, no app title, no theme badge)
        AsciiHorizontalRule()
        TopPlaintextNavBar(
            bsYear = grid.bsYear,
            bsMonth = grid.bsMonth,
            adSpanLabel = grid.adMonthSpanLabel,
            nepaliScript = ne,
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth,
            onJumpToToday = onJumpToToday,
            onOpenYearMonthPicker = onOpenYearMonthPicker,
            onOpenSettings = onOpenSettings
        )
        AsciiTableHorizontalBorder(columns = 7)

        // 2. Weekday Column Header Row framed with `|` dividers and `-------` border
        WeekdayHeaderRow(nepaliScript = ne)
        AsciiTableHorizontalBorder(columns = 7)

        // 3. Monthly BS Calendar Table (7 columns x N weeks with literal `-------` & `|` borders)
        MonthGridTable(
            grid = grid,
            selectedBsDate = uiState.selectedBsDate,
            preferences = prefs,
            onSelectDate = onSelectDate
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Selected Day Details + Tithi + Holidays + Repeating BS Events
        if (selectedCell != null) {
            SelectedDayDetailPanel(
                cell = selectedCell,
                nepaliScript = ne,
                onAddEvent = onOpenAddEvent,
                onEditEvent = onOpenEditEvent,
                onDeleteEvent = onDeleteEvent,
                onExportEventToGCal = onExportEventToGCal
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 5. Monthly Holidays & Festivals Summary Box
        MonthHolidaysSummaryBox(
            grid = grid,
            nepaliScript = ne,
            onSelectDate = onSelectDate
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun TopPlaintextNavBar(
    bsYear: Int,
    bsMonth: Int,
    adSpanLabel: String,
    nepaliScript: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onJumpToToday: () -> Unit,
    onOpenYearMonthPicker: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val palette = LocalMitiPalette.current
    val monthName = BsCalendarData.getMonthName(bsMonth, nepaliScript)
    val yearLabel = BsCalendarData.formatNumber(bsYear, nepaliScript)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.surfaceElevated)
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlaintextBracketButton(
                label = AppStrings.prevBtn(nepaliScript),
                onClick = onPreviousMonth,
                textColor = palette.accentCyan
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable(onClick = onOpenYearMonthPicker)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (nepaliScript) "[$monthName $yearLabel v]" else "[${monthName.uppercase()} $yearLabel v]",
                    color = palette.foreground,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = adSpanLabel,
                    color = palette.comment,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }

            PlaintextBracketButton(
                label = AppStrings.nextBtn(nepaliScript),
                onClick = onNextMonth,
                textColor = palette.accentCyan
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlaintextBracketButton(
                label = AppStrings.todayBtn(nepaliScript),
                onClick = onJumpToToday,
                textColor = palette.accentGreen
            )
            PlaintextBracketButton(
                label = AppStrings.settingsBtn(nepaliScript),
                onClick = onOpenSettings,
                textColor = palette.comment
            )
        }
    }
}

@Composable
private fun WeekdayHeaderRow(nepaliScript: Boolean) {
    val palette = LocalMitiPalette.current
    val headers = if (nepaliScript) BsCalendarData.WEEKDAYS_NE else BsCalendarData.WEEKDAYS_EN

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.surfaceElevated)
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        headers.forEachIndexed { index, label ->
            val isSaturday = index == 6
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "|",
                    color = palette.gridBorder,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
                Text(
                    text = label,
                    color = if (isSaturday) palette.holidayRed else palette.comment,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Text(
            text = "|",
            color = palette.gridBorder,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun MonthGridTable(
    grid: MonthGridState,
    selectedBsDate: BsDate,
    preferences: UserPreferences,
    onSelectDate: (BsDate) -> Unit
) {
    val palette = LocalMitiPalette.current
    val totalSlots = grid.leadingEmptyCells + grid.totalDays
    val rowCount = (totalSlots + 6) / 7

    Column(modifier = Modifier.fillMaxWidth()) {
        for (row in 0 until rowCount) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (col in 0 until 7) {
                    val slotIndex = row * 7 + col
                    val dayNumber = slotIndex - grid.leadingEmptyCells + 1
                    val cellInfo = if (dayNumber in 1..grid.totalDays) {
                        grid.days[dayNumber - 1]
                    } else {
                        null
                    }

                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left ASCII column pipe `|`
                        Text(
                            text = "|",
                            color = palette.gridBorder,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 12.sp
                        )
                        DayGridCell(
                            cell = cellInfo,
                            isSelected = cellInfo?.bsDate == selectedBsDate,
                            preferences = preferences,
                            onSelectDate = onSelectDate,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                // Far-right closing ASCII column pipe `|`
                Text(
                    text = "|",
                    color = palette.gridBorder,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }
            // Literal `+-------------+` ASCII border line below every week row
            AsciiTableHorizontalBorder(columns = 7)
        }
    }
}

@Composable
private fun DayGridCell(
    cell: DayCellInfo?,
    isSelected: Boolean,
    preferences: UserPreferences,
    onSelectDate: (BsDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalMitiPalette.current
    val ne = preferences.useNepaliScript

    if (cell == null) {
        Box(
            modifier = modifier
                .height(62.dp)
                .background(palette.background)
        )
        return
    }

    val isPublicHoliday = cell.isSaturday || cell.holidays.any { it.isPublicHoliday }
    val hasFestival = cell.holidays.isNotEmpty()
    val hasUserEvent = cell.events.isNotEmpty()

    val bgColor = when {
        isSelected -> palette.selectedCellBg
        cell.isToday -> palette.todayCellBg
        else -> palette.surface
    }

    val dayNumberColor = when {
        isPublicHoliday -> palette.holidayRed
        hasFestival -> palette.festivalOrange
        cell.isToday -> palette.accentGreen
        else -> palette.foreground
    }

    val rawDayNum = BsCalendarData.formatNumber(cell.bsDate.day, ne).padStart(2, if (ne) '०' else '0')
    val dayDisplay = when {
        isSelected -> "[$rawDayNum]"
        cell.isToday -> "*$rawDayNum*"
        else -> rawDayNum
    }

    Column(
        modifier = modifier
            .height(62.dp)
            .background(bgColor)
            .clickable { onSelectDate(cell.bsDate) }
            .padding(horizontal = 2.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top row: BS Day number (bracketed if selected) + AD sub-date
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = dayDisplay,
                color = dayNumberColor,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = if (isSelected || cell.isToday) 11.sp else 13.sp,
                lineHeight = 13.sp,
                maxLines = 1
            )
            if (preferences.showGregorianSubDate) {
                Text(
                    text = BsCalendarData.formatNumber(cell.adDate.dayOfMonth, ne),
                    color = palette.comment,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    lineHeight = 9.sp,
                    maxLines = 1
                )
            }
        }

        // Middle row: Plaintext markers (`*` for festival/holiday, `+N` for repeating BS event)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasFestival) {
                Text(
                    text = "*",
                    color = if (isPublicHoliday) palette.holidayRed else palette.festivalOrange,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    lineHeight = 10.sp
                )
            }
            if (hasUserEvent) {
                val countLabel = BsCalendarData.formatNumber(cell.events.size, ne)
                Text(
                    text = "+$countLabel",
                    color = palette.accentCyan,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    lineHeight = 10.sp
                )
            }
        }

        // Bottom row: Daily Tithi short label (Aunsi through Purnima)
        if (preferences.showTithiInGrid) {
            val tithiColor = when {
                cell.tithi.isPurnima -> palette.purnimaYellow
                cell.tithi.isAunsi -> palette.aunsiPink
                else -> palette.tithiPurple
            }
            Text(
                text = if (ne) cell.tithi.nepaliName else cell.tithi.shortLabel,
                color = tithiColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp,
                lineHeight = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SelectedDayDetailPanel(
    cell: DayCellInfo,
    nepaliScript: Boolean,
    onAddEvent: () -> Unit,
    onEditEvent: (BsEvent) -> Unit,
    onDeleteEvent: (BsEvent) -> Unit,
    onExportEventToGCal: (ResolvedBsEvent, LocalDate) -> Unit
) {
    val palette = LocalMitiPalette.current
    val bsMonthName = BsCalendarData.getMonthName(cell.bsDate.month, nepaliScript)
    val bsDayStr = BsCalendarData.formatNumber(cell.bsDate.day, nepaliScript)
    val bsYearStr = BsCalendarData.formatNumber(cell.bsDate.year, nepaliScript)
    val weekdayName = if (nepaliScript) {
        BsCalendarData.WEEKDAYS_FULL_NE[cell.dayOfWeekIndex]
    } else {
        BsCalendarData.WEEKDAYS_FULL_EN[cell.dayOfWeekIndex]
    }
    val adDateFormatted = BsCalendarData.formatAdDate(cell.adDate, nepaliScript)

    PlaintextBox(
        headerTitle = if (nepaliScript) {
            "$bsYearStr $bsMonthName $bsDayStr ($weekdayName)"
        } else {
            "${bsMonthName.uppercase()} $bsDayStr, $bsYearStr BS ($weekdayName)"
        },
        headerRight = "$adDateFormatted AD"
    ) {
        // 1. Lunar Tithi & Paksha line
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tithiDisplay = if (nepaliScript) {
                "तिथि: ${cell.tithi.nepaliName} (${cell.tithi.paksha.nepaliName})"
            } else {
                "TITHI: ${cell.tithi.englishName} (${cell.tithi.paksha.englishName})"
            }
            val tithiMarker = when {
                cell.tithi.isPurnima -> if (nepaliScript) " [पूर्णिमा ●]" else " [FULL MOON ●]"
                cell.tithi.isAunsi -> if (nepaliScript) " [औंसी ○]" else " [NEW MOON ○]"
                else -> ""
            }
            Text(
                text = tithiDisplay + tithiMarker,
                color = palette.tithiPurple,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
            if (cell.isSaturday) {
                Text(
                    text = AppStrings.saturdayHolidayBadge(nepaliScript),
                    color = palette.holidayRed,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }

        // 2. Holidays & Festivals on this day
        if (cell.holidays.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            cell.holidays.forEach { holiday ->
                val tag = if (holiday.isPublicHoliday) {
                    AppStrings.publicHolidayTag(nepaliScript)
                } else {
                    AppStrings.festivalTag(nepaliScript)
                }
                val title = if (nepaliScript) holiday.titleNe else holiday.titleEn
                Text(
                    text = "$tag $title",
                    color = if (holiday.isPublicHoliday) palette.holidayRed else palette.festivalOrange,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        AsciiHorizontalRule()
        Spacer(modifier = Modifier.height(6.dp))

        // 3. Repeating BS Events on this day
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = AppStrings.repeatingEventsHeader(nepaliScript),
                color = palette.comment,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                modifier = Modifier.weight(1f)
            )
            PlaintextBracketButton(
                label = AppStrings.addRepeatingBsEventBtn(nepaliScript),
                onClick = onAddEvent,
                textColor = palette.accentGreen
            )
        }

        if (cell.events.isEmpty()) {
            Text(
                text = AppStrings.noRepeatingEventsMsg(nepaliScript),
                color = palette.comment,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(4.dp))
            cell.events.forEach { resolved ->
                val ev = resolved.event
                val originMonthName = BsCalendarData.getMonthName(ev.bsMonth, nepaliScript)
                val originDayStr = BsCalendarData.formatNumber(ev.bsDay, nepaliScript)
                val resolvedDayStr = BsCalendarData.formatNumber(resolved.resolvedDate.day, nepaliScript)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(palette.surfaceElevated)
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "+ ${ev.title}",
                            color = palette.accentCyan,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            PlaintextBracketButton(
                                label = AppStrings.exportGCalBtn(nepaliScript),
                                onClick = { onExportEventToGCal(resolved, cell.adDate) },
                                textColor = palette.accentGreen
                            )
                            PlaintextBracketButton(
                                label = AppStrings.editBtn(nepaliScript),
                                onClick = { onEditEvent(ev) },
                                textColor = palette.accentPurple
                            )
                            PlaintextBracketButton(
                                label = AppStrings.deleteBtn(nepaliScript),
                                onClick = { onDeleteEvent(ev) },
                                textColor = palette.holidayRed
                            )
                        }
                    }

                    val ruleInfo = buildString {
                        append(AppStrings.repeatsAnnuallyEvery(nepaliScript, originMonthName, originDayStr))
                        if (resolved.isClampedOrRolled) {
                            append(" ")
                            append(AppStrings.clampedDayNote(nepaliScript, originDayStr, resolvedDayStr))
                        }
                    }
                    Text(
                        text = ruleInfo,
                        color = palette.comment,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )

                    val timeStr = BsCalendarData.formatTime24h(ev.reminderHour, ev.reminderMinute, nepaliScript)
                    Text(
                        text = AppStrings.alarmInfo(
                            ne = nepaliScript,
                            enabled = ev.reminderEnabled,
                            timeStr = timeStr,
                            strategyLabel = ev.missingDayStrategy.label(nepaliScript)
                        ),
                        color = palette.comment,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )

                    if (ev.notes.isNotBlank()) {
                        Text(
                            text = ev.notes,
                            color = palette.foreground,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    AsciiHorizontalRule()
                }
            }
        }
    }
}

@Composable
private fun MonthHolidaysSummaryBox(
    grid: MonthGridState,
    nepaliScript: Boolean,
    onSelectDate: (BsDate) -> Unit
) {
    val palette = LocalMitiPalette.current
    val monthName = BsCalendarData.getMonthName(grid.bsMonth, nepaliScript)
    val yearStr = BsCalendarData.formatNumber(grid.bsYear, nepaliScript)
    val holidayDays = grid.days.filter { it.holidays.isNotEmpty() }

    PlaintextBox(
        headerTitle = AppStrings.holidaysInMonthHeader(nepaliScript, monthName, yearStr),
        headerRight = BsCalendarData.formatNumber(holidayDays.size, nepaliScript)
    ) {
        if (holidayDays.isEmpty()) {
            Text(
                text = AppStrings.noFestivalsInMonthMsg(nepaliScript),
                color = palette.comment,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
        } else {
            holidayDays.forEach { cell ->
                val dayNum = BsCalendarData.formatNumber(cell.bsDate.day, nepaliScript)
                    .padStart(2, if (nepaliScript) '०' else '0')
                val tithiLabel = if (nepaliScript) cell.tithi.nepaliName else cell.tithi.englishName
                cell.holidays.forEach { h ->
                    val title = if (nepaliScript) h.titleNe else h.titleEn
                    val tag = if (h.isPublicHoliday) " ${AppStrings.holidayBadge(nepaliScript)}" else ""
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectDate(cell.bsDate) }
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$dayNum $monthName : $title$tag",
                            color = if (h.isPublicHoliday) palette.holidayRed else palette.festivalOrange,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = tithiLabel,
                            color = palette.comment,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
