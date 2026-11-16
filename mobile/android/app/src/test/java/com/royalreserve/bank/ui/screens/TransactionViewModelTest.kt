package com.royalreserve.bank.ui.screens

import com.royalreserve.bank.MainDispatcherRule
import com.royalreserve.bank.data.ApiError
import com.royalreserve.bank.data.BankApi
import com.royalreserve.bank.data.ItemRow
import com.royalreserve.bank.data.MockBankApi
import com.royalreserve.bank.data.TransactionItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test
    fun initialRowHasBlankCodeAndNameAndValueOne() {
        assertEquals(
            listOf(ItemRow("", "", "1", null)),
            TransactionViewModel { MockBankApi() }.rows,
        )
    }

    @Test
    fun oopsFallbackIsDisplayedAsErrorAndRowsRemain() = runTest {
        val vm = TransactionViewModel { MockBankApi() }
        val rows = listOf(ItemRow("AAPL"), ItemRow("missing"))
        vm.rows = rows
        vm.submit()
        advanceUntilIdle()
        assertEquals("Oops! Something went wrong, please try again later!", vm.banner!!.text)
        assertTrue(vm.banner!!.error)
        assertEquals(rows, vm.rows)
        assertFalse(vm.pending)
    }

    @Test
    fun successDoesNotRequireAvailabilityCheckOrResetRows() = runTest {
        val vm = TransactionViewModel { MockBankApi() }
        vm.submit()
        assertTrue(vm.banner!!.error)
        val rows = listOf(ItemRow("AAPL", "Apple", "2"), ItemRow("PETR4"))
        vm.rows = rows
        vm.submit()
        advanceUntilIdle()
        assertEquals("Transaction placed successfully.", vm.banner!!.text)
        assertFalse(vm.banner!!.error)
        assertEquals(rows, vm.rows)
    }

    @Test
    fun successfulAvailabilityCheckClearsPreviousValidationBanner() = runTest {
        val vm = TransactionViewModel { MockBankApi() }
        vm.check()
        assertEquals("Enter at least one asset code first.", vm.banner!!.text)

        vm.rows = listOf(ItemRow("AAPL"))
        vm.check()
        advanceUntilIdle()

        assertNull(vm.banner)
        assertEquals(true, vm.rows.single().availability)
    }

    @Test
    fun successfulAvailabilityCheckClearsPreviousOopsBanner() = runTest {
        val vm = TransactionViewModel { MockBankApi() }
        vm.rows = listOf(ItemRow("missing"))
        vm.submit()
        advanceUntilIdle()
        assertTrue(vm.banner!!.error)

        vm.rows = listOf(ItemRow("AAPL"))
        vm.check()
        advanceUntilIdle()

        assertNull(vm.banner)
        assertEquals(true, vm.rows.single().availability)
    }

    @Test
    fun emptySubmissionAndCheckHaveExactMessages() = runTest {
        val vm = TransactionViewModel { MockBankApi() }
        vm.check()
        assertEquals("Enter at least one asset code first.", vm.banner!!.text)
        vm.submit()
        assertEquals(
            "Each item needs an asset code and a positive integer value.",
            vm.banner!!.text,
        )
        assertTrue(vm.banner!!.error)
    }

    @Test
    fun availabilityMapsByUntrimmedCodeAndEditingClearsOnlyThatRow() = runTest {
        val vm = TransactionViewModel { MockBankApi() }
        vm.rows = listOf(ItemRow("AAPL"), ItemRow(" PETR4 "), ItemRow("bad"))
        vm.check()
        advanceUntilIdle()
        assertEquals(listOf(true, null, false), vm.rows.map { it.availability })
        vm.edit(0, vm.rows[0].copy(name = "Apple"))
        assertEquals(listOf(null, null, false), vm.rows.map { it.availability })
    }

    @Test
    fun apiFailureIsDisplayedVerbatim() = runTest {
        val api =
            object : BankApi by MockBankApi() {
                override suspend fun transaction(items: List<TransactionItem>): String =
                    throw ApiError(503, "raw service failure")
            }
        val vm = TransactionViewModel { api }
        vm.rows = listOf(ItemRow("AAPL"))
        vm.submit()
        advanceUntilIdle()
        assertEquals("raw service failure", vm.banner!!.text)
        assertTrue(vm.banner!!.error)
        assertFalse(vm.pending)
    }
}
