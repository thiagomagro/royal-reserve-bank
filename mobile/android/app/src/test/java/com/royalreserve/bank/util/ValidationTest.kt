package com.royalreserve.bank.util

import com.royalreserve.bank.data.ItemRow
import com.royalreserve.bank.data.TransactionItem
import org.junit.Assert.*
import org.junit.Test

class ValidationTest {
    @Test
    fun assetCodesSplitOnCommasAndWhitespaceWithoutChangingOrderOrCase() {
        assertEquals(
            listOf("AAPL", "aapl", "AAPL", "PETR4"),
            parseCodes(" AAPL,, aapl\nAAPL\tPETR4 "),
        )
    }

    @Test
    fun blankAssetInputReturnsNoCodes() {
        assertTrue(parseCodes(" ,\n\t ").isEmpty())
        assertTrue(parseCodes("").isEmpty())
    }

    @Test
    fun accountAcceptsTrimmedHolderAndNonnegativeBalance() {
        assertTrue(validAccount(" Jane ", "0"))
        assertNull(accountValidationMessage("Jane", "12.50"))
    }

    @Test
    fun invalidAccountHasExactValidationMessage() {
        for ((holder, balance) in
            listOf(
                " " to "0",
                "Jane" to "",
                "Jane" to "bad",
                "Jane" to "-1",
                "Jane" to "NaN",
                "Jane" to "Infinity",
            )) {
            assertEquals(
                "Provide a holder name and a balance of 0 or more.",
                accountValidationMessage(holder, balance),
            )
        }
    }

    @Test
    fun positiveDepositHasNoValidationError() {
        assertTrue(validDeposit("0.01"))
        assertNull(depositValidationMessage("25.50"))
    }

    @Test
    fun invalidDepositHasExactValidationMessage() {
        for (amount in listOf("", "bad", "0", "-1", "NaN", "Infinity")) {
            assertEquals("Provide an amount greater than 0.", depositValidationMessage(amount))
        }
    }

    @Test
    fun transactionTrimsFieldsFiltersBlankCodesAndParsesIntegerPrefix() {
        val rows =
            listOf(
                ItemRow(" ", value = "bad"),
                ItemRow(" AAPL ", " Apple ", "2.9"),
                ItemRow("PETR4", value = "  +3units"),
            )
        assertEquals(
            listOf(TransactionItem("AAPL", "Apple", 2), TransactionItem("PETR4", "", 3)),
            parseItems(rows),
        )
        assertNull(transactionValidationMessage(rows))
    }

    @Test
    fun emptyTransactionHasExactValidationMessage() {
        for (rows in listOf(emptyList(), listOf(ItemRow()))) {
            assertNull(parseItems(rows))
            assertEquals(
                "Each item needs an asset code and a positive integer value.",
                transactionValidationMessage(rows),
            )
        }
    }

    @Test
    fun invalidTransactionValuesHaveExactValidationMessage() {
        for (value in listOf("", "bad", "0", "-1", "NaN")) {
            assertEquals(
                "Each item needs an asset code and a positive integer value.",
                transactionValidationMessage(listOf(ItemRow("AAPL", value = value))),
            )
        }
    }

    @Test
    fun transactionOopsDetectionIsCaseSensitive() {
        assertTrue(transactionIsError("Oops! Something went wrong, please try again later!"))
        assertFalse(transactionIsError("oops"))
        assertFalse(transactionIsError("Transaction completed successfully!"))
    }
}
