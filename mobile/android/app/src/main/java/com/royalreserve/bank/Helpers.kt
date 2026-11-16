package com.royalreserve.bank

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

fun formatCurrency(value: Double, code: String): String = try {
    NumberFormat.getCurrencyInstance(Locale.US).apply {
        currency = Currency.getInstance(code)
        minimumFractionDigits = Currency.getInstance(code).defaultFractionDigits.coerceAtLeast(0)
        maximumFractionDigits = minimumFractionDigits
    }.format(value)
} catch (_: IllegalArgumentException) { "$code ${String.format(Locale.US, "%.2f", value)}" }
fun parseCodes(input: String) = input.split(Regex("[\\s,]+")).filter { it.isNotBlank() }
fun validAccount(holder: String, balance: String) = holder.trim().isNotEmpty() && balance.toDoubleOrNull()?.let { it.isFinite() && it >= 0 } == true
fun validDeposit(amount: String) = amount.toDoubleOrNull()?.let { it.isFinite() && it > 0 } == true
data class ItemRow(val code: String = "", val name: String = "", val value: String = "1", val availability: Boolean? = null)
fun parseItems(rows: List<ItemRow>): List<TransactionItem>? {
    val items = rows.filter { it.code.trim().isNotEmpty() }.map {
        val value = Regex("^[+-]?\\d+").find(it.value.trimStart())?.value?.toIntOrNull() ?: return null
        if (value <= 0) return null
        TransactionItem(it.code.trim(), it.name.trim(), value)
    }
    return items.takeIf { it.isNotEmpty() }
}
fun transactionIsError(message: String) = message.contains("Oops")
