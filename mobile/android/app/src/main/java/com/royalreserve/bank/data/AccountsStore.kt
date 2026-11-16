package com.royalreserve.bank.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AccountState(
    val accounts: List<Account> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)

class AccountsStore(private val api: () -> BankApi) {
    private val mutable = MutableStateFlow(AccountState())
    val state = mutable.asStateFlow()

    suspend fun refresh() {
        mutable.value = mutable.value.copy(loading = true, error = null)
        try {
            mutable.value = AccountState(api().accounts(), false)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            mutable.value =
                mutable.value.copy(loading = false, error = e.message ?: "unknown error")
        }
    }
}
