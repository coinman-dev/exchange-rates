package com.exchangerates.app.presentation.picker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.exchangerates.app.R
import com.exchangerates.app.core.theme.AppDimens
import com.exchangerates.app.core.theme.AppTheme

/** Действия с карточкой валюты (долгое нажатие). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardActionsSheet(
    code: String,
    isBase: Boolean,
    canRemove: Boolean,
    onDismiss: () -> Unit,
    onMakeBase: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onOpenChart: () -> Unit,
) {
    val colors = AppTheme.colors
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background,
        contentColor = colors.textPrimary,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                text = code,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary,
                modifier = Modifier.padding(horizontal = AppDimens.screenPadding),
            )
            Spacer(Modifier.height(10.dp))
            if (!isBase) {
                ActionRow(Icons.Filled.Star, stringResource(R.string.action_make_base), onMakeBase)
                ActionRow(Icons.Filled.ArrowUpward, stringResource(R.string.action_move_up), onMoveUp)
                ActionRow(
                    Icons.Filled.ArrowDownward,
                    stringResource(R.string.action_move_down),
                    onMoveDown,
                )
            }
            ActionRow(Icons.Filled.ShowChart, stringResource(R.string.action_open_chart), onOpenChart)
            if (canRemove) {
                ActionRow(
                    icon = Icons.Filled.Delete,
                    title = stringResource(R.string.action_remove),
                    onClick = onRemove,
                    destructive = true,
                )
            }
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    destructive: Boolean = false,
) {
    val colors = AppTheme.colors
    val tint = if (destructive) colors.negative else colors.textPrimary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = AppDimens.screenPadding, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(text = title, style = MaterialTheme.typography.bodyLarge, color = tint)
    }
}
