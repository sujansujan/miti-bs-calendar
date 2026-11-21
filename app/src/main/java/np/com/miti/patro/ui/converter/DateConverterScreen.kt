package np.com.miti.patro.ui.converter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import np.com.miti.patro.domain.engine.BsCalendarData
import np.com.miti.patro.domain.engine.BsDateConverter
import np.com.miti.patro.domain.engine.NepaliFestivals
import np.com.miti.patro.domain.engine.TithiCalculator
import np.com.miti.patro.domain.model.BsDate
import np.com.miti.patro.ui.components.AsciiHorizontalRule
import np.com.miti.patro.ui.components.AsciiTableHorizontalBorder
import np.com.miti.patro.ui.components.BracketButton
import np.com.miti.patro.ui.components.MitiHaptic
import np.com.miti.patro.ui.components.PlaintextBox
import np.com.miti.patro.ui.components.rememberMitiHaptics
import np.com.miti.patro.ui.theme.AppStrings
import np.com.miti.patro.ui.theme.LocalMitiPalette
import np.com.miti.patro.ui.theme.MitiMonospaceFont
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

private enum class ConverterMode {
    BS_TO_AD,
    AD_TO_BS,
    AGE_DIFF
}

@Composable
fun DateConverterScreen(
    initialBsDate: BsDate,
    useDevanagari: Boolean,
    onBackToCalendar: () -> Unit,
    onOpenBsDateInCalendar: (BsDate) -> Unit,
    onAddEventOnBsDate: (BsDate) -> Unit
) {
    val palette = LocalMitiPalette.current
    val haptics = rememberMitiHaptics()
    val ne = useDevanagari
    val scrollState = rememberScrollState()

    var mode by remember { mutableStateOf(ConverterMode.BS_TO_AD) }
    var currentBsDate by remember { mutableStateOf(initialBsDate) }

    val todayBs = remember { BsDateConverter.todayBs() }
    var diffStartBs by remember {
        mutableStateOf(
            BsDate(
                year = (todayBs.year - 25).coerceIn(BsCalendarData.MIN_YEAR, BsCalendarData.MAX_YEAR),
                month = todayBs.month,
                day = todayBs.day.coerceAtMost(
                    BsCalendarData.daysInMonth(
                        (todayBs.year - 25).coerceIn(BsCalendarData.MIN_YEAR, BsCalendarData.MAX_YEAR),
                        todayBs.month
                    )
                )
            )
        )
    }
    var diffEndBs by remember { mutableStateOf(todayBs) }
    var editingDiffStart by remember { mutableStateOf(true) }

    val currentAdDate = remember(currentBsDate) { BsDateConverter.bsToAd(currentBsDate) }
    val tithi = remember(currentAdDate) { TithiCalculator.calculateTithiAtSunrise(currentAdDate) }
    val festivals = remember(currentBsDate, tithi) {
        NepaliFestivals.getFestivalsForDay(currentBsDate, tithi)
    }

    fun clampBs(year: Int, month: Int, day: Int): BsDate {
        val safeY = year.coerceIn(BsCalendarData.MIN_YEAR, BsCalendarData.MAX_YEAR)
        val safeM = month.coerceIn(1, 12)
        val maxD = BsCalendarData.daysInMonth(safeY, safeM)
        return BsDate(safeY, safeM, day.coerceIn(1, maxD))
    }

    fun updateFromBs(year: Int, month: Int, day: Int) {
        currentBsDate = clampBs(year, month, day)
    }

    fun updateFromAd(year: Int, month: Int, day: Int) {
        val safeY = year.coerceIn(1914, 2043)
        val safeM = month.coerceIn(1, 12)
        val maxD = YearMonth.of(safeY, safeM).lengthOfMonth()
        val candidateAd = LocalDate.of(safeY, safeM, day.coerceIn(1, maxD))
        currentBsDate = BsDateConverter.adToBs(candidateAd)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BracketButton(
                label = AppStrings.backToCalendar(ne),
                onClick = onBackToCalendar,
                textColor = palette.cyan
            )
            Text(
                text = AppStrings.converterTopHeader(ne),
                color = palette.cyan,
                fontFamily = MitiMonospaceFont,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        AsciiTableHorizontalBorder(columns = 1)
        Spacer(modifier = Modifier.height(8.dp))

        val boxHeader = when (mode) {
            ConverterMode.BS_TO_AD -> AppStrings.bsToAdBoxHeader(ne)
            ConverterMode.AD_TO_BS -> AppStrings.adToBsBoxHeader(ne)
            ConverterMode.AGE_DIFF -> AppStrings.ageDiffTabBtn(ne)
        }

        PlaintextBox(header = boxHeader) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BracketButton(
                    label = if (ne) "BS->AD" else "BS->AD",
                    highlighted = mode == ConverterMode.BS_TO_AD,
                    onClick = { mode = ConverterMode.BS_TO_AD },
                    textColor = if (mode == ConverterMode.BS_TO_AD) palette.cyan else palette.comment,
                    compact = true
                )
                BracketButton(
                    label = if (ne) "AD->BS" else "AD->BS",
                    highlighted = mode == ConverterMode.AD_TO_BS,
                    onClick = { mode = ConverterMode.AD_TO_BS },
                    textColor = if (mode == ConverterMode.AD_TO_BS) palette.cyan else palette.comment,
                    compact = true
                )
                BracketButton(
                    label = AppStrings.ageDiffTabBtn(ne),
                    highlighted = mode == ConverterMode.AGE_DIFF,
                    onClick = { mode = ConverterMode.AGE_DIFF },
                    textColor = if (mode == ConverterMode.AGE_DIFF) palette.purple else palette.comment,
                    compact = true
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            AsciiHorizontalRule()
            Spacer(modifier = Modifier.height(6.dp))

            when (mode) {
                ConverterMode.BS_TO_AD -> {
                    val maxBsDay = BsCalendarData.daysInMonth(currentBsDate.year, currentBsDate.month)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BracketButton("-10y", onClick = {
                            updateFromBs(currentBsDate.year - 10, currentBsDate.month, currentBsDate.day)
                        })
                        BracketButton("-1y", onClick = {
                            updateFromBs(currentBsDate.year - 1, currentBsDate.month, currentBsDate.day)
                        })
                        Text(
                            text = BsCalendarData.formatNumber(currentBsDate.year, ne),
                            color = palette.orange,
                            fontFamily = MitiMonospaceFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        BracketButton("+1y", onClick = {
                            updateFromBs(currentBsDate.year + 1, currentBsDate.month, currentBsDate.day)
                        })
                        BracketButton("+10y", onClick = {
                            updateFromBs(currentBsDate.year + 10, currentBsDate.month, currentBsDate.day)
                        })
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    for (row in 0 until 4) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (col in 0 until 3) {
                                val m = row * 3 + col + 1
                                val selected = (m == currentBsDate.month)
                                val mName = BsCalendarData.monthName(m, ne)
                                Text(
                                    text = mName,
                                    color = if (selected) palette.cyan else palette.foreground,
                                    fontFamily = MitiMonospaceFont,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (selected) palette.selection else palette.surface)
                                        .border(1.dp, if (selected) palette.cyan else palette.comment)
                                        .clickable {
                                            haptics(MitiHaptic.TICK)
                                            updateFromBs(currentBsDate.year, m, currentBsDate.day)
                                        }
                                        .padding(vertical = 6.dp, horizontal = 2.dp)
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
                        BracketButton("-5d", onClick = {
                            updateFromBs(currentBsDate.year, currentBsDate.month, (currentBsDate.day - 5).coerceAtLeast(1))
                        })
                        BracketButton("-1d", onClick = {
                            updateFromBs(currentBsDate.year, currentBsDate.month, (currentBsDate.day - 1).coerceAtLeast(1))
                        })
                        Text(
                            text = "${AppStrings.bsDayLabel(ne)} ${BsCalendarData.formatNumber(currentBsDate.day, ne)}",
                            color = palette.cyan,
                            fontFamily = MitiMonospaceFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        BracketButton("+1d", onClick = {
                            updateFromBs(currentBsDate.year, currentBsDate.month, (currentBsDate.day + 1).coerceAtMost(maxBsDay))
                        })
                        BracketButton("+5d", onClick = {
                            updateFromBs(currentBsDate.year, currentBsDate.month, (currentBsDate.day + 5).coerceAtMost(maxBsDay))
                        })
                    }
                }

                ConverterMode.AD_TO_BS -> {
                    val maxAdDay = YearMonth.of(currentAdDate.year, currentAdDate.monthValue).lengthOfMonth()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BracketButton("-10y", onClick = {
                            updateFromAd(currentAdDate.year - 10, currentAdDate.monthValue, currentAdDate.dayOfMonth)
                        })
                        BracketButton("-1y", onClick = {
                            updateFromAd(currentAdDate.year - 1, currentAdDate.monthValue, currentAdDate.dayOfMonth)
                        })
                        Text(
                            text = BsCalendarData.formatNumber(currentAdDate.year, ne),
                            color = palette.orange,
                            fontFamily = MitiMonospaceFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        BracketButton("+1y", onClick = {
                            updateFromAd(currentAdDate.year + 1, currentAdDate.monthValue, currentAdDate.dayOfMonth)
                        })
                        BracketButton("+10y", onClick = {
                            updateFromAd(currentAdDate.year + 10, currentAdDate.monthValue, currentAdDate.dayOfMonth)
                        })
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    for (row in 0 until 4) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (col in 0 until 3) {
                                val m = row * 3 + col + 1
                                val selected = (m == currentAdDate.monthValue)
                                val mLabel = "${BsCalendarData.formatNumberPadded(m, ne)} ${BsCalendarData.getAdMonthName(m, ne)}"
                                Text(
                                    text = mLabel,
                                    color = if (selected) palette.cyan else palette.foreground,
                                    fontFamily = MitiMonospaceFont,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (selected) palette.selection else palette.surface)
                                        .border(1.dp, if (selected) palette.cyan else palette.comment)
                                        .clickable {
                                            haptics(MitiHaptic.TICK)
                                            updateFromAd(currentAdDate.year, m, currentAdDate.dayOfMonth)
                                        }
                                        .padding(vertical = 6.dp, horizontal = 2.dp)
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
                        BracketButton("-5d", onClick = {
                            updateFromAd(currentAdDate.year, currentAdDate.monthValue, (currentAdDate.dayOfMonth - 5).coerceAtLeast(1))
                        })
                        BracketButton("-1d", onClick = {
                            updateFromAd(currentAdDate.year, currentAdDate.monthValue, (currentAdDate.dayOfMonth - 1).coerceAtLeast(1))
                        })
                        Text(
                            text = "${AppStrings.adDayLabel(ne)} ${BsCalendarData.formatNumber(currentAdDate.dayOfMonth, ne)}",
                            color = palette.cyan,
                            fontFamily = MitiMonospaceFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        BracketButton("+1d", onClick = {
                            updateFromAd(currentAdDate.year, currentAdDate.monthValue, (currentAdDate.dayOfMonth + 1).coerceAtMost(maxAdDay))
                        })
                        BracketButton("+5d", onClick = {
                            updateFromAd(currentAdDate.year, currentAdDate.monthValue, (currentAdDate.dayOfMonth + 5).coerceAtMost(maxAdDay))
                        })
                    }
                }

                ConverterMode.AGE_DIFF -> {
                    val activeBs = if (editingDiffStart) diffStartBs else diffEndBs
                    fun updateActive(y: Int, m: Int, d: Int) {
                        val next = clampBs(y, m, d)
                        if (editingDiffStart) diffStartBs = next else diffEndBs = next
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BracketButton(
                            label = "${AppStrings.startDateLabel(ne)} ${diffStartBs.formatIso()}",
                            highlighted = editingDiffStart,
                            onClick = { editingDiffStart = true },
                            textColor = if (editingDiffStart) palette.cyan else palette.comment,
                            compact = true
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BracketButton(
                            label = "${AppStrings.endDateLabel(ne)} ${diffEndBs.formatIso()}",
                            highlighted = !editingDiffStart,
                            onClick = { editingDiffStart = false },
                            textColor = if (!editingDiffStart) palette.purple else palette.comment,
                            compact = true
                        )
                        BracketButton(
                            label = AppStrings.todayTag(ne),
                            onClick = {
                                if (editingDiffStart) diffStartBs = todayBs else diffEndBs = todayBs
                            },
                            textColor = palette.green,
                            compact = true
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    val maxActiveDay = BsCalendarData.daysInMonth(activeBs.year, activeBs.month)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BracketButton("-10y", onClick = { updateActive(activeBs.year - 10, activeBs.month, activeBs.day) })
                        BracketButton("-1y", onClick = { updateActive(activeBs.year - 1, activeBs.month, activeBs.day) })
                        Text(
                            text = BsCalendarData.formatNumber(activeBs.year, ne),
                            color = palette.orange,
                            fontFamily = MitiMonospaceFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        BracketButton("+1y", onClick = { updateActive(activeBs.year + 1, activeBs.month, activeBs.day) })
                        BracketButton("+10y", onClick = { updateActive(activeBs.year + 10, activeBs.month, activeBs.day) })
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    for (row in 0 until 4) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (col in 0 until 3) {
                                val m = row * 3 + col + 1
                                val selected = (m == activeBs.month)
                                val mName = BsCalendarData.monthName(m, ne)
                                Text(
                                    text = mName,
                                    color = if (selected) palette.cyan else palette.foreground,
                                    fontFamily = MitiMonospaceFont,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.5.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (selected) palette.selection else palette.surface)
                                        .border(1.dp, if (selected) palette.cyan else palette.comment)
                                        .clickable {
                                            haptics(MitiHaptic.TICK)
                                            updateActive(activeBs.year, m, activeBs.day)
                                        }
                                        .padding(vertical = 5.dp, horizontal = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BracketButton("-5d", onClick = { updateActive(activeBs.year, activeBs.month, (activeBs.day - 5).coerceAtLeast(1)) })
                        BracketButton("-1d", onClick = { updateActive(activeBs.year, activeBs.month, (activeBs.day - 1).coerceAtLeast(1)) })
                        Text(
                            text = "${AppStrings.bsDayLabel(ne)} ${BsCalendarData.formatNumber(activeBs.day, ne)}",
                            color = palette.cyan,
                            fontFamily = MitiMonospaceFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        BracketButton("+1d", onClick = { updateActive(activeBs.year, activeBs.month, (activeBs.day + 1).coerceAtMost(maxActiveDay)) })
                        BracketButton("+5d", onClick = { updateActive(activeBs.year, activeBs.month, (activeBs.day + 5).coerceAtMost(maxActiveDay)) })
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    AsciiHorizontalRule()
                    Spacer(modifier = Modifier.height(6.dp))

                    val diff = remember(diffStartBs, diffEndBs) {
                        BsDateConverter.diffBsDates(diffStartBs, diffEndBs)
                    }
                    val yStr = BsCalendarData.formatNumber(diff.years, ne)
                    val mStr = BsCalendarData.formatNumber(diff.months, ne)
                    val dStr = BsCalendarData.formatNumber(diff.days, ne)
                    val totalDaysStr = BsCalendarData.formatNumber(diff.totalDays.toInt(), ne)
                    val weeks = (diff.totalDays / 7L).toInt()
                    val remDays = (diff.totalDays % 7L).toInt()
                    val wStr = BsCalendarData.formatNumber(weeks, ne)
                    val rStr = BsCalendarData.formatNumber(remDays, ne)

                    Text(
                        text = if (ne) {
                            "${AppStrings.diffSummaryLabel(true)} $yStr वर्ष, $mStr महिना, $dStr दिन"
                        } else {
                            "${AppStrings.diffSummaryLabel(false)} $yStr yrs, $mStr mos, $dStr days"
                        },
                        color = palette.green,
                        fontFamily = MitiMonospaceFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (ne) {
                            "${AppStrings.totalDaysLabel(true)} $totalDaysStr दिन"
                        } else {
                            "${AppStrings.totalDaysLabel(false)} $totalDaysStr days"
                        },
                        color = palette.cyan,
                        fontFamily = MitiMonospaceFont,
                        fontSize = 13.sp
                    )
                    Text(
                        text = if (ne) {
                            "${AppStrings.totalWeeksLabel(true)} $wStr हप्ता, $rStr दिन"
                        } else {
                            "${AppStrings.totalWeeksLabel(false)} $wStr wks, $rStr days"
                        },
                        color = palette.comment,
                        fontFamily = MitiMonospaceFont,
                        fontSize = 12.5.sp
                    )
                }
            }

            if (mode != ConverterMode.AGE_DIFF) {
                Spacer(modifier = Modifier.height(8.dp))
                AsciiHorizontalRule()
                Spacer(modifier = Modifier.height(8.dp))

                val wdIndex = BsDateConverter.dayOfWeekIndex(currentAdDate)
                val wdName = if (ne) BsCalendarData.weekdayFull(wdIndex, true) else BsCalendarData.weekdayShort(wdIndex, false)
                val wdShort = BsCalendarData.weekdayShort(wdIndex, ne)
                val adFormatted = if (ne) BsCalendarData.formatAdDate(currentAdDate, true) else currentAdDate.toString()
                val daysDiff = ChronoUnit.DAYS.between(LocalDate.now(), currentAdDate)
                val relBadge = "[${AppStrings.formatRelativeDays(daysDiff, ne)}]"
                Text(
                    text = "${AppStrings.bsDateLabel(ne)} ${currentBsDate.formatHuman(ne)} ($wdName) $relBadge",
                    color = if (mode == ConverterMode.BS_TO_AD) palette.foreground else palette.green,
                    fontFamily = MitiMonospaceFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${AppStrings.adDateLabel(ne)} $adFormatted ($wdShort)",
                    color = if (mode == ConverterMode.BS_TO_AD) palette.green else palette.foreground,
                    fontFamily = MitiMonospaceFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                val tithiSpan = remember(currentAdDate, tithi.number, ne) {
                    TithiCalculator.calculateTithiTimeSpan(currentAdDate, tithi.number)
                        .formatDisplay(currentAdDate, ne)
                }
                Text(
                    text = "${AppStrings.tithiLabel(ne)} ${tithi.stripDisplay(ne)}",
                    color = palette.cyan,
                    fontFamily = MitiMonospaceFont,
                    fontSize = 13.sp
                )
                Text(
                    text = "      $tithiSpan",
                    color = palette.comment,
                    fontFamily = MitiMonospaceFont,
                    fontSize = 11.5.sp
                )
                if (festivals.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    val festNames = festivals.joinToString(", ") {
                        if (ne) it.nepaliTitle else it.englishTitle
                    }
                    Text(
                        text = "* $festNames",
                        color = palette.red,
                        fontFamily = MitiMonospaceFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BracketButton(
                        label = AppStrings.openInCalendarBtn(ne),
                        onClick = { onOpenBsDateInCalendar(currentBsDate) },
                        textColor = palette.cyan
                    )
                    BracketButton(
                        label = "+",
                        onClick = { onAddEventOnBsDate(currentBsDate) },
                        textColor = palette.green,
                        highlighted = true
                    )
                }
            }
        }
    }
}
