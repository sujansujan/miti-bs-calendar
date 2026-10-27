package np.com.miti.patro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import np.com.miti.patro.ui.theme.LocalMitiPalette

/**
 * Renders a literal ASCII horizontal rule of `-------` characters spanning the full width,
 * inspired by plaintextsports.com scoreboard tables and box-scores.
 */
@Composable
fun AsciiHorizontalRule(
    modifier: Modifier = Modifier,
    char: Char = '-',
    edgeChar: Char = '+',
    color: Color? = null
) {
    val palette = LocalMitiPalette.current
    val ruleColor = color ?: palette.gridBorder
    val line = buildString {
        append(edgeChar)
        repeat(96) { append(char) }
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
    val borderColor = color ?: palette.gridBorder
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
 * Keep PlaintextDivider as an alias that renders literal `-------` lines so every divider
 * across the entire app uses plain text aesthetics.
 */
@Composable
fun PlaintextDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = 1.dp,
    color: Color? = null
) {
    AsciiHorizontalRule(modifier = modifier, color = color)
}

/**
 * Plaintext bracketed button, e.g. `[< PREV]`, `[TODAY]`, `[+ ADD]`, `[CFG]`.
 * Zero elevation, zero rounded corners, strict monospace typography.
 */
@Composable
fun PlaintextBracketButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color? = null,
    backgroundColor: Color = Color.Transparent,
    bold: Boolean = true
) {
    val palette = LocalMitiPalette.current
    Box(
        modifier = modifier
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor ?: palette.accentCyan,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            fontSize = 12.sp
        )
    }
}

/**
 * Framed ASCII box with a top `+--- TITLE -------+` header rule and a bottom `+-------+` rule.
 */
@Composable
fun PlaintextBox(
    modifier: Modifier = Modifier,
    headerTitle: String? = null,
    headerRight: String? = null,
    content: @Composable () -> Unit
) {
    val palette = LocalMitiPalette.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.surface)
    ) {
        if (headerTitle != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surfaceElevated)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "+-- $headerTitle ",
                        color = palette.accentPurple,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (headerRight != null) {
                        Text(
                            text = " $headerRight --+",
                            color = palette.comment,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    } else {
                        Text(
                            text = "------------------------------------------------+",
                            color = palette.gridBorder,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                AsciiHorizontalRule()
            }
        } else {
            AsciiHorizontalRule()
        }
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            content()
        }
        AsciiHorizontalRule()
    }
}

/**
 * Plaintext terminal-style text input with `> ` prompt and `-------` bottom border.
 */
@Composable
fun PlaintextTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true
) {
    val palette = LocalMitiPalette.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.surfaceElevated)
            .padding(top = 6.dp, start = 6.dp, end = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "> ",
                color = palette.accentPurple,
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
                    cursorBrush = SolidColor(palette.accentPurple),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        AsciiHorizontalRule(char = '-', edgeChar = '-')
    }
}

/**
 * Plaintext toggle row using `[x]` and `[ ]` indicators.
 */
@Composable
fun PlaintextToggleRow(
    label: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalMitiPalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = palette.foreground,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        )
        Text(
            text = if (checked) "[x]" else "[ ]",
            color = if (checked) palette.accentGreen else palette.comment,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            textAlign = TextAlign.End
        )
    }
}
