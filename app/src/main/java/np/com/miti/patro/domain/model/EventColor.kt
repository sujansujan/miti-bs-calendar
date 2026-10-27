package np.com.miti.patro.domain.model

import androidx.compose.ui.graphics.Color
import np.com.miti.patro.ui.theme.MitiPalette

/**
 * User-selectable color for repeating BS events.
 * Adapts automatically to Alucard (Light) and Dracula (Dark) palettes,
 * and provides an ARGB integer for Google Calendar event color syncing.
 */
enum class EventColor(
    val key: String,
    val labelEn: String,
    val labelNe: String,
    val alucardHex: Long,
    val draculaHex: Long,
    val gcalArgb: Int
) {
    GREEN(
        key = "GREEN",
        labelEn = "Green",
        labelNe = "हरियो",
        alucardHex = 0xFF14710A,
        draculaHex = 0xFF50FA7B,
        gcalArgb = 0xFF0B8043.toInt()
    ),
    RED(
        key = "RED",
        labelEn = "Red",
        labelNe = "रातो",
        alucardHex = 0xFFCB3A2A,
        draculaHex = 0xFFFF5555,
        gcalArgb = 0xFFD50000.toInt()
    ),
    CYAN(
        key = "CYAN",
        labelEn = "Cyan",
        labelNe = "आकासे",
        alucardHex = 0xFF036A96,
        draculaHex = 0xFF8BE9FD,
        gcalArgb = 0xFF039BE5.toInt()
    ),
    PURPLE(
        key = "PURPLE",
        labelEn = "Purple",
        labelNe = "बैजनी",
        alucardHex = 0xFF644AC9,
        draculaHex = 0xFFBD93F9,
        gcalArgb = 0xFF8E24AA.toInt()
    ),
    ORANGE(
        key = "ORANGE",
        labelEn = "Orange",
        labelNe = "सुन्तला",
        alucardHex = 0xFFA34D14,
        draculaHex = 0xFFFFB86C,
        gcalArgb = 0xFFF4511E.toInt()
    ),
    PINK(
        key = "PINK",
        labelEn = "Pink",
        labelNe = "गुलाबी",
        alucardHex = 0xFFA3144D,
        draculaHex = 0xFFFF79C6,
        gcalArgb = 0xFFD81B60.toInt()
    ),
    YELLOW(
        key = "YELLOW",
        labelEn = "Gold",
        labelNe = "पहेंलो",
        alucardHex = 0xFF846E15,
        draculaHex = 0xFFF1FA8C,
        gcalArgb = 0xFFF6BF26.toInt()
    ),
    TEAL(
        key = "TEAL",
        labelEn = "Teal",
        labelNe = "नीलो-हरियो",
        alucardHex = 0xFF0D7A68,
        draculaHex = 0xFF2EE6C5,
        gcalArgb = 0xFF009688.toInt()
    );

    fun label(useDevanagari: Boolean): String = if (useDevanagari) labelNe else labelEn

    fun composeColor(palette: MitiPalette): Color {
        return Color(if (palette.isDark) draculaHex else alucardHex)
    }

    companion object {
        fun fromKey(key: String?, fallbackPersonalHoliday: Boolean = false): EventColor {
            if (!key.isNullOrBlank()) {
                entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.let { return it }
            }
            return if (fallbackPersonalHoliday) RED else GREEN
        }
    }
}
