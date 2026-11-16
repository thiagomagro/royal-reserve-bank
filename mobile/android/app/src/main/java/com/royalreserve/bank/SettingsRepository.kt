package com.royalreserve.bank

import android.content.Context

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("rrb.settings", Context.MODE_PRIVATE)
    var mockMode: Boolean
        get() = prefs.getBoolean("mockMode", true)
        set(value) { prefs.edit().putBoolean("mockMode", value).apply() }
    var baseUrl: String
        get() = prefs.getString("baseUrl", "http://10.0.2.2:8080")!!
        set(value) { prefs.edit().putString("baseUrl", value.trim().trimEnd('/')).apply() }
    var token: String?
        get() = prefs.getString("token", null)
        set(value) { prefs.edit().apply { if (value == null) remove("token") else putString("token", value.trim()) }.apply() }
}

class BankRepository(private val settings: SettingsRepository, private val mock: BankApi = MockBankApi()) {
    fun api(): BankApi = if (settings.mockMode) mock else HttpBankApi(settings.baseUrl, settings.token)
}
