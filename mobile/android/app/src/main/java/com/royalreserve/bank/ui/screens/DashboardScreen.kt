package com.royalreserve.bank.ui.screens

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.royalreserve.bank.data.AccountsStore
import com.royalreserve.bank.ui.components.AccountRow
import com.royalreserve.bank.ui.components.Action
import com.royalreserve.bank.ui.components.Banner
import com.royalreserve.bank.ui.components.BannerView
import com.royalreserve.bank.ui.components.Page
import com.royalreserve.bank.ui.components.Panel
import com.royalreserve.bank.util.formatCurrency
import kotlinx.coroutines.launch

@Composable
internal fun Dashboard(vm: DashboardViewModel, navigate: (String) -> Unit) {
    val state by vm.store.state.collectAsState()
    Page("Dashboard") {
        if (state.loading) Text("Loading dashboard…")
        else if (state.error != null)
            BannerView(Banner("Failed to load accounts: ${state.error}", true))
        else {
            Panel {
                Text("Accounts")
                Text("${state.accounts.size}", style = MaterialTheme.typography.headlineLarge)
            }
            state.accounts
                .groupBy { it.currency }
                .forEach { (currency, list) ->
                    Panel {
                        Text("Total balance ($currency)")
                        Text(
                            formatCurrency(list.sumOf { it.balance }, currency),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    }
                }
            Panel {
                Text("Top accounts", style = MaterialTheme.typography.titleLarge)
                Text("Holder / Account # / Balance")
                state.accounts
                    .sortedByDescending { it.balance }
                    .take(5)
                    .forEach {
                        AccountRow(it)
                        HorizontalDivider()
                    }
            }
            Action("Manage accounts") { navigate("Accounts") }
            Action("New transaction") { navigate("Transaction") }
            Action("Check assets") { navigate("Assets") }
        }
    }
}

class DashboardViewModel(val store: AccountsStore) : ViewModel() {
    init {
        viewModelScope.launch { store.refresh() }
    }
}
