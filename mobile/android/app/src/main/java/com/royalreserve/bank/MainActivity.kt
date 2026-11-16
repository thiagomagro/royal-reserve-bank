package com.royalreserve.bank

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = SettingsRepository(applicationContext)
        val repo = BankRepository(settings)
        val store = AccountsStore(repo::api)
        setContent { BankApp(settings, repo, store) }
    }
}
@Composable private inline fun <reified T : ViewModel> screenModel(crossinline create: () -> T): T = viewModel(factory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST") override fun <M : ViewModel> create(modelClass: Class<M>): M = create() as M
})
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun BankApp(settings: SettingsRepository, repo: BankRepository, store: AccountsStore) {
    MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF0F172A), background = Color(0xFFF8FAFC), surface = Color.White, onSurface = Color(0xFF0F172A))) {
        val nav = rememberNavController()
        val current by nav.currentBackStackEntryAsState()
        val destinations = listOf("Dashboard", "Accounts", "Transaction", "Assets", "Settings")
        val icons = listOf(Icons.Default.Dashboard, Icons.Default.AccountBalance, Icons.Default.SwapHoriz, Icons.Default.ShowChart, Icons.Default.Settings)
        Scaffold(topBar = { TopAppBar(title = { Text("Royal Reserve Bank") }) }, bottomBar = {
            NavigationBar { destinations.forEachIndexed { i, title ->
                NavigationBarItem(selected = current?.destination?.route == title, onClick = { nav.navigate(title) { popUpTo(nav.graph.startDestinationId) { saveState = true }; launchSingleTop = true; restoreState = true } }, icon = { Icon(icons[i], contentDescription = title) }, label = { Text(title, maxLines = 1, style = MaterialTheme.typography.labelSmall) }, modifier = Modifier.testTag("nav_$title"))
            } }
        }) { padding ->
            NavHost(nav, "Dashboard", Modifier.padding(padding)) {
                composable("Dashboard") { val vm = screenModel { DashboardViewModel(store) }; Dashboard(vm) { nav.navigate(it) } }
                composable("Accounts") { Accounts(screenModel { AccountsViewModel(repo::api, store) }) }
                composable("Transaction") { Transaction(screenModel { TransactionViewModel(repo::api) }) }
                composable("Assets") { Assets(screenModel { AssetsViewModel(repo::api) }) }
                composable("Settings") { Settings(screenModel { SettingsViewModel(settings, store) }) }
            }
        }
    }
}
@Composable private fun Page(title: String, banner: Banner? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        BannerView(banner)
        content()
    }
}
@Composable private fun BannerView(banner: Banner?) {
    banner?.let { Text(it.text, color = if (it.error) Color(0xFFB91C1C) else Color(0xFF047857), modifier = Modifier.fillMaxWidth().background(if (it.error) Color(0xFFFEE2E2) else Color(0xFFD1FAE5)).padding(12.dp).testTag("banner")) }
}
@Composable private fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content) }
}
@Composable private fun Action(text: String, enabled: Boolean = true, onClick: () -> Unit) { Button(onClick, enabled = enabled, modifier = Modifier.testTag(text)) { Text(text) } }
@Composable private fun Field(label: String, value: String, numeric: Boolean = false, multiline: Boolean = false, autofocus: Boolean = false, change: (String) -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(autofocus) { if (autofocus) focus.requestFocus() }
    OutlinedTextField(value, change, label = { Text(label) }, modifier = Modifier.fillMaxWidth().testTag(label).focusRequester(focus), singleLine = !multiline, minLines = if (multiline) 4 else 1, keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text))
}
@Composable private fun AccountRow(account: Account) {
    Text(account.accountHolderName, style = MaterialTheme.typography.titleMedium)
    Text("${account.accountNumber}  •  ${formatCurrency(account.balance, account.currency)}  •  ${account.currency}")
}
@Composable private fun Dashboard(vm: DashboardViewModel, navigate: (String) -> Unit) {
    val state by vm.store.state.collectAsState()
    Page("Dashboard") {
        if (state.loading) Text("Loading dashboard…")
        else if (state.error != null) BannerView(Banner("Failed to load accounts: ${state.error}", true))
        else {
            Panel { Text("Accounts"); Text("${state.accounts.size}", style = MaterialTheme.typography.headlineLarge) }
            state.accounts.groupBy { it.currency }.forEach { (currency, list) -> Panel { Text("Total balance ($currency)"); Text(formatCurrency(list.sumOf { it.balance }, currency), style = MaterialTheme.typography.headlineSmall) } }
            Panel { Text("Top accounts", style = MaterialTheme.typography.titleLarge); Text("Holder / Account # / Balance"); state.accounts.sortedByDescending { it.balance }.take(5).forEach { AccountRow(it); HorizontalDivider() } }
            Action("Manage accounts") { navigate("Accounts") }; Action("New transaction") { navigate("Transaction") }; Action("Check assets") { navigate("Assets") }
        }
    }
}
@Composable private fun Accounts(vm: AccountsViewModel) {
    val state by vm.store.state.collectAsState()
    Page("Accounts", vm.banner) {
        Action("New account", !vm.pending) { vm.creating = true }
        if (state.loading) Text("Loading accounts…")
        else if (state.error != null) BannerView(Banner("Failed to load accounts: ${state.error}", true))
        else Panel {
            Text("Holder / Account # / Balance / Currency")
            state.accounts.forEach { account ->
                AccountRow(account)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Action("Add funds", !vm.pending) { vm.amount = ""; vm.depositTarget = account }
                    OutlinedButton(onClick = { vm.deleteTarget = account }, enabled = !vm.pending) { Text("Delete") }
                }; HorizontalDivider()
            }
        }
    }
    if (vm.creating) AlertDialog(onDismissRequest = { if (!vm.pending) vm.creating = false }, title = { Text("New account") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BannerView(vm.banner?.takeIf { it.error })
            Field("Account holder name", vm.holder) { vm.holder = it }; Field("Initial balance", vm.balance, numeric = true) { vm.balance = it }
            var expanded by remember { mutableStateOf(false) }
            Box { OutlinedButton(onClick = { expanded = true }, modifier = Modifier.testTag("Currency")) { Text("Currency: ${vm.currency}") }; DropdownMenu(expanded, { expanded = false }) { listOf("USD", "EUR", "GBP", "BRL").forEach { code -> DropdownMenuItem(text = { Text(code) }, onClick = { vm.currency = code; expanded = false }) } } }
        }
    }, confirmButton = { Action(if (vm.pending) "Creating…" else "Create account", !vm.pending, vm::create) }, dismissButton = { TextButton(onClick = { vm.creating = false }, enabled = !vm.pending) { Text("Cancel") } })
    vm.depositTarget?.let { account -> AlertDialog(onDismissRequest = { if (!vm.pending) { vm.depositTarget = null; vm.amount = "" } }, title = { Text("Add funds") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { BannerView(vm.banner?.takeIf { it.error }); Text(account.accountHolderName); Text(formatCurrency(account.balance, account.currency)); Field("Amount (${account.currency})", vm.amount, numeric = true, autofocus = true) { vm.amount = it } }
    }, confirmButton = { Action(if (vm.pending) "Adding…" else "Confirm deposit", !vm.pending, vm::deposit) }, dismissButton = { TextButton(onClick = { vm.depositTarget = null; vm.amount = "" }, enabled = !vm.pending) { Text("Cancel") } }) }
    vm.deleteTarget?.let { account ->
        val cancelFocus = remember { FocusRequester() }
        LaunchedEffect(account) { cancelFocus.requestFocus() }
        AlertDialog(onDismissRequest = { vm.deleteTarget = null }, title = { Text("Delete account") }, text = { Text("Delete the account for ${account.accountHolderName}?") }, confirmButton = { Action("Delete", !vm.pending, vm::delete) }, dismissButton = { TextButton(onClick = { vm.deleteTarget = null }, modifier = Modifier.focusRequester(cancelFocus)) { Text("Cancel") } })
    }
}
@Composable private fun Availability(value: Boolean) { Text(if (value) "Available" else "Unavailable", color = if (value) Color(0xFF047857) else Color(0xFFB91C1C), modifier = Modifier.background(if (value) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)).padding(6.dp)) }
@Composable private fun Transaction(vm: TransactionViewModel) {
    Page("New transaction", vm.banner) {
        vm.rows.forEachIndexed { index, row -> Panel {
            Field("Asset code", row.code) { vm.edit(index, row.copy(code = it)) }; Field("Asset name", row.name) { vm.edit(index, row.copy(name = it)) }; Field("Value", row.value, numeric = true) { vm.edit(index, row.copy(value = it)) }
            row.availability?.let { Availability(it) }
            OutlinedButton(onClick = { vm.rows = vm.rows.filterIndexed { i, _ -> i != index } }, enabled = vm.rows.size > 1) { Text("Remove") }
        } }
        Action("Add item") { vm.rows = vm.rows + ItemRow() }
        Action(if (vm.pending) "Checking…" else "Check availability", !vm.pending, vm::check)
        Action(if (vm.pending) "Submitting…" else "Submit transaction", !vm.pending, vm::submit)
    }
}
@Composable private fun Assets(vm: AssetsViewModel) {
    Page("Assets", vm.banner) {
        Field("PETR4, VALE3, AAPL", vm.input) { vm.input = it }
        Action(if (vm.pending) "Checking…" else "Check availability", !vm.pending, vm::check)
        if (vm.results.isNotEmpty()) Panel { Text("Asset code / Status"); vm.results.forEach { Text(it.assetCode); Availability(it.isAssetAvailable); HorizontalDivider() } }
    }
}
@Composable private fun Settings(vm: SettingsViewModel) {
    Page("Settings", vm.banner) {
        Panel { Text("API mock mode: ${if (vm.mockMode) "enabled" else "disabled"}"); Row { Text("Offline mock mode", Modifier.weight(1f)); Switch(vm.mockMode, vm::toggle, modifier = Modifier.testTag("mockMode")) } }
        Panel { Field("Base URL", vm.baseUrl) { vm.baseUrl = it }; Action("Save base URL", onClick = vm::saveUrl) }
        Panel { Field("API token (Auth0 JWT)", vm.token, multiline = true) { vm.token = it }; Text("The token is stored on this device and sent as a Bearer token on every API request."); Action("Save token", onClick = vm::saveToken); Action("Clear token", onClick = vm::clearToken) }
    }
}
