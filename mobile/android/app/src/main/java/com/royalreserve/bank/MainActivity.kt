package com.royalreserve.bank

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.royalreserve.bank.data.AccountsStore
import com.royalreserve.bank.data.BankRepository
import com.royalreserve.bank.data.SettingsRepository
import com.royalreserve.bank.ui.BankApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = SettingsRepository(applicationContext)
        val repo = BankRepository(settings)
        val store = AccountsStore(repo::api)
        setContent { BankApp(settings, repo, store) }
    }
}
