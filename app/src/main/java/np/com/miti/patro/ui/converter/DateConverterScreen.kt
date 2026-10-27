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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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

private val AD_MONTH_SHORT_EN = arrayOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)

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

    // Section 1: BS -> AD state
    var bsYear by remember { mutableIntStateOf(initialBsDate.year) }
    var bsMonth by remember { mutableIntStateOf(initialBsDate.month) }
    val maxBsDay = BsCalendarData.daysInMonth(bsYear, bsMonth)
    var bsDay by remember { mutableIntStateOf(initialBsDate.day.coerceIn(1, maxBsDay)) }
    val safeBsDay = bsDay.coerceIn(1, maxBsDay)
    val currentBsInput = BsDate(bsYear, bsMonth, safeBsDay)
    val convertedAdFromBs = remember(currentBsInput) { BsDateConverter.bsToAd(currentBsInput) }
    val bsToAdTithi = remember(convertedAdFromBs) { TithiCalculator.calculateTithiAtSunrise(convertedAdFromBs) }
    val bsToAdFestivals = remember(currentBsInput, bsToAdTithi) {
        NepaliFestivals.getFestivalsForDay(currentBsInput, bsToAdTithi)
    }

    // Section 2: AD -> BS state
    val initialAd = remember(initialBsDate) { BsDateConverter.bsToAd(initialBsDate) }
    var adYear by remember { mutableIntStateOf(initialAd.year.coerceIn(1914, 2043)) }
    var adMonth by remember { mutableIntStateOf(initialAd.monthValue) }
    val maxAdDay = remember(adYear, adMonth) { YearMonth.of(adYear, adMonth).lengthOfMonth() }
    var adDay by remember { mutableIntStateOf(initialAd.dayOfMonth.coerceIn(1, maxAdDay)) }
    val safeAdDay = adDay.coerceIn(1, maxAdDay)
    val currentAdInput = LocalDate.of(adYear, adMonth, safeAdDay)
    val convertedBsFromAd = remember(currentAdInput) { BsDateConverter.adToBs(currentAdInput) }
    val adToBsTithi = remember(currentAdInput) { TithiCalculator.calculateTithiAtSunrise(currentAdInput) }
    val adToBsFestivals = remember(convertedBsFromAd, adToBsTithi) {
        NepaliFestivals.getFestivalsForDay(convertedBsFromAd, adToBsTithi)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        // Top Navigation Bar
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
                text = if (ne) "बि.सं. <-> इ.सं." else "BS <-> AD",
                color = palette.foreground,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = AppStrings.converterTopHeader(ne),
            color = palette.purple,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
        AsciiHorizontalRule()

        Spacer(modifier = Modifier.height(6.dp))

        // =====================================================================
        // BOX 1: BS -> AD CONVERTER
        // =====================================================================
        PlaintextBox(header = AppStrings.bsToAdBoxHeader(ne)) {
            // Year Stepper
            Text(
                text = AppStrings.selectYearLabel(ne),
                color = palette.comment,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BracketButton("-10y", onClick = {
                    bsYear = (bsYear - 10).coerceIn(BsCalendarData.MIN_YEAR, BsCalendarData.MAX_YEAR)
                })
                BracketButton("-1y", onClick = {
                    bsYear = (bsYear - 1).coerceIn(BsCalendarData.MIN_YEAR, BsCalendarData.MAX_YEAR)
                })
                Text(
                    text = BsCalendarData.formatNumber(bsYear, ne),
                    color = palette.orange,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                BracketButton("+1y", onClick = {
                    bsYear = (bsYear + 1).coerceIn(BsCalendarData.MIN_YEAR, BsCalendarData.MAX_YEAR)
                })
                BracketButton("+10y", onClick = {
                    bsYear = (bsYear + 10).coerceIn(BsCalendarData.MIN_YEAR, BsCalendarData.MAX_YEAR)
                })
            }

            // BS Month Grid (4x3)
            Text(
                text = AppStrings.selectMonthLabel(ne),
                color = palette.comment,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            for (row in 0 until 4) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (col in 0 until 3) {
                        val m = row * 3 + col + 1
                        val selected = m == bsMonth
                        val mName = BsCalendarData.monthName(m, ne)
                        Text(
                            text = if (selected) "[*$mName*]" else "[$mName]",
                            color = if (selected) palette.cyan else palette.foreground,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .weight(1f)
                                .background(if (selected) palette.selection else palette.surface)
                                .border(1.dp, if (selected) palette.cyan else palette.comment)
                                .clickable { bsMonth = m }
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            // BS Day Stepper
            Text(
                text = "${AppStrings.bsDayLabel(ne)} (1..${BsCalendarData.formatNumber(maxBsDay, ne)})",
                color = palette.comment,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BracketButton("-5d", onClick = { bsDay = (safeBsDay - 5).coerceIn(1, maxBsDay) })
                BracketButton("-1d", onClick = { bsDay = (safeBsDay - 1).coerceIn(1, maxBsDay) })
                Text(
                    text = BsCalendarData.formatNumber(safeBsDay, ne),
                    color = palette.cyan,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                BracketButton("+1d", onClick = { bsDay = (safeBsDay + 1).coerceIn(1, maxBsDay) })
                BracketButton("+5d", onClick = { bsDay = (safeBsDay + 5).coerceIn(1, maxBsDay) })
            }

            AsciiHorizontalRule()
            Spacer(modifier = Modifier.height(4.dp))

            // Output Result for BS -> AD
            Text(
                text = AppStrings.convertedResultHeader(ne),
                color = palette.purple,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            val wdIndex = BsDateConverter.dayOfWeekIndex(convertedAdFromBs)
            val wdName = BsCalendarData.weekdayFull(wdIndex, ne)
            Text(
                text = "${AppStrings.bsDateLabel(ne)} ${currentBsInput.formatHuman(ne)} ($wdName)",
                color = palette.foreground,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Text(
                text = "${AppStrings.adDateLabel(ne)} $convertedAdFromBs (${convertedAdFromBs.dayOfWeek.name})",
                color = palette.green,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            val tithiDisplay = if (ne) {
                "${bsToAdTithi.nepaliName} (${bsToAdTithi.paksha.nepaliName})"
            } else {
                "${bsToAdTithi.englishName} (${bsToAdTithi.paksha.englishName})"
            }
            Text(
                text = "${AppStrings.tithiLabel(ne)} $tithiDisplay",
                color = palette.cyan,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
            if (bsToAdFestivals.isNotEmpty()) {
                val festNames = bsToAdFestivals.joinToString(", ") {
                    if (ne) it.nepaliTitle else it.englishTitle
                }
                Text(
                    text = "${AppStrings.festivalsHeader(ne)} $festNames",
                    color = palette.red,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BracketButton(
                    label = AppStrings.openInCalendarBtn(ne),
                    onClick = { onOpenBsDateInCalendar(currentBsInput) },
                    textColor = palette.cyan
                )
                BracketButton(
                    label = AppStrings.addEventOnConvertedBtn(ne),
                    onClick = { onAddEventOnBsDate(currentBsInput) },
                    textColor = palette.green
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // =====================================================================
        // BOX 2: AD -> BS CONVERTER
        // =====================================================================
        PlaintextBox(header = AppStrings.adToBsBoxHeader(ne)) {
            Text(
                text = AppStrings.adYearLabel(ne),
                color = palette.comment,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BracketButton("-10y", onClick = { adYear = (adYear - 10).coerceIn(1914, 2043) })
                BracketButton("-1y", onClick = { adYear = (adYear - 1).coerceIn(1914, 2043) })
                Text(
                    text = adYear.toString(),
                    color = palette.orange,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                BracketButton("+1y", onClick = { adYear = (adYear + 1).coerceIn(1914, 2043) })
                BracketButton("+10y", onClick = { adYear = (adYear + 10).coerceIn(1914, 2043) })
            }

            // AD Month Grid (4x3)
            Text(
                text = AppStrings.adMonthLabel(ne),
                color = palette.comment,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            for (row in 0 until 4) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (col in 0 until 3) {
                        val m = row * 3 + col + 1
                        val selected = m == adMonth
                        val mLabel = "${m.toString().padStart(2, '0')} ${AD_MONTH_SHORT_EN[m - 1]}"
                        Text(
                            text = if (selected) "[*$mLabel*]" else "[$mLabel]",
                            color = if (selected) palette.cyan else palette.foreground,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .weight(1f)
                                .background(if (selected) palette.selection else palette.surface)
                                .border(1.dp, if (selected) palette.cyan else palette.comment)
                                .clickable { adMonth = m }
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${AppStrings.adDayLabel(ne)} (1..$maxAdDay)",
                color = palette.comment,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BracketButton("-5d", onClick = { adDay = (safeAdDay - 5).coerceIn(1, maxAdDay) })
                BracketButton("-1d", onClick = { adDay = (safeAdDay - 1).coerceIn(1, maxAdDay) })
                Text(
                    text = safeAdDay.toString(),
                    color = palette.cyan,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                BracketButton("+1d", onClick = { adDay = (safeAdDay + 1).coerceIn(1, maxAdDay) })
                BracketButton("+5d", onClick = { adDay = (safeAdDay + 5).coerceIn(1, maxAdDay) })
            }

            AsciiHorizontalRule()
            Spacer(modifier = Modifier.height(4.dp))

            // Output Result for AD -> BS
            Text(
                text = AppStrings.convertedResultHeader(ne),
                color = palette.purple,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            val wdIndex = BsDateConverter.dayOfWeekIndex(currentAdInput)
            val wdName = BsCalendarData.weekdayFull(wdIndex, ne)
            Text(
                text = "${AppStrings.adDateLabel(ne)} $currentAdInput (${currentAdInput.dayOfWeek.name})",
                color = palette.foreground,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Text(
                text = "${AppStrings.bsDateLabel(ne)} ${convertedBsFromAd.formatHuman(ne)} ($wdName)",
                color = palette.green,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            val tithiDisplay = if (ne) {
                "${adToBsTithi.nepaliName} (${adToBsTithi.paksha.nepaliName})"
            } else {
                "${adToBsTithi.englishName} (${adToBsTithi.paksha.englishName})"
            }
            Text(
                text = "${AppStrings.tithiLabel(ne)} $tithiDisplay",
                color = palette.cyan,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
            if (adToBsFestivals.isNotEmpty()) {
                val festNames = adToBsFestivals.joinToString(", ") {
                    if (ne) it.nepaliTitle else it.englishTitle
                }
                Text(
                    text = "${AppStrings.festivalsHeader(ne)} $festNames",
                    color = palette.red,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BracketButton(
                    label = AppStrings.openInCalendarBtn(ne),
                    onClick = { onOpenBsDateInCalendar(convertedBsFromAd) },
                    textColor = palette.cyan
                )
                BracketButton(
                    label = AppStrings.addEventOnConvertedBtn(ne),
                    onClick = { onAddEventOnBsDate(convertedBsFromAd) },
                    textColor = palette.green
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
