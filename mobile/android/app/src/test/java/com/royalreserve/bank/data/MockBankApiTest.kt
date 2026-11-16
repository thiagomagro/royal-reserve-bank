package com.royalreserve.bank.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class MockBankApiTest {
    private val api = MockBankApi()

    @Test
    fun seedsHaveExactOrderAndFields() = runTest {
        assertEquals(
            listOf(
                Account("1", "1001", "Alice Johnson", 12500.5, "USD"),
                Account("2", "1002", "Bruno Souza", 8400.0, "BRL"),
                Account("3", "1003", "Carla Mendes", 3200.0, "EUR"),
            ),
            api.accounts(),
        )
    }

    @Test
    fun createStartsWithIndependentCountersAndAppends() = runTest {
        val message = api.create(NewAccount("Jane", 12.5, "GBP"))
        assertEquals("Successfully set up a new bank account for Jane.", message)
        assertEquals(Account("4", "1004", "Jane", 12.5, "GBP"), api.accounts().last())
        assertEquals(4, api.accounts().size)
    }

    @Test
    fun createDoesNotReuseDeletedCounters() = runTest {
        api.create(NewAccount("Jane", 0.0, "USD"))
        api.delete("Jane")
        api.create(NewAccount("Jane", 0.0, "USD"))
        assertEquals("5", api.accounts().last().id)
        assertEquals("1005", api.accounts().last().accountNumber)
    }

    @Test
    fun resetRestoresSeedsAndBothCounters() = runTest {
        api.create(NewAccount("Jane", 0.0, "USD"))
        api.delete("Alice Johnson")
        api.reset()
        assertEquals(3, api.accounts().size)
        assertEquals("Alice Johnson", api.accounts().first().accountHolderName)
        api.create(NewAccount("Jane", 0.0, "USD"))
        assertEquals("4", api.accounts().last().id)
        assertEquals("1004", api.accounts().last().accountNumber)
    }

    @Test
    fun deleteRemovesFirstExactMatchAndReturnsMockText() = runTest {
        api.create(NewAccount("Alice Johnson", 1.0, "USD"))
        assertEquals(
            "Successfully deleted the bank account for Alice Johnson.",
            api.delete("Alice Johnson"),
        )
        assertEquals(listOf("2", "3", "4"), api.accounts().map { it.id })
    }

    @Test
    fun deleteIsCaseSensitiveAndPreservesStoreOn404() = runTest {
        expectError(404, "No bank account found for alice johnson.") { api.delete("alice johnson") }
        assertEquals(3, api.accounts().size)
    }

    @Test
    fun deletingEveryAccountReturnsEmptyList() = runTest {
        api.accounts().forEach { api.delete(it.accountHolderName) }
        assertEquals(emptyList<Account>(), api.accounts())
    }

    @Test
    fun depositReturnsUpdatedAccountAndRoundsToCents() = runTest {
        api.create(NewAccount("Jane", 1.0, "USD"))
        assertEquals(Account("4", "1004", "Jane", 1.13, "USD"), api.deposit("4", 0.125))
        assertEquals(1.13, api.accounts().last().balance, 0.0)
    }

    @Test
    fun depositRejectsInvalidAmountsBeforeLookingUpId() = runTest {
        for (amount in listOf(0.0, -1.0, Double.NaN)) {
            expectError(400, "The deposit amount must be greater than zero.") {
                api.deposit("missing", amount)
            }
        }
        assertEquals(12500.5, api.accounts().first().balance, 0.0)
    }

    @Test
    fun deposit404IncludesOpaqueRequestedId() = runTest {
        expectError(404, "No bank account found with id a/b c.") { api.deposit("a/b c", 1.0) }
    }

    @Test
    fun assetsPreserveDuplicatesUnknownsAndCaseSensitiveOrder() = runTest {
        val codes = listOf("AAPL", "aapl", "unknown", "AAPL", "PETR4", "VALE3", "BOND-BR-2030")
        val results = api.assets(codes)
        assertEquals(codes, results.map { it.assetCode })
        assertEquals(
            listOf(true, false, false, true, true, true, true),
            results.map { it.isAssetAvailable },
        )
    }

    @Test
    fun assetsAcceptEmptyInput() = runTest {
        assertEquals(emptyList<AssetAvailability>(), api.assets(emptyList()))
    }

    @Test
    fun transactionSucceedsWithoutMutatingAccountsOrDepletingAssets() = runTest {
        val before = api.accounts()
        val items = listOf(TransactionItem("AAPL", "Apple", 2), TransactionItem("PETR4", "", 1))
        repeat(2) { assertEquals("Transaction placed successfully.", api.transaction(items)) }
        assertEquals(before, api.accounts())
        assertTrue(api.assets(listOf("AAPL")).single().isAssetAvailable)
    }

    @Test
    fun transactionWithAnyUnavailableCodeReturnsOopsFallback() = runTest {
        val message =
            api.transaction(listOf(TransactionItem("AAPL", "", 1), TransactionItem("aapl", "", 1)))
        assertEquals("Oops! Something went wrong, please try again later!", message)
    }

    @Test
    fun emptyTransactionSucceedsWhenCalledDirectly() = runTest {
        assertEquals("Transaction placed successfully.", api.transaction(emptyList()))
    }

    private suspend fun expectError(status: Int, message: String, block: suspend () -> Unit) {
        try {
            block()
            fail("Expected ApiError")
        } catch (error: ApiError) {
            assertEquals(status, error.status)
            assertEquals(message, error.message)
        }
    }
}
