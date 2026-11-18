package com.sigha.converter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sigha.converter.R
import com.sigha.converter.work.QueueStatus

/**
 * Single-line top bar with an optional back arrow; the title stays on the start edge in RTL.
 * It deliberately carries one line: a second line does not fit a Material 3 top bar once the
 * font scale is raised, so secondary copy belongs in the content instead.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SighaTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        modifier = modifier,
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.action_back)
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
        trailing?.invoke()
    }
}

/**
 * Compose-shape empty state: no bitmap assets, scales with the theme and RTL.
 * Below 480dp of window height (landscape phones) it switches to a compact variant so the
 * whole state — title, body and action — still fits on screen instead of dead-ending above
 * the fold.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    footnote: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    val compact = LocalConfiguration.current.screenHeightDp < 480
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (compact) 24.dp else 32.dp,
                vertical = if (compact) 12.dp else 40.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 56.dp else 112.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(if (compact) 16.dp else 32.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(if (compact) 26.dp else 48.dp)
            )
        }
        Spacer(Modifier.height(if (compact) 8.dp else 24.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(if (compact) 6.dp else 12.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (action != null) {
            Spacer(Modifier.height(if (compact) 12.dp else 28.dp))
            action()
        }
        if (footnote != null) {
            Spacer(Modifier.height(if (compact) 8.dp else 20.dp))
            Text(
                text = footnote,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun statusLabel(status: QueueStatus): String = stringResource(
    when (status) {
        QueueStatus.QUEUED -> R.string.status_queued
        QueueStatus.RUNNING -> R.string.status_running
        QueueStatus.DONE -> R.string.status_done
        QueueStatus.FAILED -> R.string.status_failed
        QueueStatus.CANCELLED -> R.string.status_cancelled
    }
)

@Composable
fun statusColor(status: QueueStatus): Color = when (status) {
    QueueStatus.DONE -> MaterialTheme.colorScheme.primary
    QueueStatus.FAILED -> MaterialTheme.colorScheme.error
    QueueStatus.CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant
    QueueStatus.QUEUED, QueueStatus.RUNNING -> MaterialTheme.colorScheme.secondary
}

/**
 * Status chip on tonal Material 3 containers rather than accent-on-its-own-tint: the tone
 * pairs measure 5.8:1-13.4:1 across both themes (the old tinted-accent version bottomed out at
 * 3.99:1 in light theme). The label stays plain text: a stateDescription duplicating it made
 * TalkBack read the status twice.
 */
@Composable
fun StatusPill(status: QueueStatus, modifier: Modifier = Modifier) {
    val label = statusLabel(status)
    val (container, content) = when (status) {
        QueueStatus.DONE -> MaterialTheme.colorScheme.primaryContainer to
            MaterialTheme.colorScheme.onPrimaryContainer
        QueueStatus.FAILED -> MaterialTheme.colorScheme.errorContainer to
            MaterialTheme.colorScheme.onErrorContainer
        QueueStatus.CANCELLED -> MaterialTheme.colorScheme.surfaceContainerHighest to
            MaterialTheme.colorScheme.onSurfaceVariant
        QueueStatus.QUEUED, QueueStatus.RUNNING -> MaterialTheme.colorScheme.secondaryContainer to
            MaterialTheme.colorScheme.onSecondaryContainer
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = content,
        modifier = modifier
            .background(container, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissLabel) }
        }
    )
}
