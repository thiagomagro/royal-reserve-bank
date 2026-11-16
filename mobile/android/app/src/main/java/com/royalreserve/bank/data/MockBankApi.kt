package com.royalreserve.bank.data

import kotlin.math.floor

class MockBankApi : BankApi {
    private val store = mutableListOf<Account>()
    private var nextId = 4
    private var nextNumber = 1004
    private val available = setOf("PETR4", "VALE3", "AAPL", "BOND-BR-2030")

    init {
        reset()
    }

    @Synchronized
    fun reset() {
        store.clear()
        store.addAll(
            listOf(
                Account("1", "1001", "Alice Johnson", 12500.5, "USD"),
                Account("2", "1002", "Bruno Souza", 8400.0, "BRL"),
                Account("3", "1003", "Carla Mendes", 3200.0, "EUR"),
            )
        )
        nextId = 4
        nextNumber = 1004
    }

    override suspend fun accounts() = synchronized(this) { store.toList() }

    override suspend fun create(account: NewAccount): String =
        synchronized(this) {
            store.add(
                Account(
                    (nextId++).toString(),
                    (nextNumber++).toString(),
                    account.accountHolderName,
                    account.balance,
                    account.currency,
                )
            )
            "Successfully set up a new bank account for ${account.accountHolderName}."
        }

    override suspend fun delete(holder: String): String =
        synchronized(this) {
            val index = store.indexOfFirst { it.accountHolderName == holder }
            if (index < 0) throw ApiError(404, "No bank account found for $holder.")
            store.removeAt(index)
            "Successfully deleted the bank account for $holder."
        }

    override suspend fun deposit(id: String, amount: Double): Account =
        synchronized(this) {
            if (amount.isNaN() || amount <= 0)
                throw ApiError(400, "The deposit amount must be greater than zero.")
            val index = store.indexOfFirst { it.id == id }
            if (index < 0) throw ApiError(404, "No bank account found with id $id.")
            val updated =
                store[index].copy(
                    balance = floor((store[index].balance + amount) * 100 + 0.5) / 100
                )
            store[index] = updated
            updated
        }

    override suspend fun assets(codes: List<String>) =
        codes.map { AssetAvailability(it, it in available) }

    override suspend fun transaction(items: List<TransactionItem>) =
        if (items.all { it.assetCode in available }) "Transaction placed successfully."
        else "Oops! Something went wrong, please try again later!"
}
