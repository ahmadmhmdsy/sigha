package com.sigha.converter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sigha.converter.R
import com.sigha.converter.ui.components.SectionHeader
import com.sigha.converter.ui.components.SighaCard
import com.sigha.converter.ui.components.SighaTopAppBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Attribution screen (WS4).
 *
 * The list is the shipped set, not a marketing list: every row below corresponds to code or a
 * native library inside the APK, and every licence text is bundled under assets/licenses/ and
 * rendered offline. FFmpegKit/FFmpeg are served by the LGPL sections, including the written offer
 * for Corresponding Source and the relink instructions, because a signed APK is a distribution of
 * an LGPL library and the licence duties attach to that binary.
 */
private data class LicenceEntry(
    val name: String,
    val licence: String?,
    val asset: String?,
    val unresolved: Boolean = false
)

private const val LICENCES = "licenses/"
private const val FFMPEG_KIT = "licenses/ffmpeg-kit/"

private val LGPL_COMPONENTS = listOf(
    LicenceEntry(
        name = "FFmpegKit Full 8.1.9 (maintained fork)",
        licence = "LGPL-3.0",
        asset = FFMPEG_KIT + "FFmpegKit-LGPL-3.0.txt"
    ),
    LicenceEntry(
        name = "FFmpeg (libavcodec, libavformat, libavfilter, libavutil, libavdevice, libswresample, libswscale)",
        licence = "LGPL-2.1-or-later",
        asset = LICENCES + "LGPL-2.1.txt"
    ),
    LicenceEntry(
        name = "libsndfile",
        licence = "LGPL-2.1-or-later",
        asset = FFMPEG_KIT + "license_libsndfile.txt"
    ),
    LicenceEntry(
        name = "fribidi",
        licence = "LGPL-2.1-or-later",
        asset = FFMPEG_KIT + "license_fribidi.txt"
    ),
    LicenceEntry(
        name = "soxr",
        licence = "LGPL-2.1-or-later",
        asset = FFMPEG_KIT + "license_soxr.txt"
    ),
    LicenceEntry(
        name = "twoLAME",
        licence = "LGPL-2.1-or-later",
        asset = FFMPEG_KIT + "license_twolame.txt"
    ),
    LicenceEntry(
        name = "libiconv",
        licence = "LGPL-2.1 (bundled text: GPL-3.0 package COPYING)",
        asset = FFMPEG_KIT + "license_libiconv.txt"
    ),
    LicenceEntry(
        name = "LAME",
        licence = "LGPL-2.0",
        asset = FFMPEG_KIT + "license_lame.txt"
    ),
    LicenceEntry(
        name = "Shine",
        licence = "LGPL-2.0",
        asset = FFMPEG_KIT + "license_shine.txt"
    )
)

private val COMPONENTS = listOf(
    LicenceEntry("PDFBox-Android 2.0.27.0 and FontBox", "Apache-2.0", LICENCES + "Apache-2.0.txt"),
    LicenceEntry(
        "LiberationSans-Regular.ttf (bundled by PDFBox)",
        "SIL OFL 1.1",
        LICENCES + "SIL-OFL-1.1.txt"
    ),
    LicenceEntry("Apache Commons Compress 1.28.0", "Apache-2.0", LICENCES + "Apache-2.0.txt"),
    LicenceEntry(
        "Apache Commons IO / Codec / Lang3 (transitive)",
        "Apache-2.0",
        LICENCES + "Apache-2.0.txt"
    ),
    LicenceEntry("XZ for Java 1.12", "0BSD", LICENCES + "BSD-0-Clause-XZ-for-Java.txt"),
    LicenceEntry("Tesseract4Android 4.9.0 (libtesseract)", "Apache-2.0", LICENCES + "Apache-2.0.txt"),
    LicenceEntry("Leptonica (libleptonica)", "BSD-2-Clause", LICENCES + "BSD-2-Clause-leptonica.txt"),
    LicenceEntry(
        "libjpeg-turbo (libjpeg)",
        "IJG / BSD-3-Clause / zlib",
        LICENCES + "IJG-libjpeg-turbo-LICENSE.md"
    ),
    LicenceEntry("libpng (libpngx)", "libpng licence", FFMPEG_KIT + "license_libpng.txt"),
    LicenceEntry("Bouncy Castle 1.72 (via PDFBox)", "MIT", LICENCES + "MIT-BouncyCastle.txt"),
    LicenceEntry(
        "Kotlin, kotlinx.coroutines, kotlinx.serialization",
        "Apache-2.0",
        LICENCES + "Apache-2.0.txt"
    ),
    LicenceEntry(
        "AndroidX, Jetpack Compose, Material 3",
        "Apache-2.0",
        LICENCES + "Apache-2.0.txt"
    ),
    LicenceEntry("Coil 2.7.0 (with OkHttp / Okio)", "Apache-2.0", LICENCES + "Apache-2.0.txt"),
    LicenceEntry(
        name = "smart-exception-java 0.2.1 (via FFmpegKit)",
        licence = null,
        asset = null,
        unresolved = true
    )
)

private val FFMPEG_LIBRARIES = listOf(
    LicenceEntry("cpu_features", "Apache-2.0", FFMPEG_KIT + "license_cpu_features.txt"),
    LicenceEntry("dav1d", "BSD-2-Clause", FFMPEG_KIT + "license_dav1d.txt"),
    LicenceEntry("expat", "MIT", FFMPEG_KIT + "license_expat.txt"),
    LicenceEntry("fontconfig", "MIT-style", FFMPEG_KIT + "license_fontconfig.txt"),
    LicenceEntry("freetype", "FreeType Licence / GPL-2.0", FFMPEG_KIT + "license_freetype.txt"),
    LicenceEntry("harfbuzz", "Old MIT", FFMPEG_KIT + "license_harfbuzz.txt"),
    LicenceEntry("kvazaar", "BSD-3-Clause", FFMPEG_KIT + "license_kvazaar.txt"),
    LicenceEntry("libaom", "BSD-3-Clause", FFMPEG_KIT + "license_libaom.txt"),
    LicenceEntry("libass", "ISC", FFMPEG_KIT + "license_libass.txt"),
    LicenceEntry("libilbc", "BSD-3-Clause", FFMPEG_KIT + "license_libilbc.txt"),
    LicenceEntry("libogg", "BSD-3-Clause", FFMPEG_KIT + "license_libogg.txt"),
    LicenceEntry("libtheora", "BSD-3-Clause", FFMPEG_KIT + "license_libtheora.txt"),
    LicenceEntry("libuuid", "Modified BSD", LICENCES + "BSD-3-Clause.txt"),
    LicenceEntry("libvorbis", "BSD-3-Clause", FFMPEG_KIT + "license_libvorbis.txt"),
    LicenceEntry("libvpx", "BSD-3-Clause", FFMPEG_KIT + "license_libvpx.txt"),
    LicenceEntry("libxml2", "MIT", FFMPEG_KIT + "license_libxml2.txt"),
    LicenceEntry("opencore-amr", "Apache-2.0", FFMPEG_KIT + "license_opencore_amr.txt"),
    LicenceEntry(
        "openh264",
        "BSD-2-Clause + Cisco AVC/H.264 patent licence",
        FFMPEG_KIT + "license_openh264.txt"
    ),
    LicenceEntry("opus", "BSD-3-Clause", FFMPEG_KIT + "license_opus.txt"),
    LicenceEntry("SDL", "zlib licence", FFMPEG_KIT + "license_sdl.txt"),
    LicenceEntry("snappy", "BSD-3-Clause", FFMPEG_KIT + "license_snappy.txt"),
    LicenceEntry("speex", "BSD-3-Clause", FFMPEG_KIT + "license_speex.txt"),
    LicenceEntry("vo-amrwbenc", "Apache-2.0", FFMPEG_KIT + "license_vo_amrwbenc.txt"),
    LicenceEntry("zimg", "WTFPL", FFMPEG_KIT + "license_zimg.txt")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            SighaTopAppBar(
                title = stringResource(R.string.legal_licenses_title),
                onBack = onBack
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.legal_licenses_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                SighaCard {
                    ExpandableBlock(
                        title = stringResource(R.string.legal_licenses_full_notice),
                        subtitle = null,
                        asset = "licenses/THIRD-PARTY-NOTICES.txt",
                        showDivider = false
                    )
                }
            }
            item {
                SectionHeader(title = stringResource(R.string.legal_licenses_section_rights))
            }
            item {
                LicenceCard(LGPL_COMPONENTS)
            }
            item {
                SighaCard {
                    Column {
                        ExpandableBlock(
                            title = stringResource(R.string.legal_licenses_written_offer),
                            subtitle = null,
                            asset = "legal/source-offer.txt",
                            showDivider = true
                        )
                        ExpandableBlock(
                            title = stringResource(R.string.legal_licenses_relink),
                            subtitle = null,
                            asset = "legal/relink-instructions.txt",
                            showDivider = true
                        )
                        Text(
                            text = stringResource(R.string.legal_licenses_no_warranty),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                        )
                        Text(
                            text = stringResource(R.string.legal_licenses_no_warranty_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                SectionHeader(title = stringResource(R.string.legal_licenses_section_components))
            }
            item {
                LicenceCard(COMPONENTS)
            }
            item {
                SectionHeader(title = stringResource(R.string.legal_licenses_section_ffmpeg_libs))
            }
            item {
                LicenceCard(FFMPEG_LIBRARIES)
            }
            item {
                Text(
                    text = stringResource(R.string.legal_licenses_contact),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun LicenceCard(entries: List<LicenceEntry>) {
    SighaCard {
        Column {
            entries.forEachIndexed { index, entry ->
                ExpandableBlock(
                    title = entry.name,
                    subtitle = entry.licence,
                    asset = entry.asset,
                    unresolved = entry.unresolved,
                    showDivider = index != entries.lastIndex
                )
            }
        }
    }
}

@Composable
private fun ExpandableBlock(
    title: String,
    subtitle: String?,
    asset: String?,
    showDivider: Boolean,
    unresolved: Boolean = false
) {
    var expanded by rememberSaveable(title) { mutableStateOf(false) }
    val expandable = asset != null || unresolved
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (expandable) {
                        Modifier.clickable(role = Role.Button) { expanded = !expanded }
                    } else {
                        Modifier
                    }
                )
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyMedium)
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (unresolved) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                } else if (unresolved) {
                    Text(
                        text = stringResource(R.string.legal_licenses_unresolved),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            if (expandable) {
                Icon(
                    imageVector = if (expanded) {
                        Icons.Outlined.KeyboardArrowUp
                    } else {
                        Icons.Outlined.KeyboardArrowDown
                    },
                    contentDescription = stringResource(
                        if (expanded) {
                            R.string.legal_licenses_collapse
                        } else {
                            R.string.legal_licenses_expand
                        }
                    ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (expanded) {
            if (asset == null) {
                Text(
                    text = stringResource(R.string.legal_licenses_unresolved_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            } else {
                LicenceTextView(assetPath = asset)
            }
        }
        if (showDivider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun LicenceTextView(assetPath: String) {
    val context = LocalContext.current
    val text by produceState<String?>(initialValue = null, key1 = assetPath) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.applicationContext.assets
                    .open(assetPath)
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }
            }.getOrNull()
        }
    }
    val loaded = text
    when {
        loaded == null -> Text(
            text = stringResource(R.string.legal_licenses_loading),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        loaded.isBlank() -> Text(
            text = stringResource(R.string.legal_licenses_load_error),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        else -> SelectionContainer {
            Text(
                text = loaded,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp)
            )
        }
    }
}
