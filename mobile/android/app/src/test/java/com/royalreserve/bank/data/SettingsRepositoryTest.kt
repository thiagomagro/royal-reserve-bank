package com.royalreserve.bank.data

import com.royalreserve.bank.MemoryPreferences
import org.junit.Assert.*
import org.junit.Test

class SettingsRepositoryTest {
    private val preferences = MemoryPreferences()
    private val settings = SettingsRepository(preferences)

    @Test
    fun defaultSettingsUseOfflineMockAndEmulatorGateway() {
        assertTrue(settings.mockMode)
        assertEquals("http://10.0.2.2:8080", settings.baseUrl)
        assertNull(settings.token)
    }

    @Test
    fun savedBaseUrlIsTrimmedAndTrailingSlashesAreRemoved() {
        settings.baseUrl = "  http://gateway:8080///  "
        assertEquals("http://gateway:8080", SettingsRepository(preferences).baseUrl)
    }

    @Test
    fun tokenIsTrimmedAndEmptySavedTokenIsDistinctFromClearedToken() {
        settings.token = "  jwt\n"
        assertEquals("jwt", SettingsRepository(preferences).token)
        settings.token = " "
        assertEquals("", SettingsRepository(preferences).token)
        assertTrue(preferences.contains("token"))
        settings.token = null
        assertNull(SettingsRepository(preferences).token)
        assertFalse(preferences.contains("token"))
    }

    @Test
    fun mockModePersistsAcrossRepositoryInstances() {
        settings.mockMode = false
        assertFalse(SettingsRepository(preferences).mockMode)
    }
}
