package com.royalreserve.bank.ui.screens

import com.royalreserve.bank.MainDispatcherRule
import com.royalreserve.bank.MemoryPreferences
import com.royalreserve.bank.data.AccountsStore
import com.royalreserve.bank.data.MockBankApi
import com.royalreserve.bank.data.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val preferences = MemoryPreferences()
    private val settings = SettingsRepository(preferences)
    private val api = MockBankApi()
    private val store = AccountsStore { api }

    @Test
    fun saveBaseUrlTrimsInputAndTrailingSlashesThenRefreshes() = runTest {
        val vm = SettingsViewModel(settings, store)
        vm.baseUrl = " http://gateway:8080/// "
        vm.saveUrl()
        advanceUntilIdle()
        assertEquals("http://gateway:8080", settings.baseUrl)
        assertEquals("http://gateway:8080", vm.baseUrl)
        assertEquals("Base URL saved.", vm.banner!!.text)
        assertEquals(3, store.state.value.accounts.size)
    }

    @Test
    fun saveTokenPersistsTrimmedTextAndExactSuccessMessage() {
        val vm = SettingsViewModel(settings, store)
        vm.token = "  pasted.jwt\n"
        vm.saveToken()
        assertEquals("pasted.jwt", SettingsRepository(preferences).token)
        assertEquals("API token saved.", vm.banner!!.text)
        assertFalse(vm.banner!!.error)
    }

    @Test
    fun clearTokenRemovesPreferenceAndClearsEditorWithExactMessage() {
        settings.token = "jwt"
        val vm = SettingsViewModel(settings, store)
        assertEquals("jwt", vm.token)
        vm.clearToken()
        assertEquals("", vm.token)
        assertNull(settings.token)
        assertFalse(preferences.contains("token"))
        assertEquals("API token cleared.", vm.banner!!.text)
    }

    @Test
    fun mockModeSwitchPersistsAtRuntimeAndRefreshesSharedState() = runTest {
        val vm = SettingsViewModel(settings, store)
        assertTrue(vm.mockMode)
        vm.toggle(false)
        advanceUntilIdle()
        assertFalse(vm.mockMode)
        assertFalse(SettingsRepository(preferences).mockMode)
        assertEquals(3, store.state.value.accounts.size)
    }
}
