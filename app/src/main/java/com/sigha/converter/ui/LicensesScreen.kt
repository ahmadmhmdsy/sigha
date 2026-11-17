package com.sigha.converter.ui

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sigha.converter.R
import com.sigha.converter.ui.components.SectionHeader
import com.sigha.converter.ui.components.SighaCard
import com.sigha.converter.ui.components.SighaTopAppBar

private data class LicenseLine(val name: String, val license: String)

private val COMPONENTS = listOf(
    LicenseLine("FFmpeg / FFmpegKit", "LGPL-3.0"),
    LicenseLine("Apache PDFBox (Android port)", "Apache-2.0"),
    LicenseLine("Apache Commons Compress", "Apache-2.0"),
    LicenseLine("XZ for Java", "Public domain"),
    LicenseLine("Tesseract4Android", "Apache-2.0"),
    LicenseLine("ONNX Runtime", "MIT"),
    LicenseLine("Coil", "Apache-2.0"),
    LicenseLine("Kotlin / kotlinx.coroutines", "Apache-2.0"),
    LicenseLine("KotlinX Serialization", "Apache-2.0"),
    LicenseLine("Jetpack Compose", "Apache-2.0"),
    LicenseLine("AndroidX libraries", "Apache-2.0"),
    LicenseLine("Material Components", "Apache-2.0"),
    LicenseLine("DataStore Preferences", "Apache-2.0")
)

/** Static attribution list; full texts live with each project. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            SighaTopAppBar(
                title = stringResource(R.string.licenses_title),
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
                    text = stringResource(R.string.licenses_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                SectionHeader(title = stringResource(R.string.licenses_opensource))
            }
            item {
                SighaCard {
                    Column {
                        COMPONENTS.forEachIndexed { index, component ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = component.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = component.license,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (index != COMPONENTS.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }
            item {
                SighaCard {
                    Text(
                        text = stringResource(R.string.licenses_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
