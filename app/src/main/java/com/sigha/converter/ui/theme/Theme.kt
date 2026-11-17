package com.sigha.converter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Lets downstream composables pick accent variants without asking the system again. */
val LocalSighaDarkTheme = staticCompositionLocalOf { false }

private val LightColors = lightColorScheme(
    primary = SighaEmerald,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB8EBDD),
    onPrimaryContainer = Color(0xFF002019),
    secondary = Color(0xFF7A5900),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDF9E),
    onSecondaryContainer = Color(0xFF261900),
    tertiary = SighaInk,
    background = SighaMist,
    surface = Color(0xFFFBFDFC),
    surfaceVariant = Color(0xFFDDE5E0),
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
    secondary = SighaSandLight,
    onSecondary = Color(0xFF3E2E00),
    secondaryContainer = Color(0xFF5C4300),
    onSecondaryContainer = Color(0xFFFFDF9E),
    tertiary = Color(0xFFA9C7FF),
    background = SighaNight,
    surface = Color(0xFF141B18),
    surfaceVariant = Color(0xFF3F4945),
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
    CompositionLocalProvider(LocalSighaDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = SighaTypography,
            content = content
        )
    }
}
