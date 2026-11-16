package com.royalreserve.bank.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.royalreserve.bank.data.Account
import com.royalreserve.bank.data.AccountsStore
import com.royalreserve.bank.data.BankApi
import com.royalreserve.bank.data.NewAccount
import com.royalreserve.bank.ui.components.AccountRow
import com.royalreserve.bank.ui.components.Action
import com.royalreserve.bank.ui.components.Banner
import com.royalreserve.bank.ui.components.BannerView
import com.royalreserve.bank.ui.components.ConfirmDialog
import com.royalreserve.bank.ui.components.Field
import com.royalreserve.bank.ui.components.Page
import com.royalreserve.bank.ui.components.Panel
import com.royalreserve.bank.util.accountValidationMessage
import com.royalreserve.bank.util.depositValidationMessage
import com.royalreserve.bank.util.formatCurrency
import kotlinx.coroutines.launch

@Composable
internal fun Accounts(vm: AccountsViewModel) {
    val state by vm.store.state.collectAsState()
    Page("Accounts", vm.banner) {
        Action("New account", !vm.pending) { vm.creating = true }
        if (state.loading) Text("Loading accounts…")
        else if (state.error != null)
            BannerView(Banner("Failed to load accounts: ${state.error}", true))
        else
            Panel {
                Text("Holder / Account # / Balance / Currency")
                state.accounts.forEach { account ->
                    AccountRow(account)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Action("Add funds", !vm.pending) {
                            vm.amount = ""
                            vm.depositTarget = account
                        }
                        OutlinedButton(
                            onClick = { vm.deleteTarget = account },
                            enabled = !vm.pending,
                        ) {
                            Text("Delete")
                        }
                    }
                    HorizontalDivider()
                }
            }
    }
    if (vm.creating)
        AlertDialog(
            onDismissRequest = { if (!vm.pending) vm.creating = false },
            title = { Text("New account") },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BannerView(vm.banner?.takeIf { it.error })
                    Field("Account holder name", vm.holder) { vm.holder = it }
                    Field("Initial balance", vm.balance, numeric = true) { vm.balance = it }
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.testTag("Currency"),
                        ) {
                            Text("Currency: ${vm.currency}")
                        }
                        DropdownMenu(expanded, { expanded = false }) {
                            listOf("USD", "EUR", "GBP", "BRL").forEach { code ->
                                DropdownMenuItem(
                                    text = { Text(code) },
                                    onClick = {
                                        vm.currency = code
                                        expanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Action(if (vm.pending) "Creating…" else "Create account", !vm.pending, vm::create)
            },
            dismissButton = {
                TextButton(onClick = { vm.creating = false }, enabled = !vm.pending) {
                    Text("Cancel")
                }
            },
        )
    vm.depositTarget?.let { account ->
        AlertDialog(
            onDismissRequest = {
                if (!vm.pending) {
                    vm.depositTarget = null
                    vm.amount = ""
                }
            },
            title = { Text("Add funds") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BannerView(vm.banner?.takeIf { it.error })
                    Text(account.accountHolderName)
                    Text(formatCurrency(account.balance, account.currency))
                    Field(
                        "Amount (${account.currency})",
                        vm.amount,
                        numeric = true,
                        autofocus = true,
                    ) {
                        vm.amount = it
                    }
                }
            },
            confirmButton = {
                Action(if (vm.pending) "Adding…" else "Confirm deposit", !vm.pending, vm::deposit)
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        vm.depositTarget = null
                        vm.amount = ""
                    },
                    enabled = !vm.pending,
                ) {
                    Text("Cancel")
                }
            },
        )
    }
    vm.deleteTarget?.let { account ->
        ConfirmDialog(
            title = "Delete account",
            message = "Delete the account for ${account.accountHolderName}?",
            confirmLabel = "Delete",
            enabled = !vm.pending,
            onConfirm = vm::delete,
            onCancel = { vm.deleteTarget = null },
        )
    }
}

class AccountsViewModel(private val api: () -> BankApi, val store: AccountsStore) :
    ScreenViewModel() {
    var creating by mutableStateOf(false)
    var holder by mutableStateOf("")
    var balance by mutableStateOf("0")
    var currency by mutableStateOf("USD")
    var depositTarget by mutableStateOf<Account?>(null)
    var amount by mutableStateOf("")
    var deleteTarget by mutableStateOf<Account?>(null)

    init {
        viewModelScope.launch { store.refresh() }
    }

    fun create() {
        accountValidationMessage(holder, balance)?.let {
            notify(it, true)
            return
        }
        execute("Failed to create account") {
            notify(api().create(NewAccount(holder.trim(), balance.toDouble(), currency)))
            creating = false
            holder = ""
            balance = "0"
            currency = "USD"
            store.refresh()
        }
    }

    fun deposit() {
        val target = depositTarget ?: return
        depositValidationMessage(amount)?.let {
            notify(it, true)
            return
        }
        execute("Failed to add funds") {
            val updated = api().deposit(target.id, amount.toDouble())
            notify(
                "Added funds to ${target.accountHolderName}. New balance: ${formatCurrency(updated.balance, updated.currency)}."
            )
            depositTarget = null
            amount = ""
            store.refresh()
        }
    }

    fun delete() {
        val target = deleteTarget ?: return
        deleteTarget = null
        execute("Failed to delete account") {
            notify(api().delete(target.accountHolderName))
            store.refresh()
        }
    }
}
