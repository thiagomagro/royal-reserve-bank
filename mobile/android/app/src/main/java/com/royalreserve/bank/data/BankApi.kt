package com.royalreserve.bank.data

class ApiError(val status: Int, message: String) : Exception(message)

interface BankApi {
    suspend fun accounts(): List<Account>

    suspend fun create(account: NewAccount): String

    suspend fun delete(holder: String): String

    suspend fun deposit(id: String, amount: Double): Account

    suspend fun transaction(items: List<TransactionItem>): String

    suspend fun assets(codes: List<String>): List<AssetAvailability>
}
