package com.royalreserve.bank.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType

@Composable
internal fun Action(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick, enabled = enabled, modifier = Modifier.testTag(text)) { Text(text) }
}

@Composable
internal fun Field(
    label: String,
    value: String,
    numeric: Boolean = false,
    multiline: Boolean = false,
    autofocus: Boolean = false,
    change: (String) -> Unit,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(autofocus) { if (autofocus) focus.requestFocus() }
    OutlinedTextField(
        value,
        change,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().testTag(label).focusRequester(focus),
        singleLine = !multiline,
        minLines = if (multiline) 4 else 1,
        keyboardOptions =
            KeyboardOptions(keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text),
    )
}
