package np.com.miti.patro.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import np.com.miti.patro.domain.engine.BsCalendarData
import np.com.miti.patro.ui.components.AsciiHorizontalRule
import np.com.miti.patro.ui.components.BracketButton
import np.com.miti.patro.ui.components.PlaintextBox
import np.com.miti.patro.ui.theme.AppStrings
import np.com.miti.patro.ui.theme.LocalMitiPalette

@Composable
fun YearMonthJumpDialog(
    currentYear: Int,
    currentMonth: Int,
    useDevanagari: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val palette = LocalMitiPalette.current
    val ne = useDevanagari
    var selectedYear by remember { mutableIntStateOf(currentYear) }
    var selectedMonth by remember { mutableIntStateOf(currentMonth) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.background)
                .border(1.dp, palette.cyan)
                .padding(6.dp)
        ) {
            PlaintextBox(header = AppStrings.jumpHeader(ne)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BracketButton(
                        label = "-10",
                        onClick = {
                            selectedYear = (selectedYear - 10).coerceAtLeast(BsCalendarData.MIN_YEAR)
                        }
                    )
                    BracketButton(
                        label = "-1",
                        onClick = {
                            selectedYear = (selectedYear - 1).coerceAtLeast(BsCalendarData.MIN_YEAR)
                        }
                    )
                    Text(
                        text = BsCalendarData.formatNumber(selectedYear, ne),
                        color = palette.orange,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    BracketButton(
                        label = "+1",
                        onClick = {
                            selectedYear = (selectedYear + 1).coerceAtMost(BsCalendarData.MAX_YEAR)
                        }
                    )
                    BracketButton(
                        label = "+10",
                        onClick = {
                            selectedYear = (selectedYear + 10).coerceAtMost(BsCalendarData.MAX_YEAR)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                AsciiHorizontalRule()
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
                            val isChosen = (m == selectedMonth)
                            val mName = BsCalendarData.monthName(m, ne)
                            Text(
                                text = mName,
                                color = if (isChosen) palette.cyan else palette.foreground,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isChosen) palette.selection else palette.surface)
                                    .border(1.dp, if (isChosen) palette.cyan else palette.comment)
                                    .clickable { selectedMonth = m }
                                    .padding(vertical = 6.dp, horizontal = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                AsciiHorizontalRule()
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BracketButton(
                        label = AppStrings.cancelBtn(ne),
                        onClick = onDismiss,
                        textColor = palette.red
                    )
                    BracketButton(
                        label = AppStrings.goBtn(ne),
                        highlighted = true,
                        onClick = { onConfirm(selectedYear, selectedMonth) },
                        textColor = palette.green
                    )
                }
            }
        }
    }
}
