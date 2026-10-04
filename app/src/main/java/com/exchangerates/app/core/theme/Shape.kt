package com.exchangerates.app.core.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

object AppDimens {
    /** Отступ от края экрана: на телефонах вроде Galaxy S21 узкие поля читаются лучше. */
    val screenPadding = 8.dp
    val cardCorner = 20.dp
    val cardPaddingH = 12.dp
    val cardPaddingV = 12.dp
    val cardGap = 6.dp
    val flagSize = 28.dp
    val operatorButton = 54.dp

    /** Запас снизу под плавающую панель навигации. */
    val bottomBarSpace = 132.dp

    /** Запас снизу под панель операторов, пока открыта клавиатура. */
    val operatorBarSpace = operatorButton + 24.dp
}
