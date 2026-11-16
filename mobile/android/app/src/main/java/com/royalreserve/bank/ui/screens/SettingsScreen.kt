package com.royalreserve.bank.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewModelScope
import com.royalreserve.bank.data.AccountsStore
import com.royalreserve.bank.data.SettingsRepository
import com.royalreserve.bank.ui.components.Action
import com.royalreserve.bank.ui.components.Field
import com.royalreserve.bank.ui.components.Page
import com.royalreserve.bank.ui.components.Panel
import kotlinx.coroutines.launch

@Composable
internal fun Settings(vm: SettingsViewModel) {
    Page("Settings", vm.banner) {
        Panel {
            Text("API mock mode: ${if (vm.mockMode) "enabled" else "disabled"}")
            Row {
                Text("Offline mock mode", Modifier.weight(1f))
                Switch(vm.mockMode, vm::toggle, modifier = Modifier.testTag("mockMode"))
            }
        }
        Panel {
            Field("Base URL", vm.baseUrl) { vm.baseUrl = it }
            Action("Save base URL", onClick = vm::saveUrl)
        }
        Panel {
            Field("API token (Auth0 JWT)", vm.token, multiline = true) { vm.token = it }
            Text(
                "The token is stored on this device and sent as a Bearer token on every API request."
            )
            Action("Save token", onClick = vm::saveToken)
            Action("Clear token", onClick = vm::clearToken)
        }
    }
}

class SettingsViewModel(val settings: SettingsRepository, private val store: AccountsStore) :
    ScreenViewModel() {
    var mockMode by mutableStateOf(settings.mockMode)
    var baseUrl by mutableStateOf(settings.baseUrl)
    var token by mutableStateOf(settings.token.orEmpty())

    fun toggle(value: Boolean) {
        settings.mockMode = value
        mockMode = value
        viewModelScope.launch { store.refresh() }
    }

    fun saveUrl() {
        settings.baseUrl = baseUrl
        baseUrl = settings.baseUrl
        notify("Base URL saved.")
        viewModelScope.launch { store.refresh() }
    }

    fun saveToken() {
        settings.token = token
        notify("API token saved.")
    }

    fun clearToken() {
        settings.token = null
        token = ""
        notify("API token cleared.")
    }
}
