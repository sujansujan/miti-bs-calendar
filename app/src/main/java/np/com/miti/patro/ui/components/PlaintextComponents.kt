package np.com.miti.patro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import np.com.miti.patro.ui.theme.LocalMitiPalette

private const val DEFAULT_ASCII_RULE =
    "+----------------------------------------------------------------------------------------------------+"

@Composable
fun AsciiHorizontalRule(
    modifier: Modifier = Modifier,
    color: Color = LocalMitiPalette.current.comment
) {
    Text(
        text = "----------------------------------------------------------------------------------------------------",
        color = color,
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        maxLines = 1,
        overflow = TextOverflow.Clip,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun AsciiTableHorizontalBorder(
    modifier: Modifier = Modifier,
    color: Color = LocalMitiPalette.current.comment
) {
    Text(
        text = "+--------------+--------------+--------------+--------------+--------------+--------------+--------------+",
        color = color,
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        maxLines = 1,
        overflow = TextOverflow.Clip,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun AsciiGridHorizontalDivider(
    columns: Int = 7,
    modifier: Modifier = Modifier,
    color: Color = LocalMitiPalette.current.comment
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(7.dp)
    ) {
        val strokePx = 1.dp.toPx()
        val midY = size.height / 2f
        val tickHalf = 3.dp.toPx()
        val lineColor = color.copy(alpha = 0.65f)
        drawLine(
            color = lineColor,
            start = Offset(0f, midY),
            end = Offset(size.width, midY),
            strokeWidth = strokePx
        )
        for (col in 0..columns) {
            val x = (col * size.width / columns).coerceIn(strokePx / 2f, size.width - strokePx / 2f)
            drawLine(
                color = lineColor,
                start = Offset(x, midY - tickHalf),
                end = Offset(x, midY + tickHalf),
                strokeWidth = strokePx
            )
        }
    }
}

@Composable
fun PlaintextToggleRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = LocalMitiPalette.current.green,
    labelColor: Color = LocalMitiPalette.current.foreground
) {
    val palette = LocalMitiPalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 40.dp)
            .clickable(onClick = onToggle)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = labelColor,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        )
        Box(
            modifier = Modifier
                .background(if (checked) palette.selection else Color.Transparent)
                .border(1.dp, if (checked) activeColor else palette.comment)
                .padding(horizontal = 6.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (checked) "ON " else "OFF",
                color = if (checked) activeColor else palette.comment,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun BracketButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    highlighted: Boolean = false,
    textColor: Color? = null
) {
    val palette = LocalMitiPalette.current
    val resolvedText = when {
        !enabled -> palette.comment
        textColor != null -> textColor
        highlighted -> palette.cyan
        else -> palette.foreground
    }
    val bgColor = if (highlighted) palette.selection else Color.Transparent

    Box(
        modifier = modifier
            .background(bgColor)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (highlighted) "[*$label*]" else "[$label]",
            color = resolvedText,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Medium,
            fontSize = 13.sp,
            maxLines = 1
        )
    }
}

@Composable
fun PlaintextBox(
    header: String,
    modifier: Modifier = Modifier,
    headerColor: Color = LocalMitiPalette.current.cyan,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalMitiPalette.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.selection)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "+-- $header ",
                color = headerColor,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1
            )
            Text(
                text = "--------------------------------------------------------------------------------",
                color = palette.comment,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier.weight(1f)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            content = content
        )
        Text(
            text = DEFAULT_ASCII_RULE,
            color = palette.comment,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun PlaintextTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true
) {
    val palette = LocalMitiPalette.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.background)
            .border(1.dp, palette.comment)
            .padding(horizontal = 8.dp, vertical = 7.dp)
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                color = palette.comment,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = TextStyle(
                color = palette.foreground,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            ),
            cursorBrush = SolidColor(palette.cyan),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
