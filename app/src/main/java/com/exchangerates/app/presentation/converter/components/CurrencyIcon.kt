package com.exchangerates.app.presentation.converter.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.exchangerates.app.core.theme.AppDimens
import com.exchangerates.app.core.theme.AppTheme
import com.exchangerates.app.core.util.Flags
import com.exchangerates.app.domain.model.Currency
import com.exchangerates.app.domain.model.CurrencyKind

/**
 * Круглая иконка валюты.
 *
 * Флаги — системные emoji по коду страны: не занимают места в APK, всегда
 * чёткие и не требуют сети. У Xe криптовалюты показаны без иконки (просто
 * серый код) — здесь для них есть собственные значки, а для металлов
 * используются химические символы.
 */
@Composable
fun CurrencyIcon(
    currency: Currency,
    modifier: Modifier = Modifier,
    size: Dp = AppDimens.flagSize,
) {
    val colors = AppTheme.colors
    val flag = Flags.emoji(currency.country)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(iconBackground(currency, colors.surfaceElevated)),
        contentAlignment = Alignment.Center,
    ) {
        when {
            flag != null -> Text(
                text = flag,
                style = TextStyle(
                    fontSize = (size.value * 0.86f).sp,
                    textAlign = TextAlign.Center,
                ),
            )
            else -> Text(
                text = badgeText(currency),
                style = TextStyle(
                    fontSize = (size.value * 0.40f).sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = badgeTextColor(currency),
                ),
            )
        }
    }
}

private fun iconBackground(currency: Currency, fallback: Color): Brush = when {
    currency.kind == CurrencyKind.METAL -> Brush.linearGradient(metalColors(currency.code))
    currency.kind == CurrencyKind.CRYPTO -> Brush.linearGradient(cryptoColors(currency.code))
    else -> Brush.linearGradient(listOf(fallback, fallback))
}

private fun metalColors(code: String): List<Color> = when (code) {
    "XAU" -> listOf(Color(0xFFF7D774), Color(0xFFCF9B23))
    "XAG" -> listOf(Color(0xFFE3E6EA), Color(0xFF9BA3AC))
    "XPT" -> listOf(Color(0xFFDDE6EC), Color(0xFF8FA3AF))
    else -> listOf(Color(0xFFD8DDE2), Color(0xFF8B9299))
}

private fun cryptoColors(code: String): List<Color> = when (code) {
    "BTC" -> listOf(Color(0xFFF7A93B), Color(0xFFE07C0A))
    "ETH" -> listOf(Color(0xFF8A92E8), Color(0xFF5560C4))
    "USDT" -> listOf(Color(0xFF3ECFA0), Color(0xFF1BA37B))
    "USDC" -> listOf(Color(0xFF4B8DF8), Color(0xFF2563EB))
    "BNB" -> listOf(Color(0xFFF3D34A), Color(0xFFD4AF10))
    "SOL" -> listOf(Color(0xFF9B6BF5), Color(0xFF5E32C4))
    "XRP" -> listOf(Color(0xFF6C7A89), Color(0xFF3B4650))
    "TON" -> listOf(Color(0xFF4FA8E8), Color(0xFF1E7FC2))
    "DOGE" -> listOf(Color(0xFFE0C574), Color(0xFFB99A2E))
    "TRX" -> listOf(Color(0xFFE8544A), Color(0xFFBE2D24))
    else -> listOf(Color(0xFF4A4A52), Color(0xFF33333A))
}

private fun badgeText(currency: Currency): String = when (currency.code) {
    "XAU" -> "Au"
    "XAG" -> "Ag"
    "XPT" -> "Pt"
    "XPD" -> "Pd"
    "BTC" -> "₿"
    "ETH" -> "Ξ"
    "USDT" -> "₮"
    else -> currency.code.take(3)
}

private fun badgeTextColor(currency: Currency): Color = when (currency.kind) {
    CurrencyKind.METAL -> Color(0xFF2A2013)
    CurrencyKind.CRYPTO -> Color.White
    CurrencyKind.FIAT -> Color(0xFFB9B9BF)
}
