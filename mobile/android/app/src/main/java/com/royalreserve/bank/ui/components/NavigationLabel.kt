package com.royalreserve.bank.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp

@Composable
internal fun NavigationLabel(text: String) {
    val style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp)
    val textMeasurer = rememberTextMeasurer()
    val textWidth = textMeasurer.measure(text, style = style, softWrap = false).size.width

    BoxWithConstraints {
        val availableWidth = (constraints.maxWidth - 1).coerceAtLeast(1)
        val scale = (availableWidth.toFloat() / textWidth.coerceAtLeast(1)).coerceAtMost(1f)
        Text(
            text,
            style = style.copy(fontSize = style.fontSize * scale),
            maxLines = 1,
            softWrap = false,
        )
    }
}
