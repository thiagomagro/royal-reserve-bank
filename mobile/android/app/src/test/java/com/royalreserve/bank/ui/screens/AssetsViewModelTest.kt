package com.royalreserve.bank.ui.screens

import com.royalreserve.bank.MainDispatcherRule
import com.royalreserve.bank.data.ApiError
import com.royalreserve.bank.data.AssetAvailability
import com.royalreserve.bank.data.BankApi
import com.royalreserve.bank.data.MockBankApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AssetsViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test
    fun emptyInputShowsExactMessageAndHasNoResults() = runTest {
        val vm = AssetsViewModel { MockBankApi() }
        vm.check()
        assertEquals("Enter at least one asset code.", vm.banner!!.text)
        assertTrue(vm.banner!!.error)
        assertTrue(vm.results.isEmpty())
    }

    @Test
    fun successfulResultsPreserveOrderAndDuplicates() = runTest {
        val vm = AssetsViewModel { MockBankApi() }
        vm.input = "AAPL, unknown AAPL"
        vm.check()
        advanceUntilIdle()
        assertEquals(
            listOf(
                AssetAvailability("AAPL", true),
                AssetAvailability("unknown", false),
                AssetAvailability("AAPL", true),
            ),
            vm.results,
        )
    }

    @Test
    fun failurePreservesPriorResultsAndShowsRawError() = runTest {
        val api =
            object : BankApi by MockBankApi() {
                override suspend fun assets(codes: List<String>): List<AssetAvailability> =
                    throw ApiError(400, "raw asset error")
            }
        val vm = AssetsViewModel { api }
        val previous = listOf(AssetAvailability("AAPL", true))
        vm.results = previous
        vm.input = "AAPL"
        vm.check()
        advanceUntilIdle()
        assertEquals(previous, vm.results)
        assertEquals("raw asset error", vm.banner!!.text)
        assertTrue(vm.banner!!.error)
    }
}
