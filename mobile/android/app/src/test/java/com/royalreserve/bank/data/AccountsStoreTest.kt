package com.royalreserve.bank.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class AccountsStoreTest {
    @Test
    fun initialStateIsLoadingAndRefreshSupportsEmptyAccounts() = runTest {
        val api = MockBankApi()
        api.accounts().forEach { api.delete(it.accountHolderName) }
        val store = AccountsStore { api }
        assertTrue(store.state.value.loading)
        store.refresh()
        assertFalse(store.state.value.loading)
        assertNull(store.state.value.error)
        assertTrue(store.state.value.accounts.isEmpty())
    }

    @Test
    fun failureIsExposedAsRawErrorAndLoadingEnds() = runTest {
        val api =
            object : BankApi by MockBankApi() {
                override suspend fun accounts(): List<Account> =
                    throw ApiError(401, "raw gateway error")
            }
        val store = AccountsStore { api }
        store.refresh()
        assertFalse(store.state.value.loading)
        assertEquals("raw gateway error", store.state.value.error)
    }
}
