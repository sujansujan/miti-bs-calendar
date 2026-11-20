package np.com.miti.patro.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import np.com.miti.patro.R
import np.com.miti.patro.data.preferences.ThemeMode

val MitiMonospaceFont: FontFamily = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold)
)

private val MonospaceTypography = Typography(
    bodyLarge = TextStyle(fontFamily = MitiMonospaceFont, fontSize = 14.sp, lineHeight = 20.sp),
    bodyMedium = TextStyle(fontFamily = MitiMonospaceFont, fontSize = 13.sp, lineHeight = 19.sp),
    bodySmall = TextStyle(fontFamily = MitiMonospaceFont, fontSize = 11.sp, lineHeight = 16.sp),
    titleLarge = TextStyle(fontFamily = MitiMonospaceFont, fontSize = 18.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = MitiMonospaceFont, fontSize = 15.sp, lineHeight = 21.sp),
    labelMedium = TextStyle(fontFamily = MitiMonospaceFont, fontSize = 12.sp, lineHeight = 17.sp)
)

@Composable
fun MitiTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.ALUCARD -> false
        ThemeMode.DRACULA -> true
    }

    val palette = if (useDark) DraculaPalette else AlucardPalette

    val colorScheme = if (useDark) {
        darkColorScheme(
            background = palette.background,
            surface = palette.surface,
            primary = palette.purple,
            secondary = palette.cyan,
            tertiary = palette.green,
            error = palette.red,
            onBackground = palette.foreground,
            onSurface = palette.foreground
        )
    } else {
        lightColorScheme(
            background = palette.background,
            surface = palette.surface,
            primary = palette.purple,
            secondary = palette.cyan,
            tertiary = palette.green,
            error = palette.red,
            onBackground = palette.foreground,
            onSurface = palette.foreground
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = palette.background.toArgb()
            window.navigationBarColor = palette.background.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !useDark
            controller.isAppearanceLightNavigationBars = !useDark
        }
    }

    CompositionLocalProvider(LocalMitiPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MonospaceTypography,
            content = content
        )
    }
}
