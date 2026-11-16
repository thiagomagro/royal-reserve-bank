package com.royalreserve.bank.util

import com.royalreserve.bank.data.ItemRow
import com.royalreserve.bank.data.TransactionItem

fun parseCodes(input: String) = input.split(Regex("[\\s,]+")).filter { it.isNotBlank() }

fun validAccount(holder: String, balance: String) =
    holder.trim().isNotEmpty() && balance.toDoubleOrNull()?.let { it.isFinite() && it >= 0 } == true

fun validDeposit(amount: String) = amount.toDoubleOrNull()?.let { it.isFinite() && it > 0 } == true

fun accountValidationMessage(holder: String, balance: String): String? =
    if (validAccount(holder, balance)) null else "Provide a holder name and a balance of 0 or more."

fun depositValidationMessage(amount: String): String? =
    if (validDeposit(amount)) null else "Provide an amount greater than 0."

fun transactionValidationMessage(rows: List<ItemRow>): String? =
    if (parseItems(rows) != null) null
    else "Each item needs an asset code and a positive integer value."

fun parseItems(rows: List<ItemRow>): List<TransactionItem>? {
    val items =
        rows
            .filter { it.code.trim().isNotEmpty() }
            .map {
                val value =
                    Regex("^[+-]?\\d+").find(it.value.trimStart())?.value?.toIntOrNull()
                        ?: return null
                if (value <= 0) return null
                TransactionItem(it.code.trim(), it.name.trim(), value)
            }
    return items.takeIf { it.isNotEmpty() }
}

fun transactionIsError(message: String) = message.contains("Oops")
