package com.royalreserve.bank.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.royalreserve.bank.data.ApiError
import com.royalreserve.bank.ui.components.Banner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

open class ScreenViewModel : ViewModel() {
    var banner by mutableStateOf<Banner?>(null)
    var pending by mutableStateOf(false)

    fun notify(text: String, error: Boolean = false) {
        banner = Banner(text, error)
    }

    fun execute(fallback: String, block: suspend () -> Unit) {
        if (pending) return
        viewModelScope.launch {
            pending = true
            try {
                block()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                notify(if (e is ApiError) e.message.orEmpty() else fallback, true)
            } finally {
                pending = false
            }
        }
    }
}
