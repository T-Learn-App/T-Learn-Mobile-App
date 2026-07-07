package com.example.t_learnappmobile.presentation.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun SmartText(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 2,
    color: Color = Color.Unspecified,
    textAlign: TextAlign = TextAlign.Center,
    fontWeight: FontWeight? = null,
    defaultFontSize: TextUnit = 32.sp,
    minFontSize: TextUnit = 14.sp
) {
    val fontSize = remember(text, defaultFontSize) {
        when {
            text.length <= 15 -> defaultFontSize
            text.length <= 25 -> defaultFontSize * 0.85f
            text.length <= 35 -> defaultFontSize * 0.7f
            text.length <= 45 -> defaultFontSize * 0.55f
            else -> minFontSize
        }
    }

    Text(
        text = text,
        modifier = modifier,
        fontSize = fontSize,
        maxLines = maxLines,
        color = color,
        textAlign = textAlign,
        fontWeight = fontWeight,
        softWrap = true
    )
}

@Composable
fun SmartWordText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight = FontWeight.Bold
) {
    SmartText(
        text = text,
        modifier = modifier,
        maxLines = 2,
        color = color,
        textAlign = TextAlign.Center,
        fontWeight = fontWeight,
        defaultFontSize = 36.sp,
        minFontSize = 18.sp
    )
}

@Composable
fun SmartTranslationText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight = FontWeight.Medium
) {
    SmartText(
        text = text,
        modifier = modifier,
        maxLines = 3,
        color = color,
        textAlign = TextAlign.Center,
        fontWeight = fontWeight,
        defaultFontSize = 28.sp,
        minFontSize = 16.sp
    )
}

@Composable
fun SmartGameWordText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified
) {
    SmartText(
        text = text,
        modifier = modifier,
        maxLines = 2,
        color = color,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Bold,
        defaultFontSize = 32.sp,
        minFontSize = 18.sp
    )
}

@Composable
fun SmartGameOptionText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified
) {
    SmartText(
        text = text,
        modifier = modifier,
        maxLines = 2,
        color = color,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Bold,
        defaultFontSize = 22.sp,
        minFontSize = 14.sp
    )
}