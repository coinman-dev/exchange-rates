package com.exchangerates.app.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.exchangerates.app.core.theme.AppDimens
import com.exchangerates.app.core.theme.AppTheme

/** Экраны нижней навигации. Кнопки переводов, как у Xe, нет — она не нужна. */
enum class AppTab(val route: String, val icon: ImageVector) {
    HOME("home", Icons.Outlined.Home),
    CHART("chart", Icons.Filled.ShowChart),
    MORE("more", Icons.Filled.MoreHoriz),
}

/**
 * Верхняя панель-пилюля: слева «Курсы» (обзор), справа «Конвертер».
 * Повторяет сегментированный переключатель из нового дизайна Xe.
 */
@Composable
fun TopSegmentedBar(
    convertTitle: String,
    ratesContentDescription: String,
    isConverterSelected: Boolean,
    onSelectRates: () -> Unit,
    onSelectConverter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.screenPadding, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .background(colors.surface, CircleShape)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SegmentButton(
                icon = Icons.Outlined.Home,
                title = null,
                selected = !isConverterSelected,
                contentDescription = ratesContentDescription,
                onClick = onSelectRates,
            )
            SegmentButton(
                icon = Icons.Filled.SwapHoriz,
                title = convertTitle,
                selected = isConverterSelected,
                contentDescription = convertTitle,
                onClick = onSelectConverter,
            )
        }
    }
}

@Composable
private fun SegmentButton(
    icon: ImageVector,
    title: String?,
    selected: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .background(
                if (selected) colors.surfaceElevated else androidx.compose.ui.graphics.Color.Transparent,
                CircleShape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = if (title == null) 16.dp else 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (selected) colors.textPrimary else colors.textSecondary,
            modifier = Modifier.size(22.dp),
        )
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) colors.textPrimary else colors.textSecondary,
            )
        }
    }
}

/** Плавающая нижняя навигация-пилюля. */
@Composable
fun FloatingBottomBar(
    current: AppTab,
    labels: Map<AppTab, String>,
    onSelect: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    Box(
        modifier = modifier.fillMaxWidth().padding(bottom = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .background(colors.surface, CircleShape)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            AppTab.entries.forEach { tab ->
                val selected = tab == current
                Column(
                    modifier = Modifier
                        .background(
                            if (selected) colors.surfaceElevated else androidx.compose.ui.graphics.Color.Transparent,
                            CircleShape,
                        )
                        .clickable { onSelect(tab) }
                        .padding(horizontal = 22.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = labels[tab],
                        tint = if (selected) colors.textPrimary else colors.textSecondary,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = labels[tab].orEmpty(),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) colors.textPrimary else colors.textSecondary,
                    )
                }
            }
        }
    }
}

/** Кнопка «+ Добавить валюту» — контурная, во всю ширину. */
@Composable
fun AddCurrencyButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, colors.outline, RoundedCornerShape(AppDimens.cardCorner))
            .clickable(onClick = onClick)
            .padding(vertical = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = colors.textPrimary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
        )
    }
}

/**
 * Подпись под списком: когда обновлялись курсы и какой это курс.
 * Точка слева — индикатор состояния: синяя (актуально), оранжевая (офлайн).
 *
 * Две строки вместо одной: на узком экране русская дата и название курса
 * рядом не помещались и переносились по слогам.
 */
@Composable
fun FooterStatus(
    updatedText: String,
    modeText: String,
    isOffline: Boolean,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 12.dp, bottom = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(if (isOffline) colors.warning else colors.accent, CircleShape),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = updatedText,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            modifier = Modifier
                .clickable(onClick = onInfoClick)
                .padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = modeText,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                maxLines = 1,
                softWrap = false,
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
