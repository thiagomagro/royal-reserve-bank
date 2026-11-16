package com.royalreserve.bank.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

@Composable
internal fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    enabled: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val cancelFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { cancelFocus.requestFocus() }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { Action(confirmLabel, enabled, onConfirm) },
        dismissButton = {
            TextButton(onClick = onCancel, modifier = Modifier.focusRequester(cancelFocus)) {
                Text("Cancel")
            }
        },
    )
}
