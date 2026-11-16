package com.royalreserve.bank.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val BankColors =
    lightColorScheme(
        primary = Slate900,
        background = Slate50,
        surface = White,
        onSurface = Slate900,
    )

@Composable
fun BankTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = BankColors, content = content)
}
