package com.sigha.converter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sigha.converter.R
import com.sigha.converter.ui.components.SighaCard
import com.sigha.converter.ui.components.SighaTopAppBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Privacy screen (WS4).
 *
 * The policy is a single Markdown document shipped as an asset so that the text in the app and the
 * text hosted on the public URL cannot drift: app/src/main/assets/legal/privacy-policy.md is the
 * same content as docs/legal/privacy-policy.md. It is rendered with a deliberately small parser —
 * headings, bullets and paragraphs — because the document is written for this screen, not the other
 * way round.
 */
private sealed interface PolicyBlock {
    data class Heading(val text: String) : PolicyBlock
    data class Subheading(val text: String) : PolicyBlock
    data class Paragraph(val text: String) : PolicyBlock
    data class Bullet(val text: String) : PolicyBlock
}

private const val POLICY_ASSET = "legal/privacy-policy.md"

private val BOLD = Regex("\\*\\*(.+?)\\*\\*")
private val INLINE_CODE = Regex("\\x60([^\\x60]+)\\x60")
private val LINK = Regex("\\[([^\\]]+)\\]\\(([^)]+)\\)")

private fun cleanInline(input: String): String {
    var text = BOLD.replace(input) { match -> match.groupValues[1] }
    text = INLINE_CODE.replace(text) { match -> match.groupValues[1] }
    text = LINK.replace(text) { match ->
        match.groupValues[1] + " (" + match.groupValues[2] + ")"
    }
    return text.trim()
}

private fun parsePolicy(markdown: String): List<PolicyBlock> {
    val blocks = mutableListOf<PolicyBlock>()
    val paragraph = StringBuilder()
    fun flushParagraph() {
        if (paragraph.isNotBlank()) {
            blocks += PolicyBlock.Paragraph(paragraph.toString().trim())
        }
        paragraph.clear()
    }
    markdown.lineSequence().forEach { raw ->
        val line = raw.trimEnd()
        when {
            line.isBlank() -> flushParagraph()
            line.startsWith("## ") -> {
                flushParagraph()
                blocks += PolicyBlock.Subheading(cleanInline(line.removePrefix("## ")))
            }
            line.startsWith("# ") -> {
                flushParagraph()
                blocks += PolicyBlock.Heading(cleanInline(line.removePrefix("# ")))
            }
            line.startsWith("- ") -> {
                flushParagraph()
                blocks += PolicyBlock.Bullet(cleanInline(line.removePrefix("- ")))
            }
            line.startsWith("<!--") -> Unit
            else -> {
                if (paragraph.isNotEmpty()) paragraph.append(' ')
                paragraph.append(cleanInline(line))
            }
        }
    }
    flushParagraph()
    return blocks
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val markdown by produceState<String?>(initialValue = null, key1 = POLICY_ASSET) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.applicationContext.assets
                    .open(POLICY_ASSET)
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }
            }.getOrNull()
        }
    }
    val blocks = remember(markdown) { markdown?.let(::parsePolicy).orEmpty() }

    Scaffold(
        modifier = modifier,
        topBar = {
            SighaTopAppBar(
                title = stringResource(R.string.legal_privacy_title),
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
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                HeadlineCard()
            }
            val current = markdown
            if (current == null) {
                item {
                    Text(
                        text = stringResource(R.string.legal_privacy_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (blocks.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.legal_privacy_load_error),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                items(
                    count = blocks.size,
                    key = { index -> index }
                ) { index ->
                    when (val block = blocks[index]) {
                        is PolicyBlock.Heading -> Text(
                            text = block.text,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        is PolicyBlock.Subheading -> Text(
                            text = block.text,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        is PolicyBlock.Paragraph -> Text(
                            text = block.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        is PolicyBlock.Bullet -> Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "\u2022",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = block.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeadlineCard() {
    SighaCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.legal_privacy_headline),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.legal_privacy_effective),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
