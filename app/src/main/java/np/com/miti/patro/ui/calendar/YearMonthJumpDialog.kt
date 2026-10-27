package np.com.miti.patro.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import np.com.miti.patro.ui.components.PlaintextBox
import np.com.miti.patro.ui.components.PlaintextBracketButton
import np.com.miti.patro.ui.theme.AppStrings
import np.com.miti.patro.ui.theme.LocalMitiPalette

@Composable
fun YearMonthJumpDialog(
    initialYear: Int,
    initialMonth: Int,
    nepaliScript: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (year: Int, month: Int) -> Unit
) {
    val palette = LocalMitiPalette.current
    val ne = nepaliScript
    var selectedYear by remember { mutableIntStateOf(initialYear) }
    var selectedMonth by remember { mutableIntStateOf(initialMonth) }

    Dialog(onDismissRequest = onDismiss) {
        PlaintextBox(
            headerTitle = AppStrings.jumpToBsYearMonthHeader(ne),
            headerRight = "${BsCalendarData.formatNumber(selectedYear, ne)}-${BsCalendarData.formatNumber(selectedMonth, ne).padStart(2, if (ne) '०' else '0')}",
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.background)
        ) {
            // Year Stepper
            Text(
                text = AppStrings.yearRangeLabel(ne),
                color = palette.comment,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlaintextBracketButton(
                    label = if (ne) "[-१०]" else "[-10]",
                    onClick = { selectedYear = (selectedYear - 10).coerceAtLeast(BsCalendarData.MIN_YEAR) }
                )
                PlaintextBracketButton(
                    label = if (ne) "[-१]" else "[-1]",
                    onClick = { selectedYear = (selectedYear - 1).coerceAtLeast(BsCalendarData.MIN_YEAR) }
                )
                Text(
                    text = BsCalendarData.formatNumber(selectedYear, ne),
                    color = palette.accentPurple,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                PlaintextBracketButton(
                    label = if (ne) "[+१]" else "[+1]",
                    onClick = { selectedYear = (selectedYear + 1).coerceAtMost(BsCalendarData.MAX_YEAR) }
                )
                PlaintextBracketButton(
                    label = if (ne) "[+१०]" else "[+10]",
                    onClick = { selectedYear = (selectedYear + 10).coerceAtMost(BsCalendarData.MAX_YEAR) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            AsciiHorizontalRule()
            Spacer(modifier = Modifier.height(6.dp))

            // 12 BS Months in a 3x4 plaintext grid
            Text(
                text = AppStrings.selectMonthLabel(ne),
                color = palette.comment,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(4.dp))

            for (row in 0 until 4) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (col in 0 until 3) {
                        val m = row * 3 + col + 1
                        val isCurrent = m == selectedMonth
                        val mName = BsCalendarData.getMonthName(m, ne)
                        val label = if (isCurrent) "*$mName*" else "[$mName]"
                        PlaintextBracketButton(
                            label = label,
                            onClick = { selectedMonth = m },
                            textColor = if (isCurrent) palette.accentGreen else palette.foreground,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            AsciiHorizontalRule()
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PlaintextBracketButton(
                    label = AppStrings.cancelBtn(ne),
                    onClick = onDismiss,
                    textColor = palette.comment
                )
                PlaintextBracketButton(
                    label = AppStrings.goBtn(ne),
                    onClick = { onConfirm(selectedYear, selectedMonth) },
                    textColor = palette.accentGreen
                )
            }
        }
    }
}
