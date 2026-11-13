package np.com.miti.patro.domain.model

import androidx.compose.ui.graphics.Color
import np.com.miti.patro.ui.theme.MitiPalette

/**
 * 8 semantic event colors harmonized across both Alucard (Light) and Dracula (Dark) palettes.
 */
enum class EventColor(
    val key: String,
    val labelEn: String,
    val labelNe: String
) {
    GREEN("GREEN", "Green", "हरियो"),
    RED("RED", "Red", "रातो"),
    CYAN("CYAN", "Cyan", "नीलो"),
    PURPLE("PURPLE", "Purple", "बैजनी"),
    ORANGE("ORANGE", "Orange", "सुन्तला"),
    PINK("PINK", "Pink", "गुलाबी"),
    YELLOW("YELLOW", "Yellow", "पहेँलो"),
    TEAL("TEAL", "Teal", "फिरोजा");

    fun composeColor(palette: MitiPalette): Color = when (this) {
        GREEN -> palette.green
        RED -> palette.red
        CYAN -> palette.cyan
        PURPLE -> palette.purple
        ORANGE -> palette.orange
        PINK -> palette.pink
        YELLOW -> palette.yellow
        TEAL -> if (palette.isDark) Color(0xFF66D9EF) else Color(0xFF007A78)
    }

    companion object {
        fun fromKey(key: String?, fallbackPersonalHoliday: Boolean = false): EventColor {
            if (!key.isNullOrBlank()) {
                entries.find { it.key.equals(key, ignoreCase = true) }?.let { return it }
            }
            return if (fallbackPersonalHoliday) RED else GREEN
        }
    }
}
