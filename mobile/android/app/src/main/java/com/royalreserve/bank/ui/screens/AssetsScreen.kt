package com.royalreserve.bank.ui.screens

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.royalreserve.bank.data.AssetAvailability
import com.royalreserve.bank.data.BankApi
import com.royalreserve.bank.ui.components.Action
import com.royalreserve.bank.ui.components.Availability
import com.royalreserve.bank.ui.components.Field
import com.royalreserve.bank.ui.components.Page
import com.royalreserve.bank.ui.components.Panel
import com.royalreserve.bank.util.parseCodes

@Composable
internal fun Assets(vm: AssetsViewModel) {
    Page("Assets", vm.banner) {
        Field("PETR4, VALE3, AAPL", vm.input) { vm.input = it }
        Action(if (vm.pending) "Checking…" else "Check availability", !vm.pending, vm::check)
        if (vm.results.isNotEmpty())
            Panel {
                Text("Asset code / Status")
                vm.results.forEach {
                    Text(it.assetCode)
                    Availability(it.isAssetAvailable)
                    HorizontalDivider()
                }
            }
    }
}

class AssetsViewModel(private val api: () -> BankApi) : ScreenViewModel() {
    var input by mutableStateOf("")
    var results by mutableStateOf(emptyList<AssetAvailability>())

    fun check() {
        val codes = parseCodes(input)
        if (codes.isEmpty()) {
            notify("Enter at least one asset code.", true)
            return
        }
        execute("Failed to check asset availability") { results = api().assets(codes) }
    }
}
