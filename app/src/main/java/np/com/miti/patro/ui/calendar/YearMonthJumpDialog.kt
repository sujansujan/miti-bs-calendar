package np.com.miti.patro.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.window.Dialog
import np.com.miti.patro.domain.engine.BsCalendarData
import np.com.miti.patro.ui.components.AsciiHorizontalRule
import np.com.miti.patro.ui.components.BracketButton
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
    var year by remember { mutableIntStateOf(currentYear) }
    var month by remember { mutableIntStateOf(currentMonth) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.background)
                .border(1.dp, palette.foreground)
                .padding(12.dp)
        ) {
            Text(
                text = AppStrings.jumpHeader(ne),
                color = palette.purple,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            AsciiHorizontalRule()
            Spacer(modifier = Modifier.height(8.dp))

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
                BracketButton(label = "-10", onClick = {
                    year = (year - 10).coerceAtLeast(BsCalendarData.MIN_YEAR)
                })
                BracketButton(label = "-1", onClick = {
                    year = (year - 1).coerceAtLeast(BsCalendarData.MIN_YEAR)
                })
                Text(
                    text = BsCalendarData.formatNumber(year, ne),
                    color = palette.cyan,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                BracketButton(label = "+1", onClick = {
                    year = (year + 1).coerceAtMost(BsCalendarData.MAX_YEAR)
                })
                BracketButton(label = "+10", onClick = {
                    year = (year + 10).coerceAtMost(BsCalendarData.MAX_YEAR)
                })
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = AppStrings.selectMonthLabel(ne),
                color = palette.comment,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
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
                        val selected = m == month
                        val mName = BsCalendarData.monthName(m, ne)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (selected) palette.selection else palette.surface)
                                .border(1.dp, if (selected) palette.cyan else palette.comment)
                                .clickable { month = m }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mName,
                                color = if (selected) palette.cyan else palette.foreground,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            AsciiHorizontalRule()
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                BracketButton(
                    label = AppStrings.cancelBtn(ne),
                    onClick = onDismiss,
                    textColor = palette.comment
                )
                Spacer(modifier = Modifier.width(8.dp))
                BracketButton(
                    label = AppStrings.goBtn(ne),
                    onClick = { onConfirm(year, month) },
                    textColor = palette.green,
                    highlighted = true
                )
            }
        }
    }
}
