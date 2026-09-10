package com.exchangerates.app.presentation.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.exchangerates.app.R
import com.exchangerates.app.core.theme.AppTheme
import com.exchangerates.app.domain.model.RateMode
import com.exchangerates.app.domain.model.SourceStatus
import com.exchangerates.app.presentation.util.formatDateTime
import com.exchangerates.app.presentation.util.rateModeTitle
import com.exchangerates.app.presentation.util.rememberAppLocale
import java.util.Locale

/**
 * Диалог «Об источниках»: переключатель режима курса (рыночная середина или
 * официальный курс центробанка) и состояние каждого источника с временем
 * последнего успешного ответа. Здесь же показывается требуемая атрибуция.
 */
@Composable
fun SourcesDialog(
    statuses: List<SourceStatus>,
    rateMode: RateMode,
    onRateModeChange: (RateMode) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = AppTheme.colors
    val locale = rememberAppLocale()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textSecondary,
        title = {
            Text(
                text = stringResource(R.string.sources_title),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = stringResource(R.string.rate_mode_section),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary,
                )
                Text(
                    text = stringResource(R.string.rate_mode_mid_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textTertiary,
                )
                Spacer(Modifier.height(6.dp))
                RateMode.entries.forEach { mode ->
                    RateModeRow(
                        title = rateModeTitle(mode),
                        selected = mode == rateMode,
                        onClick = { onRateModeChange(mode) },
                    )
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.sources_section),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary,
                )
                Spacer(Modifier.height(6.dp))

                if (statuses.isEmpty()) {
                    Text(
                        text = stringResource(R.string.sources_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textTertiary,
                    )
                } else {
                    statuses.sortedByDescending { it.usedFor }.forEach { status ->
                        SourceRow(status = status, locale = locale)
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textTertiary,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.close), color = colors.accent)
            }
        },
    )
}

@Composable
private fun RateModeRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(
                    if (selected) colors.accent else colors.surfaceElevated,
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textPrimary,
        )
    }
}

@Composable
private fun SourceRow(status: SourceStatus, locale: Locale) {
    val colors = AppTheme.colors
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        when {
                            !status.isOk -> colors.negative
                            status.usedFor > 0 -> colors.positive
                            else -> colors.textTertiary
                        },
                        CircleShape,
                    ),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = status.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            if (status.usedFor > 0) {
                Text(
                    text = stringResource(R.string.sources_used_for, status.usedFor),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }
        val detail = when {
            status.error != null -> stringResource(R.string.sources_error, status.error)
            status.asOf != null -> formatDateTime(status.asOf, locale)
            else -> stringResource(R.string.sources_no_data)
        }
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = if (status.error != null) colors.negative else colors.textTertiary,
            )
        }
        if (status.attribution != null) {
            Text(
                text = status.attribution,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textTertiary,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
    }
}
