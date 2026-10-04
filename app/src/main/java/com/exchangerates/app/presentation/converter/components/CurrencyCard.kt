package com.exchangerates.app.presentation.converter.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.exchangerates.app.core.theme.AppDimens
import com.exchangerates.app.core.theme.AppTheme
import com.exchangerates.app.domain.model.Currency

/** Данные одной карточки для отрисовки. */
data class CurrencyCardData(
    val currency: Currency,
    val isBase: Boolean,
    /** Значение без форматирования — подставляется в поле при фокусе. */
    val amount: java.math.BigDecimal? = null,
    val amountText: String,
    val unitRateText: String?,
    val changePercentText: String? = null,
    val changePositive: Boolean = true,
    val hasRate: Boolean = true,
)

/**
 * Карточка валюты в трёх состояниях, как в новом дизайне Xe:
 * базовая («You convert»), пассивная и активная (синяя рамка + кнопка очистки).
 */
@Composable
fun CurrencyCard(
    data: CurrencyCardData,
    isActive: Boolean,
    input: TextFieldValue,
    inputPristine: Boolean,
    inputError: Boolean,
    baseLabel: String,
    noRateLabel: String,
    onSelect: () -> Unit,
    onLongPress: () -> Unit,
    onPickCurrency: () -> Unit,
    onInputChange: (TextFieldValue) -> Unit,
    onCommit: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val borderColor by animateColorAsState(
        targetValue = when {
            inputError && isActive -> colors.negative
            isActive -> colors.accent
            else -> Color.Transparent
        },
        label = "cardBorder",
    )
    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(AppDimens.cardCorner))
            .border(2.dp, borderColor, RoundedCornerShape(AppDimens.cardCorner))
            .combinedClickable(onClick = onSelect, onLongClick = onLongPress)
            .padding(horizontal = AppDimens.cardPaddingH, vertical = AppDimens.cardPaddingV),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (data.isBase) {
                Text(
                    text = baseLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textSecondary,
                )
                Spacer(Modifier.size(6.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CurrencySelector(
                    currency = data.currency,
                    onClick = onPickCurrency,
                )
                Spacer(Modifier.width(8.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End,
                ) {
                    AmountArea(
                        data = data,
                        isActive = isActive,
                        input = input,
                        inputPristine = inputPristine,
                        focusRequester = focusRequester,
                        noRateLabel = noRateLabel,
                        onInputChange = onInputChange,
                        onCommit = onCommit,
                        onSelect = onSelect,
                    )
                    if (!data.isBase && data.unitRateText != null) {
                        Spacer(Modifier.size(2.dp))
                        UnitRateLine(
                            text = data.unitRateText,
                            changeText = data.changePercentText,
                            changePositive = data.changePositive,
                        )
                    }
                }
                if (isActive) {
                    Spacer(Modifier.width(8.dp))
                    ClearButton(onClear)
                }
            }
        }
    }

    LaunchedEffect(isActive) {
        if (isActive) focusRequester.requestFocus()
    }
}

@Composable
private fun CurrencySelector(
    currency: Currency,
    onClick: () -> Unit,
) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CurrencyIcon(currency)
        Text(
            text = currency.code,
            style = MaterialTheme.typography.titleLarge,
            color = colors.textPrimary,
        )
        Icon(
            imageVector = Icons.Filled.ExpandMore,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun AmountArea(
    data: CurrencyCardData,
    isActive: Boolean,
    input: TextFieldValue,
    inputPristine: Boolean,
    focusRequester: FocusRequester,
    noRateLabel: String,
    onInputChange: (TextFieldValue) -> Unit,
    onCommit: () -> Unit,
    onSelect: () -> Unit,
) {
    val colors = AppTheme.colors
    val amountStyle = if (data.isBase) {
        MaterialTheme.typography.displaySmall
    } else {
        MaterialTheme.typography.headlineMedium
    }

    if (isActive) {
        BasicTextField(
            value = input,
            onValueChange = onInputChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            textStyle = amountStyle.copy(
                color = if (inputPristine) colors.textSecondary else colors.textPrimary,
                textAlign = TextAlign.End,
            ),
            cursorBrush = SolidColor(colors.accent),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onCommit() }),
        )
    } else if (!data.hasRate) {
        Text(
            text = noRateLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textTertiary,
            modifier = Modifier.clickable(onClick = onSelect),
        )
    } else {
        // длинная сумма (до шести знаков после запятой) уменьшает шрифт, а не
        // обрезается многоточием: обрезанное число читается как другое число
        Text(
            text = data.amountText,
            style = amountStyle,
            color = colors.textPrimary,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            autoSize = TextAutoSize.StepBased(
                minFontSize = 14.sp,
                maxFontSize = amountStyle.fontSize,
                stepSize = 1.sp,
            ),
            modifier = Modifier.clickable(onClick = onSelect),
        )
    }
}

/**
 * Курс и изменение за сутки одной строкой.
 *
 * На узком экране (Galaxy S21 и подобные) отдельные Text в Row не помещались,
 * и изменение переносилось по одному символу в столбик. Поэтому строка
 * собирается целиком и перенос запрещён. Если она не помещается (крупный
 * системный шрифт, длинный курс), уменьшается шрифт: изменение стоит в конце
 * строки, и многоточие съедало именно его, оставляя «+…».
 */
@Composable
private fun UnitRateLine(
    text: String,
    changeText: String?,
    changePositive: Boolean,
) {
    val colors = AppTheme.colors
    val changeColor = if (changePositive) colors.positive else colors.negative
    val line = buildAnnotatedString {
        append(text)
        if (changeText != null) {
            append("  ")
            withStyle(SpanStyle(color = changeColor, fontWeight = FontWeight.Medium)) {
                append(changeText)
            }
        }
    }
    val style = MaterialTheme.typography.bodyMedium
    // высота строки закреплена: с уменьшенным шрифтом карточки иначе выходят разной высоты
    Box(
        modifier = Modifier.height(with(LocalDensity.current) { style.lineHeight.toDp() }),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Text(
            text = line,
            style = style,
            color = colors.textSecondary,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            autoSize = TextAutoSize.StepBased(
                minFontSize = 8.sp,
                maxFontSize = style.fontSize,
                stepSize = 0.5.sp,
            ),
        )
    }
}

@Composable
private fun ClearButton(onClear: () -> Unit) {
    val colors = AppTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(28.dp)
            .background(colors.surfaceElevated, CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClear),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(16.dp),
        )
    }
}

/** Пустое значение поля с курсором в конце. */
fun textFieldValue(text: String): TextFieldValue =
    TextFieldValue(text = text, selection = TextRange(text.length))
