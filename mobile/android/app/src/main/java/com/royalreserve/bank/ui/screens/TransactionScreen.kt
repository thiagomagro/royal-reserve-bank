package com.royalreserve.bank.ui.screens

import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.royalreserve.bank.data.BankApi
import com.royalreserve.bank.data.ItemRow
import com.royalreserve.bank.ui.components.Action
import com.royalreserve.bank.ui.components.Availability
import com.royalreserve.bank.ui.components.Field
import com.royalreserve.bank.ui.components.Page
import com.royalreserve.bank.ui.components.Panel
import com.royalreserve.bank.util.parseItems
import com.royalreserve.bank.util.transactionIsError
import com.royalreserve.bank.util.transactionValidationMessage

@Composable
internal fun Transaction(vm: TransactionViewModel) {
    Page("New transaction", vm.banner) {
        vm.rows.forEachIndexed { index, row ->
            Panel {
                Field("Asset code", row.code) { vm.edit(index, row.copy(code = it)) }
                Field("Asset name", row.name) { vm.edit(index, row.copy(name = it)) }
                Field("Value", row.value, numeric = true) { vm.edit(index, row.copy(value = it)) }
                row.availability?.let { Availability(it) }
                OutlinedButton(
                    onClick = { vm.rows = vm.rows.filterIndexed { i, _ -> i != index } },
                    enabled = vm.rows.size > 1,
                ) {
                    Text("Remove")
                }
            }
        }
        Action("Add item") { vm.rows = vm.rows + ItemRow() }
        Action(if (vm.pending) "Checking…" else "Check availability", !vm.pending, vm::check)
        Action(if (vm.pending) "Submitting…" else "Submit transaction", !vm.pending, vm::submit)
    }
}

class TransactionViewModel(private val api: () -> BankApi) : ScreenViewModel() {
    var rows by mutableStateOf(listOf(ItemRow()))

    fun edit(index: Int, row: ItemRow) {
        rows = rows.mapIndexed { i, old -> if (i == index) row.copy(availability = null) else old }
    }

    fun check() {
        val codes = rows.map { it.code.trim() }.filter { it.isNotEmpty() }
        if (codes.isEmpty()) {
            notify("Enter at least one asset code first.", true)
            return
        }
        execute("Failed to check asset availability") {
            val map = api().assets(codes).associate { it.assetCode to it.isAssetAvailable }
            rows = rows.map { it.copy(availability = map[it.code]) }
        }
    }

    fun submit() {
        transactionValidationMessage(rows)?.let {
            notify(it, true)
            return
        }
        val items = requireNotNull(parseItems(rows))
        execute("Transaction failed") {
            val message = api().transaction(items)
            notify(message, transactionIsError(message))
        }
    }
}
