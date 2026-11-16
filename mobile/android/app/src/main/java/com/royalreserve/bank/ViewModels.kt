package com.royalreserve.bank

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AccountState(val accounts: List<Account> = emptyList(), val loading: Boolean = true, val error: String? = null)
class AccountsStore(private val api: () -> BankApi) {
    private val mutable = MutableStateFlow(AccountState())
    val state = mutable.asStateFlow()
    suspend fun refresh() {
        mutable.value = mutable.value.copy(loading = true, error = null)
        try { mutable.value = AccountState(api().accounts(), false) }
        catch (e: Exception) { if (e is CancellationException) throw e; mutable.value = mutable.value.copy(loading = false, error = e.message ?: "unknown error") }
    }
}
data class Banner(val text: String, val error: Boolean = false)
open class ScreenViewModel : ViewModel() {
    var banner by mutableStateOf<Banner?>(null)
    var pending by mutableStateOf(false)
    fun notify(text: String, error: Boolean = false) { banner = Banner(text, error) }
    fun execute(fallback: String, block: suspend () -> Unit) {
        if (pending) return
        viewModelScope.launch {
            pending = true
            try { block() } catch (e: Exception) { if (e is CancellationException) throw e; notify(if (e is ApiError) e.message.orEmpty() else fallback, true) }
            finally { pending = false }
        }
    }
}
class DashboardViewModel(val store: AccountsStore) : ViewModel() {
    init { viewModelScope.launch { store.refresh() } }
}
class AccountsViewModel(private val api: () -> BankApi, val store: AccountsStore) : ScreenViewModel() {
    var creating by mutableStateOf(false)
    var holder by mutableStateOf("")
    var balance by mutableStateOf("0")
    var currency by mutableStateOf("USD")
    var depositTarget by mutableStateOf<Account?>(null)
    var amount by mutableStateOf("")
    var deleteTarget by mutableStateOf<Account?>(null)
    init { viewModelScope.launch { store.refresh() } }
    fun create() {
        if (!validAccount(holder, balance)) { notify("Provide a holder name and a balance of 0 or more.", true); return }
        execute("Failed to create account") {
            notify(api().create(NewAccount(holder.trim(), balance.toDouble(), currency)))
            creating = false; holder = ""; balance = "0"; currency = "USD"; store.refresh()
        }
    }
    fun deposit() {
        val target = depositTarget ?: return
        if (!validDeposit(amount)) { notify("Provide an amount greater than 0.", true); return }
        execute("Failed to add funds") {
            val updated = api().deposit(target.id, amount.toDouble())
            notify("Added funds to ${target.accountHolderName}. New balance: ${formatCurrency(updated.balance, updated.currency)}.")
            depositTarget = null; amount = ""; store.refresh()
        }
    }
    fun delete() {
        val target = deleteTarget ?: return
        deleteTarget = null
        execute("Failed to delete account") { notify(api().delete(target.accountHolderName)); store.refresh() }
    }
}
class AssetsViewModel(private val api: () -> BankApi) : ScreenViewModel() {
    var input by mutableStateOf("")
    var results by mutableStateOf(emptyList<AssetAvailability>())
    fun check() {
        val codes = parseCodes(input)
        if (codes.isEmpty()) { notify("Enter at least one asset code.", true); return }
        execute("Failed to check asset availability") { results = api().assets(codes) }
    }
}
class TransactionViewModel(private val api: () -> BankApi) : ScreenViewModel() {
    var rows by mutableStateOf(listOf(ItemRow()))
    fun edit(index: Int, row: ItemRow) { rows = rows.mapIndexed { i, old -> if (i == index) row.copy(availability = null) else old } }
    fun check() {
        val codes = rows.map { it.code.trim() }.filter { it.isNotEmpty() }
        if (codes.isEmpty()) { notify("Enter at least one asset code first.", true); return }
        execute("Failed to check asset availability") {
            val map = api().assets(codes).associate { it.assetCode to it.isAssetAvailable }
            rows = rows.map { it.copy(availability = map[it.code]) }
        }
    }
    fun submit() {
        val items = parseItems(rows)
        if (items == null) {
            notify("Each item needs an asset code and a positive integer value.", true); return
        }
        execute("Transaction failed") { val message = api().transaction(items); notify(message, transactionIsError(message)) }
    }
}
class SettingsViewModel(val settings: SettingsRepository, private val store: AccountsStore) : ScreenViewModel() {
    var mockMode by mutableStateOf(settings.mockMode)
    var baseUrl by mutableStateOf(settings.baseUrl)
    var token by mutableStateOf(settings.token.orEmpty())
    fun toggle(value: Boolean) { settings.mockMode = value; mockMode = value; viewModelScope.launch { store.refresh() } }
    fun saveUrl() { settings.baseUrl = baseUrl; baseUrl = settings.baseUrl; notify("Base URL saved."); viewModelScope.launch { store.refresh() } }
    fun saveToken() { settings.token = token; notify("API token saved.") }
    fun clearToken() { settings.token = null; token = ""; notify("API token cleared.") }
}
