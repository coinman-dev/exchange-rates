package com.exchangerates.app.presentation.converter

import androidx.compose.ui.text.input.TextFieldValue
import com.exchangerates.app.presentation.converter.components.textFieldValue

/**
 * Правка поля, в котором ещё стоит подставленная сумма.
 *
 * Набранная цифра начинает новое число, а не дописывается к старому: «100» и
 * «5» дают «5». Знак действия, наоборот, продолжает старое значение: «100» и
 * «+» дают «100+». Стирание и замена выделенного работают как в обычном поле.
 */
internal fun editPristineInput(old: TextFieldValue, new: TextFieldValue): TextFieldValue {
    val added = new.text.length - old.text.length
    if (added <= 0) return new
    val start = old.text.commonPrefixWith(new.text).length
    val isInsertion = new.text.substring(start + added) == old.text.substring(start)
    if (!isInsertion) return new
    val typed = new.text.substring(start, start + added)
    val startsNumber = typed.first().let { it.isDigit() || it == '.' || it == ',' }
    return if (startsNumber) textFieldValue(typed) else new
}
