package com.royalreserve.bank.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.royalreserve.bank.data.AccountsStore
import com.royalreserve.bank.data.BankRepository
import com.royalreserve.bank.data.SettingsRepository
import com.royalreserve.bank.ui.screens.Accounts
import com.royalreserve.bank.ui.screens.AccountsViewModel
import com.royalreserve.bank.ui.screens.Assets
import com.royalreserve.bank.ui.screens.AssetsViewModel
import com.royalreserve.bank.ui.screens.Dashboard
import com.royalreserve.bank.ui.screens.DashboardViewModel
import com.royalreserve.bank.ui.screens.Settings
import com.royalreserve.bank.ui.screens.SettingsViewModel
import com.royalreserve.bank.ui.screens.Transaction
import com.royalreserve.bank.ui.screens.TransactionViewModel
import com.royalreserve.bank.ui.theme.BankTheme

@Composable
private inline fun <reified T : ViewModel> screenModel(crossinline create: () -> T): T =
    viewModel(
        factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <M : ViewModel> create(modelClass: Class<M>): M = create() as M
            }
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankApp(settings: SettingsRepository, repo: BankRepository, store: AccountsStore) {
    BankTheme {
        val nav = rememberNavController()
        val current by nav.currentBackStackEntryAsState()
        val destinations = listOf("Dashboard", "Accounts", "Transaction", "Assets", "Settings")
        val icons =
            listOf(
                Icons.Default.Dashboard,
                Icons.Default.AccountBalance,
                Icons.Default.SwapHoriz,
                Icons.Default.ShowChart,
                Icons.Default.Settings,
            )
        Scaffold(
            topBar = { TopAppBar(title = { Text("Royal Reserve Bank") }) },
            bottomBar = {
                NavigationBar {
                    destinations.forEachIndexed { i, title ->
                        NavigationBarItem(
                            selected = current?.destination?.route == title,
                            onClick = {
                                nav.navigate(title) {
                                    popUpTo(nav.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(icons[i], contentDescription = title) },
                            label = {
                                Text(
                                    title,
                                    maxLines = 1,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                            modifier = Modifier.testTag("nav_$title"),
                        )
                    }
                }
            },
        ) { padding ->
            NavHost(nav, "Dashboard", Modifier.padding(padding)) {
                composable("Dashboard") {
                    val vm = screenModel { DashboardViewModel(store) }
                    Dashboard(vm) { nav.navigate(it) }
                }
                composable("Accounts") {
                    Accounts(screenModel { AccountsViewModel(repo::api, store) })
                }
                composable("Transaction") {
                    Transaction(screenModel { TransactionViewModel(repo::api) })
                }
                composable("Assets") { Assets(screenModel { AssetsViewModel(repo::api) }) }
                composable("Settings") {
                    Settings(screenModel { SettingsViewModel(settings, store) })
                }
            }
        }
    }
}
