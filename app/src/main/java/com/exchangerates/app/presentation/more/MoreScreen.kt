package com.exchangerates.app.presentation.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.exchangerates.app.BuildConfig
import com.exchangerates.app.R
import com.exchangerates.app.core.theme.AppDimens
import com.exchangerates.app.core.theme.AppTheme
import com.exchangerates.app.core.theme.ThemeMode
import com.exchangerates.app.data.local.AppLanguage
import com.exchangerates.app.data.local.AppSettings
import com.exchangerates.app.domain.model.RateMode
import com.exchangerates.app.presentation.util.rateModeTitle

@Composable
fun MoreScreen(
    state: MoreUiState,
    onTheme: (ThemeMode) -> Unit,
    onLanguage: (AppLanguage) -> Unit,
    onDecimals: (Int) -> Unit,
    onGrouping: (Boolean) -> Unit,
    onShowChange: (Boolean) -> Unit,
    onRateMode: (RateMode) -> Unit,
    onWifiOnly: (Boolean) -> Unit,
    onInterval: (Int) -> Unit,
    onRefreshNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    var sourcesVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AppDimens.screenPadding),
    ) {
        Text(
            text = stringResource(R.string.more_title),
            style = MaterialTheme.typography.headlineSmall,
            color = colors.textPrimary,
            modifier = Modifier.padding(vertical = 12.dp),
        )

        SectionCard(title = stringResource(R.string.settings_rates)) {
            SegmentedRow(
                label = stringResource(R.string.settings_rate_mode),
                options = RateMode.entries.map { it to rateModeTitle(it) },
                selected = state.settings.rateMode,
                onSelect = onRateMode,
                vertical = true,
            )
            SwitchRow(
                title = stringResource(R.string.settings_show_change),
                checked = state.settings.showChangePercent,
                onCheckedChange = onShowChange,
            )
            ActionRow(
                icon = Icons.Filled.Refresh,
                title = stringResource(R.string.settings_refresh_now),
                onClick = onRefreshNow,
            )
            ActionRow(
                icon = Icons.Filled.Info,
                title = stringResource(R.string.about_sources),
                onClick = { sourcesVisible = true },
            )
        }

        SectionCard(title = stringResource(R.string.settings_appearance)) {
            SegmentedRow(
                label = stringResource(R.string.settings_theme),
                options = listOf(
                    ThemeMode.SYSTEM to stringResource(R.string.theme_system),
                    ThemeMode.LIGHT to stringResource(R.string.theme_light),
                    ThemeMode.DARK to stringResource(R.string.theme_dark),
                ),
                selected = state.settings.themeMode,
                onSelect = onTheme,
            )
            SegmentedRow(
                label = stringResource(R.string.settings_language),
                options = listOf(
                    AppLanguage.SYSTEM to stringResource(R.string.language_system),
                    AppLanguage.RUSSIAN to stringResource(R.string.language_ru),
                    AppLanguage.ENGLISH to stringResource(R.string.language_en),
                ),
                selected = state.settings.language,
                onSelect = onLanguage,
            )
        }

        SectionCard(title = stringResource(R.string.settings_numbers)) {
            SegmentedRow(
                label = stringResource(R.string.settings_decimals),
                options = (0..AppSettings.MAX_DECIMALS).map { it to it.toString() },
                selected = state.settings.decimals,
                onSelect = onDecimals,
            )
            SwitchRow(
                title = stringResource(R.string.settings_grouping),
                checked = state.settings.grouping,
                onCheckedChange = onGrouping,
            )
        }

        SectionCard(title = stringResource(R.string.settings_sync)) {
            SwitchRow(
                title = stringResource(R.string.settings_wifi_only),
                checked = state.settings.syncOnlyOnWifi,
                onCheckedChange = onWifiOnly,
            )
            SegmentedRow(
                label = stringResource(
                    R.string.settings_interval,
                    state.settings.syncIntervalHours,
                ),
                options = listOf(3, 6, 12, 24).map { it to "$it" },
                selected = state.settings.syncIntervalHours,
                onSelect = onInterval,
            )
        }

        SectionCard(title = stringResource(R.string.settings_about)) {
            Text(
                text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textSecondary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.about_offline),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textTertiary,
            )
        }

        Spacer(Modifier.height(AppDimens.bottomBarSpace))
    }

    if (sourcesVisible) {
        SourcesDialog(
            statuses = state.statuses,
            rateMode = state.settings.rateMode,
            onRateModeChange = {
                onRateMode(it)
                sourcesVisible = false
            },
            onDismiss = { sourcesVisible = false },
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    val colors = AppTheme.colors
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = colors.textSecondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface, RoundedCornerShape(AppDimens.cardCorner))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            content()
        }
    }
}


@Composable
private fun <T> SegmentedRow(
    label: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    vertical: Boolean = false,
) {
    val colors = AppTheme.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(8.dp))
        if (vertical) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                options.forEach { (value, title) ->
                    OptionChip(
                        title = title,
                        selected = value == selected,
                        onClick = { onSelect(value) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        } else {
            // FlowRow: длинные подписи в одну строку не помещаются
            // и раньше рвались по буквам
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                options.forEach { (value, title) ->
                    OptionChip(
                        title = title,
                        selected = value == selected,
                        onClick = { onSelect(value) },
                    )
                }
            }
        }
    }
}

@Composable
private fun OptionChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    Box(
        modifier = modifier
            .background(
                if (selected) colors.accent else colors.surfaceElevated,
                CircleShape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) androidx.compose.ui.graphics.Color.White else colors.textPrimary,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                checkedTrackColor = colors.accent,
            ),
        )
    }
}

@Composable
private fun ActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.size(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = colors.accent,
        )
    }
}
