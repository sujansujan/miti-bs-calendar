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
import androidx.compose.ui.text.font.FontFamily
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
import np.com.miti.patro.ui.components.BracketButton
import np.com.miti.patro.ui.components.PlaintextBox
import np.com.miti.patro.ui.theme.AppStrings
import np.com.miti.patro.ui.theme.LocalMitiPalette
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

private val AD_MONTH_SHORT_EN = arrayOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)

/**
 * Single unified BS <-> AD Date Converter workspace.
 * Both BS -> AD and AD -> BS modes edit the exact same synchronized date in one place.
 */
@Composable
fun DateConverterScreen(
    initialBsDate: BsDate,
    useDevanagari: Boolean,
    onBackToCalendar: () -> Unit,
    onOpenBsDateInCalendar: (BsDate) -> Unit,
    onAddEventOnBsDate: (BsDate) -> Unit
) {
    val palette = LocalMitiPalette.current
    val ne = useDevanagari
    val scrollState = rememberScrollState()

    // Single synchronized date state (stored as BsDate; AD is derived losslessly)
    var isBsInputMode by remember { mutableStateOf(true) }
    var currentBsDate by remember { mutableStateOf(initialBsDate) }

    val currentAdDate = remember(currentBsDate) { BsDateConverter.bsToAd(currentBsDate) }
    val tithi = remember(currentAdDate) { TithiCalculator.calculateTithiAtSunrise(currentAdDate) }
    val festivals = remember(currentBsDate, tithi) {
        NepaliFestivals.getFestivalsForDay(currentBsDate, tithi)
    }

    fun updateFromBs(year: Int, month: Int, day: Int) {
        val safeY = year.coerceIn(BsCalendarData.MIN_YEAR, BsCalendarData.MAX_YEAR)
        val safeM = month.coerceIn(1, 12)
        val maxD = BsCalendarData.daysInMonth(safeY, safeM)
        currentBsDate = BsDate(safeY, safeM, day.coerceIn(1, maxD))
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
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        // Top Bar
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
                color = palette.foreground,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Unified Converter Box
        PlaintextBox(
            header = if (isBsInputMode) AppStrings.bsToAdBoxHeader(ne) else AppStrings.adToBsBoxHeader(ne)
        ) {
            // Mode Switcher Row: [*BS -> AD*]   [AD -> BS]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BracketButton(
                    label = AppStrings.bsToAdBoxHeader(ne),
                    highlighted = isBsInputMode,
                    onClick = { isBsInputMode = true },
                    textColor = if (isBsInputMode) palette.cyan else palette.comment
                )
                BracketButton(
                    label = AppStrings.adToBsBoxHeader(ne),
                    highlighted = !isBsInputMode,
                    onClick = { isBsInputMode = false },
                    textColor = if (!isBsInputMode) palette.cyan else palette.comment
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            AsciiHorizontalRule()
            Spacer(modifier = Modifier.height(6.dp))

            if (isBsInputMode) {
                // BS Input Controls
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
                        fontFamily = FontFamily.Monospace,
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

                // 4x3 BS Month Grid
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
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (selected) palette.selection else palette.surface)
                                    .border(1.dp, if (selected) palette.cyan else palette.comment)
                                    .clickable { updateFromBs(currentBsDate.year, m, currentBsDate.day) }
                                    .padding(vertical = 6.dp, horizontal = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // BS Day Stepper
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
                        fontFamily = FontFamily.Monospace,
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
            } else {
                // AD Input Controls
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
                        fontFamily = FontFamily.Monospace,
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

                // 4x3 AD Month Grid
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
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (selected) palette.selection else palette.surface)
                                    .border(1.dp, if (selected) palette.cyan else palette.comment)
                                    .clickable { updateFromAd(currentAdDate.year, m, currentAdDate.dayOfMonth) }
                                    .padding(vertical = 6.dp, horizontal = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // AD Day Stepper
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
                        fontFamily = FontFamily.Monospace,
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

            Spacer(modifier = Modifier.height(8.dp))
            AsciiHorizontalRule()
            Spacer(modifier = Modifier.height(8.dp))

            // Unified Live Result Block
            val wdIndex = BsDateConverter.dayOfWeekIndex(currentAdDate)
            val wdName = if (ne) BsCalendarData.weekdayFull(wdIndex, true) else BsCalendarData.weekdayShort(wdIndex, false)
            val wdShort = BsCalendarData.weekdayShort(wdIndex, ne)
            val adFormatted = if (ne) BsCalendarData.formatAdDate(currentAdDate, true) else currentAdDate.toString()
            val daysDiff = ChronoUnit.DAYS.between(LocalDate.now(), currentAdDate)
            val relBadge = "[${AppStrings.formatRelativeDays(daysDiff, ne)}]"
            Text(
                text = "${AppStrings.bsDateLabel(ne)} ${currentBsDate.formatHuman(ne)} ($wdName) $relBadge",
                color = if (isBsInputMode) palette.foreground else palette.green,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${AppStrings.adDateLabel(ne)} $adFormatted ($wdShort)",
                color = if (isBsInputMode) palette.green else palette.foreground,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            val tithiDisplay = if (ne) {
                "${tithi.phaseSymbol} ${tithi.nepaliName} (${tithi.paksha.nepaliName})"
            } else {
                "${tithi.phaseSymbol} ${tithi.englishName} (${tithi.paksha.shortCode})"
            }
            Text(
                text = "${AppStrings.tithiLabel(ne)} $tithiDisplay",
                color = palette.cyan,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            )
            if (festivals.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                val festNames = festivals.joinToString(", ") {
                    if (ne) it.nepaliTitle else it.englishTitle
                }
                Text(
                    text = "* $festNames",
                    color = palette.red,
                    fontFamily = FontFamily.Monospace,
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
