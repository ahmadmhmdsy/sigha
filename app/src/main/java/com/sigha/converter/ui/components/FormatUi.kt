package com.sigha.converter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.TextSnippet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sigha.converter.R
import com.sigha.converter.core.FileFormat
import com.sigha.converter.core.FormatCategory
import com.sigha.converter.ui.theme.LocalSighaDarkTheme

/** Category accent that stays readable in both light and dark themes. */
@Composable
fun categoryAccent(category: FormatCategory): Color =
    if (LocalSighaDarkTheme.current) categoryAccentDark(category) else categoryAccentLight(category)

fun categoryAccentLight(category: FormatCategory): Color = when (category) {
    FormatCategory.IMAGE -> Color(0xFF0E7C66)
    FormatCategory.PDF -> Color(0xFFB33427)
    FormatCategory.DOCUMENT -> Color(0xFF3459B5)
    FormatCategory.SPREADSHEET -> Color(0xFF1E7A3C)
    FormatCategory.PRESENTATION -> Color(0xFFB05C14)
    FormatCategory.TEXT -> Color(0xFF52606B)
    FormatCategory.AUDIO -> Color(0xFF7B3FA0)
    FormatCategory.VIDEO -> Color(0xFF1E6FA8)
    FormatCategory.ARCHIVE -> Color(0xFF8C6100)
    FormatCategory.DATA -> Color(0xFF44566A)
    FormatCategory.SUBTITLE -> Color(0xFFA94A2F)
    FormatCategory.EBOOK -> Color(0xFF2F7D6D)
    FormatCategory.AI -> Color(0xFF6248C8)
}

fun categoryAccentDark(category: FormatCategory): Color = when (category) {
    FormatCategory.IMAGE -> Color(0xFF7BD8C1)
    FormatCategory.PDF -> Color(0xFFFFA79C)
    FormatCategory.DOCUMENT -> Color(0xFFAFC8FF)
    FormatCategory.SPREADSHEET -> Color(0xFF9BE0B2)
    FormatCategory.PRESENTATION -> Color(0xFFF5BD8C)
    FormatCategory.TEXT -> Color(0xFFC6D0D9)
    FormatCategory.AUDIO -> Color(0xFFD9B0F2)
    FormatCategory.VIDEO -> Color(0xFF96CFF2)
    FormatCategory.ARCHIVE -> Color(0xFFEFCB7C)
    FormatCategory.DATA -> Color(0xFFBCC9D6)
    FormatCategory.SUBTITLE -> Color(0xFFF3B29C)
    FormatCategory.EBOOK -> Color(0xFFA4E0D2)
    FormatCategory.AI -> Color(0xFFCBB9FF)
}

fun FormatCategory.icon(): ImageVector = when (this) {
    FormatCategory.IMAGE -> Icons.Outlined.Image
    FormatCategory.PDF -> Icons.Outlined.PictureAsPdf
    FormatCategory.DOCUMENT -> Icons.Outlined.Description
    FormatCategory.SPREADSHEET -> Icons.Outlined.TableChart
    FormatCategory.PRESENTATION -> Icons.Outlined.Slideshow
    FormatCategory.TEXT -> Icons.Outlined.TextSnippet
    FormatCategory.AUDIO -> Icons.Outlined.AudioFile
    FormatCategory.VIDEO -> Icons.Outlined.Movie
    FormatCategory.ARCHIVE -> Icons.Outlined.FolderZip
    FormatCategory.DATA -> Icons.Outlined.DataObject
    FormatCategory.SUBTITLE -> Icons.Outlined.Subtitles
    FormatCategory.EBOOK -> Icons.Outlined.MenuBook
    FormatCategory.AI -> Icons.Outlined.AutoAwesome
}

fun FileFormat.mimeOrDefault(): String = mime.ifBlank { "application/octet-stream" }

/** Rounded, tinted format tile used in file cards and result rows. */
@Composable
fun FormatIcon(format: FileFormat, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    val accent = categoryAccent(format.category)
    Box(
        modifier = modifier
            .size(size)
            .background(accent.copy(alpha = 0.14f), RoundedCornerShape(size / 3)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = format.category.icon(),
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

/**
 * Small "JPG"/"PDF" chip on the category accent's tint. The accent fills the background but
 * the text is neutral: accent-on-its-own-tint only reached 3.76-4.37:1 in light theme, below
 * the 4.5:1 WCAG AA floor for 12sp text. The 44dp [FormatIcon] keeps carrying the hue.
 */
@Composable
fun FormatBadge(format: FileFormat, modifier: Modifier = Modifier) {
    val accent = categoryAccent(format.category)
    Text(
        text = format.label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .background(accent.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
fun categoryLabel(category: FormatCategory): String = stringResource(
    when (category) {
        FormatCategory.IMAGE -> R.string.category_image
        FormatCategory.PDF -> R.string.category_pdf
        FormatCategory.DOCUMENT -> R.string.category_document
        FormatCategory.SPREADSHEET -> R.string.category_spreadsheet
        FormatCategory.PRESENTATION -> R.string.category_presentation
        FormatCategory.TEXT -> R.string.category_text
        FormatCategory.AUDIO -> R.string.category_audio
        FormatCategory.VIDEO -> R.string.category_video
        FormatCategory.ARCHIVE -> R.string.category_archive
        FormatCategory.DATA -> R.string.category_data
        FormatCategory.SUBTITLE -> R.string.category_subtitle
        FormatCategory.EBOOK -> R.string.category_ebook
        FormatCategory.AI -> R.string.category_ai
    }
)
