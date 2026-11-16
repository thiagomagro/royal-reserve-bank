package com.royalreserve.bank.data

import com.royalreserve.bank.MemoryPreferences
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test

class BankRepositoryTest {
    @Test
    fun currentSettingsSelectApiAtCallTimeAndRetainMockStore() = runTest {
        val settings = SettingsRepository(MemoryPreferences())
        val mock = MockBankApi()
        val repository = BankRepository(settings, mock)
        assertSame(mock, repository.api())
        repository.api().create(NewAccount("Jane", 0.0, "USD"))
        settings.mockMode = false
        assertTrue(repository.api() is HttpBankApi)
        settings.mockMode = true
        assertEquals("Jane", repository.api().accounts().last().accountHolderName)
    }

    @Test
    fun realApiUsesLatestBaseUrlAndTokenOnEveryCall() = runTest {
        val first = MockWebServer()
        val second = MockWebServer()
        first.start()
        second.start()
        try {
            val settings = SettingsRepository(MemoryPreferences())
            val repository = BankRepository(settings)
            settings.mockMode = false
            settings.baseUrl = first.url("").toString()
            settings.token = "first"
            first.enqueue(MockResponse().setBody("[]"))
            repository.api().accounts()
            assertEquals("Bearer first", first.takeRequest().getHeader("Authorization"))
            settings.baseUrl = second.url("").toString()
            settings.token = null
            second.enqueue(MockResponse().setBody("[]"))
            repository.api().accounts()
            assertNull(second.takeRequest().getHeader("Authorization"))
        } finally {
            first.shutdown()
            second.shutdown()
        }
    }
}
