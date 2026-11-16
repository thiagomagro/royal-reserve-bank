package com.royalreserve.bank.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.royalreserve.bank.data.Account
import com.royalreserve.bank.util.formatCurrency

@Composable
internal fun AccountRow(account: Account) {
    Text(account.accountHolderName, style = MaterialTheme.typography.titleMedium)
    Text(
        "${account.accountNumber}  •  ${formatCurrency(account.balance, account.currency)}  •  ${account.currency}"
    )
}
