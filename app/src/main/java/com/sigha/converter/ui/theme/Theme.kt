package com.sigha.converter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration

/** Lets downstream composables pick accent variants without asking the system again. */
val LocalSighaDarkTheme = staticCompositionLocalOf { false }

/**
 * Every Material 3 role the components actually read is authored here. The container
 * ramp is a neutral green-grey so M3 cards, sheets, dialogs, snackbars and the navigation
 * bar sit on the Sigha palette instead of falling back to the baseline purple scheme.
 */
private val LightColors = lightColorScheme(
    primary = SighaEmerald,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB8EBDD),
    onPrimaryContainer = Color(0xFF002019),
    inversePrimary = SighaEmeraldLight,
    secondary = Color(0xFF7A5900),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDF9E),
    onSecondaryContainer = Color(0xFF261900),
    tertiary = SighaInk,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD8E3EF),
    onTertiaryContainer = Color(0xFF0B1B2B),
    background = SighaMist,
    onBackground = SighaInk,
    surface = Color(0xFFFBFDFC),
    onSurface = SighaInk,
    surfaceVariant = Color(0xFFDDE5E0),
    onSurfaceVariant = Color(0xFF4A5A66),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7FAF8),
    surfaceContainer = Color(0xFFEFF4F1),
    surfaceContainerHigh = Color(0xFFE9EFEB),
    surfaceContainerHighest = Color(0xFFE3EAE5),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFDCE4E0),
    scrim = Color.Black,
    surfaceTint = SighaEmerald,
    inverseSurface = Color(0xFF2B332F),
    inverseOnSurface = Color(0xFFEFF5F1),
    outline = Color(0xFF6F7975),
    outlineVariant = Color(0xFFC3CDC8),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

private val DarkColors = darkColorScheme(
    primary = SighaEmeraldLight,
    onPrimary = Color(0xFF00382E),
    primaryContainer = Color(0xFF005142),
    onPrimaryContainer = Color(0xFFB8EBDD),
    inversePrimary = SighaEmerald,
    secondary = SighaSandLight,
    onSecondary = Color(0xFF3E2E00),
    secondaryContainer = Color(0xFF5C4300),
    onSecondaryContainer = Color(0xFFFFDF9E),
    tertiary = Color(0xFFA9C7FF),
    onTertiary = Color(0xFF10233F),
    tertiaryContainer = Color(0xFF2C3F55),
    onTertiaryContainer = Color(0xFFD8E3EF),
    background = SighaNight,
    onBackground = Color(0xFFE3ECE7),
    surface = Color(0xFF141B18),
    onSurface = Color(0xFFE3ECE7),
    surfaceVariant = Color(0xFF3F4945),
    onSurfaceVariant = Color(0xFFB6C4BE),
    surfaceContainerLowest = Color(0xFF0B100E),
    surfaceContainerLow = Color(0xFF121916),
    surfaceContainer = Color(0xFF1A2521),
    surfaceContainerHigh = Color(0xFF202C27),
    surfaceContainerHighest = Color(0xFF27342E),
    surfaceBright = Color(0xFF2E3A35),
    surfaceDim = Color(0xFF0F1513),
    scrim = Color.Black,
    surfaceTint = SighaEmeraldLight,
    inverseSurface = Color(0xFFE3ECE7),
    inverseOnSurface = Color(0xFF10233F),
    outline = Color(0xFF89938F),
    outlineVariant = Color(0xFF3F4945),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
fun SighaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val locales = LocalConfiguration.current.locales
    val isArabic = locales.size() > 0 && locales[0].language == "ar"
    CompositionLocalProvider(LocalSighaDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = if (isArabic) SighaTypographyArabic else SighaTypography,
            shapes = SighaShapes,
            content = content
        )
    }
}
