package com.royalreserve.bank

import android.content.SharedPreferences

class MemoryPreferences : SharedPreferences {
    private val values = mutableMapOf<String, Any?>()

    override fun getAll(): MutableMap<String, *> = values.toMutableMap()

    override fun getString(key: String?, defValue: String?) = values[key] as? String ?: defValue

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
        @Suppress("UNCHECKED_CAST")
        return (values[key] as? Set<String>)?.toMutableSet() ?: defValues
    }

    override fun getInt(key: String?, defValue: Int) = values[key] as? Int ?: defValue

    override fun getLong(key: String?, defValue: Long) = values[key] as? Long ?: defValue

    override fun getFloat(key: String?, defValue: Float) = values[key] as? Float ?: defValue

    override fun getBoolean(key: String?, defValue: Boolean) = values[key] as? Boolean ?: defValue

    override fun contains(key: String?) = values.containsKey(key)

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) = Unit

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) = Unit

    override fun edit(): SharedPreferences.Editor =
        object : SharedPreferences.Editor {
            private val updates = mutableMapOf<String, Any?>()
            private var clear = false

            private fun put(key: String?, value: Any?): SharedPreferences.Editor {
                updates[requireNotNull(key)] = value
                return this
            }

            override fun putString(key: String?, value: String?) = put(key, value)

            override fun putStringSet(key: String?, values: MutableSet<String>?) =
                put(key, values?.toSet())

            override fun putInt(key: String?, value: Int) = put(key, value)

            override fun putLong(key: String?, value: Long) = put(key, value)

            override fun putFloat(key: String?, value: Float) = put(key, value)

            override fun putBoolean(key: String?, value: Boolean) = put(key, value)

            override fun remove(key: String?) = put(key, null)

            override fun clear(): SharedPreferences.Editor {
                clear = true
                return this
            }

            override fun commit(): Boolean {
                if (clear) values.clear()
                updates.forEach { (key, value) ->
                    if (value == null) values.remove(key) else values[key] = value
                }
                return true
            }

            override fun apply() {
                commit()
            }
        }
}
