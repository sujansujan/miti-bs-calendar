package np.com.miti.patro.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import np.com.miti.patro.ui.components.BracketButton
import np.com.miti.patro.ui.components.PlaintextBox
import np.com.miti.patro.ui.theme.AppStrings
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
    val ne = useDevanagari
    var year by remember { mutableIntStateOf(initialYear) }
    var month by remember { mutableIntStateOf(initialMonth) }

    Dialog(onDismissRequest = onDismiss) {
        PlaintextBox(
            borderColor = palette.cyan,
            backgroundColor = palette.background,
            headerText = AppStrings.jumpDialogHeader(ne),
            headerColor = palette.cyan
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BracketButton(
                    text = AppStrings.minus10YBtn(ne),
                    onClick = { year = (year - 10).coerceAtLeast(BsCalendarData.MIN_YEAR) },
                    color = palette.comment
                )
                BracketButton(
                    text = AppStrings.minus1YBtn(ne),
                    onClick = { year = (year - 1).coerceAtLeast(BsCalendarData.MIN_YEAR) },
                    color = palette.cyan
                )
                Text(
                    text = BsCalendarData.formatNumber(year, ne),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = palette.yellow
                )
                BracketButton(
                    text = AppStrings.plus1YBtn(ne),
                    onClick = { year = (year + 1).coerceAtMost(BsCalendarData.MAX_YEAR) },
                    color = palette.cyan
                )
                BracketButton(
                    text = AppStrings.plus10YBtn(ne),
                    onClick = { year = (year + 10).coerceAtMost(BsCalendarData.MAX_YEAR) },
                    color = palette.comment
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            AsciiHorizontalRule()
            Spacer(modifier = Modifier.height(6.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (m in 1..12) {
                    val mName = BsCalendarData.getMonthName(m, ne)
                    val isSelected = (m == month)
                    BracketButton(
                        text = if (isSelected) "*$mName*" else "[$mName]",
                        onClick = { month = m },
                        color = if (isSelected) palette.purple else palette.foreground,
                        isHighlighted = isSelected
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            AsciiHorizontalRule()
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BracketButton(
                    text = AppStrings.cancelBtn(ne),
                    onClick = onDismiss,
                    color = palette.comment
                )
                BracketButton(
                    text = AppStrings.goBtn(ne),
                    onClick = { onConfirm(year, month) },
                    color = palette.green,
                    isHighlighted = true
                )
            }
        }
    }
}
