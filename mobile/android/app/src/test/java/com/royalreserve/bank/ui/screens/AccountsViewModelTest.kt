package com.royalreserve.bank.ui.screens

import com.royalreserve.bank.MainDispatcherRule
import com.royalreserve.bank.data.Account
import com.royalreserve.bank.data.AccountsStore
import com.royalreserve.bank.data.ApiError
import com.royalreserve.bank.data.BankApi
import com.royalreserve.bank.data.MockBankApi
import com.royalreserve.bank.data.NewAccount
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountsViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test
    fun createSuccessResetsFieldsClosesDialogAndRefetchesSharedAccounts() = runTest {
        val api = CountingBankApi()
        val store = AccountsStore { api }
        val vm = AccountsViewModel({ api }, store)
        advanceUntilIdle()
        assertEquals(1, api.accountReads)
        vm.create()
        assertTrue(vm.banner!!.error)
        vm.creating = true
        vm.holder = " Jane "
        vm.balance = "12.50"
        vm.currency = "EUR"
        vm.create()
        advanceUntilIdle()
        assertFalse(vm.pending)
        assertFalse(vm.creating)
        assertEquals("", vm.holder)
        assertEquals("0", vm.balance)
        assertEquals("USD", vm.currency)
        assertEquals(2, api.accountReads)
        assertEquals(Account("4", "1004", "Jane", 12.5, "EUR"), store.state.value.accounts.last())
        assertEquals("Successfully set up a new bank account for Jane.", vm.banner!!.text)
        assertFalse(vm.banner!!.error)
    }

    @Test
    fun depositSuccessHasExactFormattedMessageAndRefetches() = runTest {
        val api = CountingBankApi()
        val store = AccountsStore { api }
        val vm = AccountsViewModel({ api }, store)
        advanceUntilIdle()
        vm.depositTarget = store.state.value.accounts.first()
        vm.deposit()
        assertTrue(vm.banner!!.error)
        vm.amount = "25.50"
        vm.deposit()
        advanceUntilIdle()
        assertEquals("Added funds to Alice Johnson. New balance: $12,526.00.", vm.banner!!.text)
        assertFalse(vm.banner!!.error)
        assertNull(vm.depositTarget)
        assertEquals("", vm.amount)
        assertEquals(2, api.accountReads)
        assertEquals(12526.0, store.state.value.accounts.first().balance, 0.0)
    }

    @Test
    fun createErrorPreservesDialogFieldsAndRawApiMessage() = runTest {
        val api =
            object : BankApi by MockBankApi() {
                override suspend fun create(account: NewAccount): String =
                    throw ApiError(400, "raw error")
            }
        val store = AccountsStore { api }
        val vm = AccountsViewModel({ api }, store)
        advanceUntilIdle()
        vm.creating = true
        vm.holder = "Jane"
        vm.balance = "2"
        vm.create()
        advanceUntilIdle()
        assertTrue(vm.creating)
        assertEquals("Jane", vm.holder)
        assertEquals("2", vm.balance)
        assertEquals("raw error", vm.banner!!.text)
        assertTrue(vm.banner!!.error)
        assertFalse(vm.pending)
        assertEquals(3, store.state.value.accounts.size)
    }

    @Test
    fun depositErrorPreservesInputAndUsesNonApiFallback() = runTest {
        val api =
            object : BankApi by MockBankApi() {
                override suspend fun deposit(id: String, amount: Double): Account =
                    error("network detail")
            }
        val store = AccountsStore { api }
        val vm = AccountsViewModel({ api }, store)
        advanceUntilIdle()
        vm.depositTarget = store.state.value.accounts.first()
        vm.amount = "1"
        vm.deposit()
        advanceUntilIdle()
        assertEquals("Failed to add funds", vm.banner!!.text)
        assertNotNull(vm.depositTarget)
        assertEquals("1", vm.amount)
        assertFalse(vm.pending)
    }

    @Test
    fun invalidMutationsShowExactMessagesWithoutCallingApi() = runTest {
        val api = CountingBankApi()
        val store = AccountsStore { api }
        val vm = AccountsViewModel({ api }, store)
        advanceUntilIdle()
        vm.create()
        assertEquals("Provide a holder name and a balance of 0 or more.", vm.banner!!.text)
        vm.depositTarget = store.state.value.accounts.first()
        vm.amount = "0"
        vm.deposit()
        assertEquals("Provide an amount greater than 0.", vm.banner!!.text)
        assertTrue(vm.banner!!.error)
        advanceUntilIdle()
        assertEquals(1, api.accountReads)
        assertEquals(3, store.state.value.accounts.size)
    }

    @Test
    fun deleteClosesConfirmationImmediatelyAndRefreshes() = runTest {
        val api = CountingBankApi()
        val store = AccountsStore { api }
        val vm = AccountsViewModel({ api }, store)
        advanceUntilIdle()
        vm.deleteTarget = store.state.value.accounts.first()
        vm.delete()
        assertNull(vm.deleteTarget)
        advanceUntilIdle()
        assertEquals("Successfully deleted the bank account for Alice Johnson.", vm.banner!!.text)
        assertEquals(2, store.state.value.accounts.size)
        assertEquals(2, api.accountReads)
    }

    private class CountingBankApi(private val mock: MockBankApi = MockBankApi()) : BankApi by mock {
        var accountReads = 0

        override suspend fun accounts(): List<Account> {
            accountReads++
            return mock.accounts()
        }
    }
}
