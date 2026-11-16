package com.royalreserve.bank.util

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test
    fun usdUsesUsCurrencyGroupingAndTwoDecimals() {
        assertEquals("$12,500.50", formatCurrency(12500.5, "USD"))
        assertEquals("$0.00", formatCurrency(0.0, "USD"))
    }

    @Test
    fun supportedCurrenciesHaveUsDisplaySymbols() {
        assertEquals("€1.50", formatCurrency(1.5, "EUR"))
        assertEquals("£1.50", formatCurrency(1.5, "GBP"))
        assertEquals("R$1.50", formatCurrency(1.5, "BRL"))
    }

    @Test
    fun currencyUsesItsOwnFractionDigits() {
        assertEquals("¥1,250", formatCurrency(1250.0, "JPY"))
    }

    @Test
    fun invalidCurrencyFallsBackToCodeAndTwoDecimals() {
        assertEquals("INVALID 12.50", formatCurrency(12.5, "INVALID"))
        assertEquals(" 0.00", formatCurrency(0.0, ""))
    }

    @Test
    fun formattingDoesNotDependOnDeviceLocale() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals("$1,234.50", formatCurrency(1234.5, "USD"))
            assertEquals("INVALID 1234.50", formatCurrency(1234.5, "INVALID"))
        } finally {
            Locale.setDefault(previous)
        }
    }
}
