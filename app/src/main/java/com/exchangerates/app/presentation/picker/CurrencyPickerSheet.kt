package com.exchangerates.app.presentation.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.exchangerates.app.R
import com.exchangerates.app.core.theme.AppDimens
import com.exchangerates.app.core.theme.AppTheme
import com.exchangerates.app.domain.model.Currency
import com.exchangerates.app.presentation.converter.components.CurrencyIcon

/**
 * Выбор валюты: поиск по коду, названию (русскому и английскому), символу
 * и алиасам (RUR, XBT), плюс фильтры по типу актива.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyPickerSheet(
    currencies: List<Currency>,
    russian: Boolean,
    alreadyAdded: Set<String>,
    title: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val colors = AppTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background,
        contentColor = colors.textPrimary,
    ) {
        CurrencyPickerContent(
            currencies = currencies,
            russian = russian,
            alreadyAdded = alreadyAdded,
            title = title,
            onSelect = onSelect,
            modifier = Modifier.fillMaxHeight(0.94f),
        )
    }
}

/**
 * Содержимое диалога вынесено отдельно: так его можно отрисовать в тестах
 * и снять скриншот без модального окна.
 */
@Composable
internal fun CurrencyPickerContent(
    currencies: List<Currency>,
    russian: Boolean,
    alreadyAdded: Set<String>,
    title: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(PickerFilter.POPULAR) }

    Column(modifier = modifier.background(colors.background)) {
        Spacer(Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = colors.textPrimary,
            modifier = Modifier.padding(horizontal = AppDimens.screenPadding),
        )
        Spacer(Modifier.height(14.dp))
        SearchField(
            query = query,
            onQueryChange = { query = it },
            modifier = Modifier.padding(horizontal = AppDimens.screenPadding),
        )
        Spacer(Modifier.height(12.dp))
        FilterRow(selected = filter, onSelect = { filter = it })
        Spacer(Modifier.height(8.dp))

        val visible = remember(currencies, query, filter, russian) {
            CurrencyFilter.apply(currencies, query, filter, russian)
        }

        if (visible.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.picker_nothing_found),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textSecondary,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                items(visible, key = { it.code }) { currency ->
                    CurrencyRow(
                        currency = currency,
                        russian = russian,
                        isAdded = currency.code in alreadyAdded,
                        onClick = { onSelect(currency.code) },
                    )
                }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = stringResource(R.string.search_currency),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textTertiary,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.accent),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Разделы списка. FlowRow вместо Row: на узком экране пять подписей в одну
 * строку не влезали и обрезались по буквам, теперь они переносятся.
 */
@Composable
private fun FilterRow(
    selected: PickerFilter,
    onSelect: (PickerFilter) -> Unit,
) {
    val colors = AppTheme.colors
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.screenPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PickerFilter.entries.forEach { entry ->
            val isSelected = entry == selected
            Text(
                text = stringResource(filterLabel(entry)),
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) colors.background else colors.textSecondary,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .background(
                        if (isSelected) colors.textPrimary else colors.surface,
                        CircleShape,
                    )
                    .clickable { onSelect(entry) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

private fun filterLabel(filter: PickerFilter): Int = when (filter) {
    PickerFilter.POPULAR -> R.string.filter_popular
    PickerFilter.ALL -> R.string.filter_all
    PickerFilter.FIAT -> R.string.filter_fiat
    PickerFilter.CRYPTO -> R.string.filter_crypto
    PickerFilter.METALS -> R.string.filter_metals
}

@Composable
private fun CurrencyRow(
    currency: Currency,
    russian: Boolean,
    isAdded: Boolean,
    onClick: () -> Unit,
) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = AppDimens.screenPadding, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CurrencyIcon(currency, size = 34.dp)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = currency.code,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary,
            )
            Text(
                text = currency.displayName(russian),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                maxLines = 1,
            )
        }
        if (currency.symbol.isNotEmpty()) {
            Text(
                text = currency.symbol,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textTertiary,
            )
        }
        if (isAdded) {
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
