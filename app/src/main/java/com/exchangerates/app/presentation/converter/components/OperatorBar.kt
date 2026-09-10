package com.exchangerates.app.presentation.converter.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.exchangerates.app.R
import com.exchangerates.app.core.theme.AppDimens
import com.exchangerates.app.core.theme.AppTheme

/** Кнопка панели операторов. */
enum class MathOperator(
    val glyph: String,
    val insert: String,
    @StringRes val descriptionRes: Int,
) {
    PLUS("+", "+", R.string.operator_plus),
    MINUS("−", "-", R.string.operator_minus),
    TIMES("×", "*", R.string.operator_times),
    DIVIDE("÷", "/", R.string.operator_divide),
    EQUALS("=", "=", R.string.operator_equals),
}

/**
 * Полоса математических операторов, как в новом дизайне Xe: плавает поверх
 * списка, сразу над системной клавиатурой, и появляется только когда
 * пользователь вводит сумму.
 */
@Composable
fun OperatorBar(
    onOperator: (MathOperator) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.screenPadding, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MathOperator.entries.forEach { operator ->
            val interaction = remember { MutableInteractionSource() }
            val description = stringResource(operator.descriptionRes)
            Box(
                modifier = Modifier
                    .size(AppDimens.operatorButton)
                    .clip(CircleShape)
                    .background(if (operator == MathOperator.EQUALS) colors.accent else colors.surfaceElevated)
                    .clickable(
                        interactionSource = interaction,
                        indication = ripple(),
                        onClick = { onOperator(operator) },
                    )
                    .semantics { contentDescription = description },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = operator.glyph,
                    color = colors.textPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}
