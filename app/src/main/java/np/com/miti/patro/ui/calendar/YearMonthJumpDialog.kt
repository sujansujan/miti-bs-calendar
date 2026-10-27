package np.com.miti.patro.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import np.com.miti.patro.domain.engine.BsCalendarData
import np.com.miti.patro.ui.components.BracketButton
import np.com.miti.patro.ui.components.PlaintextBox
import np.com.miti.patro.ui.theme.LocalMitiPalette

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun YearMonthJumpDialog(
    initialYear: Int,
    initialMonth: Int,
    useDevanagari: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val palette = LocalMitiPalette.current
    var year by remember { mutableIntStateOf(initialYear) }
    var month by remember { mutableIntStateOf(initialMonth) }

    Dialog(onDismissRequest = onDismiss) {
        PlaintextBox(
            borderColor = palette.cyan,
            backgroundColor = palette.background,
            headerText = "JUMP TO BS YEAR & MONTH (1970–2100)",
            headerColor = palette.cyan
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BracketButton(
                    text = "[-10y]",
                    onClick = { year = (year - 10).coerceAtLeast(BsCalendarData.MIN_YEAR) },
                    color = palette.comment
                )
                BracketButton(
                    text = "[-1y]",
                    onClick = { year = (year - 1).coerceAtLeast(BsCalendarData.MIN_YEAR) },
                    color = palette.cyan
                )
                Text(
                    text = BsCalendarData.formatNumber(year, useDevanagari),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = palette.yellow
                )
                BracketButton(
                    text = "[+1y]",
                    onClick = { year = (year + 1).coerceAtMost(BsCalendarData.MAX_YEAR) },
                    color = palette.cyan
                )
                BracketButton(
                    text = "[+10y]",
                    onClick = { year = (year + 10).coerceAtMost(BsCalendarData.MAX_YEAR) },
                    color = palette.comment
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (m in 1..12) {
                    val mName = BsCalendarData.getMonthName(m, useDevanagari)
                    val isSelected = (m == month)
                    Text(
                        text = if (isSelected) "[$mName]" else " $mName ",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp,
                        color = if (isSelected) palette.purple else palette.foreground,
                        modifier = Modifier
                            .background(if (isSelected) palette.selection else palette.surface)
                            .border(1.dp, if (isSelected) palette.purple else palette.comment)
                            .clickable { month = m }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                BracketButton(text = "[Cancel]", onClick = onDismiss, color = palette.comment)
                BracketButton(
                    text = "[Go]",
                    onClick = { onConfirm(year, month) },
                    color = palette.green,
                    isHighlighted = true
                )
            }
        }
    }
}
