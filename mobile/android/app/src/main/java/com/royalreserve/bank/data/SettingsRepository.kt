package com.royalreserve.bank.data

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(private val prefs: SharedPreferences) {
    constructor(
        context: Context
    ) : this(context.getSharedPreferences("rrb.settings", Context.MODE_PRIVATE))

    var mockMode: Boolean
        get() = prefs.getBoolean("mockMode", true)
        set(value) {
            prefs.edit().putBoolean("mockMode", value).apply()
        }

    var baseUrl: String
        get() = prefs.getString("baseUrl", "http://10.0.2.2:8080")!!
        set(value) {
            prefs.edit().putString("baseUrl", value.trim().trimEnd('/')).apply()
        }

    var token: String?
        get() = prefs.getString("token", null)
        set(value) {
            prefs
                .edit()
                .apply { if (value == null) remove("token") else putString("token", value.trim()) }
                .apply()
        }
}
