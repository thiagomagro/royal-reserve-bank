package com.royalreserve.bank.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.royalreserve.bank.ui.theme.Emerald100
import com.royalreserve.bank.ui.theme.Emerald700
import com.royalreserve.bank.ui.theme.Red100
import com.royalreserve.bank.ui.theme.Red700

@Composable
internal fun Availability(value: Boolean) {
    Text(
        if (value) "Available" else "Unavailable",
        color = if (value) Emerald700 else Red700,
        modifier = Modifier.background(if (value) Emerald100 else Red100).padding(6.dp),
    )
}
