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
    val screenPadding = 10.dp
    val cardCorner = 20.dp
    val cardPaddingH = 14.dp
    val cardPaddingV = 14.dp
    val cardGap = 8.dp
    val flagSize = 28.dp
    val operatorButton = 54.dp

    /** Запас снизу под плавающую панель навигации. */
    val bottomBarSpace = 132.dp
}
