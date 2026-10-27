package np.com.miti.patro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import np.com.miti.patro.ui.theme.LocalMitiPalette

/**
 * Plaintext Sports-inspired box container: sharp 1dp hairline border, zero elevation,
 * zero rounded corners, optional top ASCII-style header strip.
 */
@Composable
fun PlaintextBox(
    modifier: Modifier = Modifier,
    borderColor: Color = LocalMitiPalette.current.comment,
    backgroundColor: Color = LocalMitiPalette.current.surface,
    headerText: String? = null,
    headerColor: Color = LocalMitiPalette.current.comment,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalMitiPalette.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .border(width = 1.dp, color = borderColor)
    ) {
        if (!headerText.isNullOrBlank()) {
            Text(
                text = headerText,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = headerColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.selection.copy(alpha = 0.45f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(borderColor)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            content = content
        )
    }
}

/**
 * Plaintext bracketed link/action control (e.g., "[+Event]", "< Ashwin", "[Today]").
 */
@Composable
fun BracketButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = LocalMitiPalette.current.foreground,
    isHighlighted: Boolean = false
) {
    val palette = LocalMitiPalette.current
    Text(
        text = text,
        fontFamily = FontFamily.Monospace,
        fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
        fontSize = 13.sp,
        color = color,
        modifier = modifier
            .background(if (isHighlighted) palette.selection else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    )
}
