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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import np.com.miti.patro.ui.theme.LocalMitiPalette

private val ASCII_EDGE_WIDTH = 7.dp

private fun DrawScope.drawAsciiPlus(cx: Float, cy: Float, color: Color) {
    val stroke = 1.15.dp.toPx()
    val halfH = 2.6.dp.toPx()
    val halfV = 3.2.dp.toPx()
    drawLine(
        color = color,
        start = Offset(cx - halfH, cy),
        end = Offset(cx + halfH, cy),
        strokeWidth = stroke,
        cap = StrokeCap.Butt
    )
    drawLine(
        color = color,
        start = Offset(cx, cy - halfV),
        end = Offset(cx, cy + halfV),
        strokeWidth = stroke,
        cap = StrokeCap.Butt
    )
}

private fun DrawScope.drawAsciiDashes(
    startX: Float,
    endX: Float,
    cy: Float,
    color: Color
) {
    val avail = endX - startX
    if (avail <= 1.5.dp.toPx()) return
    val stroke = 1.15.dp.toPx()
    val dashLen = 3.8.dp.toPx()
    val gapLen = 2.0.dp.toPx()
    val step = dashLen + gapLen
    val count = ((avail + gapLen) / step).toInt().coerceAtLeast(1)
    if (count == 1 && avail < dashLen) {
        drawLine(
            color = color,
            start = Offset(startX, cy),
            end = Offset(endX, cy),
            strokeWidth = stroke,
            cap = StrokeCap.Butt
        )
        return
    }
    val totalSpan = count * dashLen + (count - 1) * gapLen
    var cursor = startX + (avail - totalSpan) / 2f
    repeat(count) {
        drawLine(
            color = color,
            start = Offset(cursor, cy),
            end = Offset(cursor + dashLen, cy),
            strokeWidth = stroke,
            cap = StrokeCap.Butt
        )
        cursor += step
    }
}

private fun DrawScope.drawAsciiVerticalPipes(
    cx: Float,
    topY: Float,
    bottomY: Float,
    color: Color
) {
    val avail = bottomY - topY
    if (avail <= 1.5.dp.toPx()) return
    val stroke = 1.1.dp.toPx()
    val segLen = 5.0.dp.toPx()
    val gapLen = 2.6.dp.toPx()
    val step = segLen + gapLen
    val count = ((avail + gapLen) / step).toInt().coerceAtLeast(1)
    if (count == 1 && avail < segLen) {
        drawLine(
            color = color,
            start = Offset(cx, topY),
            end = Offset(cx, bottomY),
            strokeWidth = stroke,
            cap = StrokeCap.Butt
        )
        return
    }
    val totalSpan = count * segLen + (count - 1) * gapLen
    var cursor = topY + (avail - totalSpan) / 2f
    repeat(count) {
        drawLine(
            color = color,
            start = Offset(cx, cursor),
            end = Offset(cx, cursor + segLen),
            strokeWidth = stroke,
            cap = StrokeCap.Butt
        )
        cursor += step
    }
}

@Composable
fun AsciiHorizontalRule(
    modifier: Modifier = Modifier,
    color: Color = LocalMitiPalette.current.border
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
    ) {
        val pad = 1.dp.toPx()
        drawAsciiDashes(pad, size.width - pad, size.height / 2f, color)
    }
}

@Composable
fun AsciiTableHorizontalBorder(
    columns: Int = 7,
    modifier: Modifier = Modifier,
    color: Color = LocalMitiPalette.current.border
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(11.dp)
    ) {
        val edgeW = ASCII_EDGE_WIDTH.toPx()
        val midY = size.height / 2f
        val padFromPlus = 4.2.dp.toPx()
        val cols = columns.coerceAtLeast(1)
        val totalCellsW = (size.width - (cols + 1) * edgeW).coerceAtLeast(0f)
        val cellW = totalCellsW / cols

        for (i in 0..cols) {
            val cx = edgeW / 2f + i * (cellW + edgeW)
            drawAsciiPlus(cx, midY, color)
            if (i < cols) {
                val nextCx = edgeW / 2f + (i + 1) * (cellW + edgeW)
                drawAsciiDashes(cx + padFromPlus, nextCx - padFromPlus, midY, color)
            }
        }
    }
}

@Composable
fun AsciiVerticalPipe(
    modifier: Modifier = Modifier,
    color: Color = LocalMitiPalette.current.border
) {
    Canvas(
        modifier = modifier
            .width(ASCII_EDGE_WIDTH)
            .fillMaxHeight()
    ) {
        val pad = 1.5.dp.toPx()
        drawAsciiVerticalPipes(size.width / 2f, pad, size.height - pad, color)
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
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = labelColor,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.5.sp,
            lineHeight = 18.sp,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        )
        Box(
            modifier = Modifier
                .background(if (checked) palette.selection else Color.Transparent)
                .padding(horizontal = 4.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (checked) "[*ON*]" else "[OFF]",
                color = if (checked) activeColor else palette.comment,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (checked) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp,
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
    trailingHeader: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalMitiPalette.current
    val borderColor = palette.border
    val sidePipeColor = borderColor.copy(alpha = 0.65f)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.headerBar)
                .drawBehind {
                    val midY = size.height / 2f
                    val edgeW = ASCII_EDGE_WIDTH.toPx()
                    val leftCx = edgeW / 2f
                    val rightCx = size.width - edgeW / 2f
                    val padFromPlus = 4.2.dp.toPx()
                    drawAsciiPlus(leftCx, midY, borderColor)
                    drawAsciiPlus(rightCx, midY, borderColor)
                    drawAsciiDashes(leftCx + padFromPlus, rightCx - padFromPlus, midY, borderColor)
                }
                .padding(vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(19.dp))
                Text(
                    text = header,
                    color = headerColor,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .background(palette.headerBar)
                        .padding(horizontal = 4.dp)
                )
            }
            if (trailingHeader != null) {
                Box(
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .background(palette.headerBar)
                        .padding(horizontal = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    trailingHeader()
                }
            }
            Spacer(modifier = Modifier.width(15.dp))
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val edgeW = ASCII_EDGE_WIDTH.toPx()
                    val padY = 1.5.dp.toPx()
                    drawAsciiVerticalPipes(edgeW / 2f, padY, size.height - padY, sidePipeColor)
                    drawAsciiVerticalPipes(size.width - edgeW / 2f, padY, size.height - padY, sidePipeColor)
                }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            content = content
        )
        AsciiTableHorizontalBorder(columns = 1, color = borderColor)
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
            .border(1.dp, palette.border)
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
