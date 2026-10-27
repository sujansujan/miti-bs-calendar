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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import np.com.miti.patro.data.preferences.ThemeMode

private val PlaintextTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 22.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 19.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 12.sp
    )
)

@Composable
fun MitiTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
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
            onBackground = palette.foreground,
            onSurface = palette.foreground,
            primary = palette.purple,
            onPrimary = palette.background,
            secondary = palette.cyan,
            error = palette.red
        )
    } else {
        lightColorScheme(
            background = palette.background,
            surface = palette.surface,
            onBackground = palette.foreground,
            onSurface = palette.foreground,
            primary = palette.purple,
            onPrimary = palette.background,
            secondary = palette.cyan,
            error = palette.red
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = palette.background.toArgb()
            window.navigationBarColor = palette.background.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !useDark
            insetsController.isAppearanceLightNavigationBars = !useDark
        }
    }

    CompositionLocalProvider(LocalMitiPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = PlaintextTypography,
            content = content
        )
    }
}
