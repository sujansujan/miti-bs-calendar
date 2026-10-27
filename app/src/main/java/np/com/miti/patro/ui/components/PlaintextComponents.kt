package np.com.miti.patro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import np.com.miti.patro.ui.theme.LocalMitiPalette

/**
 * Renders a literal ASCII horizontal rule of `-------` characters spanning the full width,
 * inspired by plaintextsports.com scoreboard tables and box-scores.
 */
@Composable
private const val DEFAULT_ASCII_RULE = "+------------------------------------------------------------------------------------------------"
private const val DASH_ONLY_ASCII_RULE = "-------------------------------------------------------------------------------------------------"

@Composable
fun AsciiHorizontalRule(
    modifier: Modifier = Modifier,
    char: Char = '-',
    edgeChar: Char = '+',
    color: Color? = null
) {
    val palette = LocalMitiPalette.current
    val ruleColor = color ?: palette.comment
    val line = when {
        char == '-' && edgeChar == '+' -> DEFAULT_ASCII_RULE
        char == '-' && edgeChar == '-' -> DASH_ONLY_ASCII_RULE
        else -> buildString {
            append(edgeChar)
            repeat(96) { append(char) }
        }
    }
    Text(
        text = line,
        color = ruleColor,
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        lineHeight = 11.sp,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * Renders a 7-column ASCII table border row (`+--------+--------+...`) where each column
 * starts with `+` followed by `-------` so column intersections align cleanly with `|` dividers.
 */
@Composable
fun AsciiTableHorizontalBorder(
    columns: Int = 7,
    color: Color? = null
) {
    val palette = LocalMitiPalette.current
    val borderColor = color ?: palette.comment
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until columns) {
            Text(
                text = "+-------------",
                color = borderColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                lineHeight = 10.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                modifier = Modifier.weight(1f)
            )
        }
        Text(
            text = "+",
            color = borderColor,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            lineHeight = 10.sp
        )
    }
}

/**
 * Plaintext Sports-inspired box container framed by literal `+-- HEADER -------+` and `-------` lines.
 */
@Composable
fun PlaintextBox(
    header: String? = null,
    modifier: Modifier = Modifier,
    borderColor: Color = LocalMitiPalette.current.comment,
    backgroundColor: Color = LocalMitiPalette.current.surface,
    headerText: String? = header,
    headerColor: Color = LocalMitiPalette.current.purple,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalMitiPalette.current
    val effectiveHeader = headerText ?: header
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
    ) {
        if (!effectiveHeader.isNullOrBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.selection.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "+-- $effectiveHeader ",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = headerColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = "--------------------------------------------------------+",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = borderColor,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.weight(1f)
                    )
                }
                AsciiHorizontalRule(color = borderColor)
            }
        } else {
            AsciiHorizontalRule(color = borderColor)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            content = content
        )
        AsciiHorizontalRule(color = borderColor)
    }
}

/**
 * Plaintext bracketed link/action control (e.g., "[+EVENT]", "[CONVERT]", "[TODAY]").
 */
@Composable
fun BracketButton(
    label: String = "",
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = LocalMitiPalette.current.foreground,
    highlighted: Boolean = false,
    text: String = label,
    color: Color = textColor,
    isHighlighted: Boolean = highlighted
) {
    val palette = LocalMitiPalette.current
    val rawLabel = if (text.isNotEmpty()) text else label
    val display = if (rawLabel.startsWith("[") || rawLabel.startsWith("*")) {
        rawLabel
    } else if (isHighlighted) {
        "[*$rawLabel*]"
    } else {
        "[$rawLabel]"
    }
    Text(
        text = display,
        fontFamily = FontFamily.Monospace,
        fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
        fontSize = 11.sp,
        maxLines = 1,
        softWrap = false,
        color = color,
        modifier = modifier
            .background(if (isHighlighted) palette.selection else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 3.dp, vertical = 4.dp)
    )
}

/**
 * Plaintext terminal-style text input with optional label, `> ` prompt, and `-------` bottom border.
 */
@Composable
fun PlaintextTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    isError: Boolean = false,
    singleLine: Boolean = true
) {
    val palette = LocalMitiPalette.current
    val ruleColor = if (isError) palette.red else palette.comment
    Column(modifier = modifier.fillMaxWidth()) {
        if (!label.isNullOrBlank()) {
            Text(
                text = label,
                color = palette.comment,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.surface)
                .padding(top = 6.dp, start = 6.dp, end = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "> ",
                    color = if (isError) palette.red else palette.purple,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Box(modifier = Modifier.weight(1f)) {
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
                        cursorBrush = SolidColor(palette.purple),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            AsciiHorizontalRule(char = '-', edgeChar = '-', color = ruleColor)
        }
    }
}

/**
 * Plaintext toggle row using `[x]` and `[ ]` indicators.
 */
@Composable
fun PlaintextToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    onToggle: ((Boolean) -> Unit)? = onCheckedChange,
    modifier: Modifier = Modifier
) {
    val palette = LocalMitiPalette.current
    val callback = onCheckedChange ?: onToggle ?: {}
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { callback(!checked) }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = palette.foreground,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        )
        Text(
            text = if (checked) "[x]" else "[ ]",
            color = if (checked) palette.green else palette.comment,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            textAlign = TextAlign.End
        )
    }
}
