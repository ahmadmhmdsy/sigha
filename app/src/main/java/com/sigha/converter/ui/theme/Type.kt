package com.sigha.converter.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * System font stack keeps Arabic and Latin glyphs consistent on every OEM skin.
 * Only weights and rhythm are tuned: a slightly tighter display face and calmer body copy.
 */
val SighaTypography = Typography(
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.4).sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.2).sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.15.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.2.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.4.sp
    )
)

/**
 * Arabic companion of [SighaTypography], selected by [SighaTheme] when the resolved
 * locale is Arabic. Arabic is cursive and mark-rich: the Latin tracking values break
 * glyph joins and the Latin leading crowds diacritics, so every one of the fifteen M3
 * styles drops to zero letter spacing and opens its line height by 15%.
 */
val SighaTypographyArabic = Typography(
    displayLarge = SighaTypography.displayLarge.arabic(),
    displayMedium = SighaTypography.displayMedium.arabic(),
    displaySmall = SighaTypography.displaySmall.arabic(),
    headlineLarge = SighaTypography.headlineLarge.arabic(),
    headlineMedium = SighaTypography.headlineMedium.arabic(),
    headlineSmall = SighaTypography.headlineSmall.arabic(),
    titleLarge = SighaTypography.titleLarge.arabic(),
    titleMedium = SighaTypography.titleMedium.arabic(),
    titleSmall = SighaTypography.titleSmall.arabic(),
    bodyLarge = SighaTypography.bodyLarge.arabic(),
    bodyMedium = SighaTypography.bodyMedium.arabic(),
    bodySmall = SighaTypography.bodySmall.arabic(),
    labelLarge = SighaTypography.labelLarge.arabic(),
    labelMedium = SighaTypography.labelMedium.arabic(),
    labelSmall = SighaTypography.labelSmall.arabic()
)

private fun TextStyle.arabic(): TextStyle = copy(
    letterSpacing = 0.sp,
    lineHeight = if (lineHeight == TextUnit.Unspecified) lineHeight else lineHeight * 1.15f
)
