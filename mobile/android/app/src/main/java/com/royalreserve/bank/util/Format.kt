package com.royalreserve.bank.util

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

fun formatCurrency(value: Double, code: String): String =
    try {
        NumberFormat.getCurrencyInstance(Locale.US)
            .apply {
                currency = Currency.getInstance(code)
                minimumFractionDigits =
                    Currency.getInstance(code).defaultFractionDigits.coerceAtLeast(0)
                maximumFractionDigits = minimumFractionDigits
            }
            .format(value)
    } catch (_: IllegalArgumentException) {
        "$code ${String.format(Locale.US, "%.2f", value)}"
    }
