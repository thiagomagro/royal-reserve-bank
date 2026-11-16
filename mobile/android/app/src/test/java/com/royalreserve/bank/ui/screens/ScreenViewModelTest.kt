package com.royalreserve.bank.ui.screens

import com.royalreserve.bank.MainDispatcherRule
import com.royalreserve.bank.data.ApiError
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScreenViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test
    fun validRequestClearsOldBannerWhilePendingAndAfterSuccess() = runTest {
        val vm = ScreenViewModel()
        val response = CompletableDeferred<Unit>()
        vm.notify("Previous error", true)

        vm.execute("Failed") { response.await() }
        runCurrent()
        assertTrue(vm.pending)
        assertNull(vm.banner)

        response.complete(Unit)
        advanceUntilIdle()
        assertFalse(vm.pending)
        assertNull(vm.banner)
    }

    @Test
    fun successfulRequestRetainsItsOwnSuccessMessage() = runTest {
        val vm = ScreenViewModel()
        vm.notify("Previous error", true)

        vm.execute("Failed") { vm.notify("Success") }
        advanceUntilIdle()

        assertEquals("Success", vm.banner!!.text)
        assertFalse(vm.banner!!.error)
    }

    @Test
    fun failedRequestReplacesOldBannerWithNewRawError() = runTest {
        val vm = ScreenViewModel()
        vm.notify("Old success")

        vm.execute("Failed") { throw ApiError(400, "New raw error") }
        advanceUntilIdle()

        assertEquals("New raw error", vm.banner!!.text)
        assertTrue(vm.banner!!.error)
        assertFalse(vm.pending)
    }
}
