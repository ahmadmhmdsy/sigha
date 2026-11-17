package com.sigha.converter.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * One shape ramp for the whole app: every Material 3 component and every Sigha surface
 * reads these tokens, so cards, dialogs, chips, sheets and the navigation bar share the
 * same corner family instead of each screen hard-coding its own radius.
 */
val SighaShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
