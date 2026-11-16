package com.royalreserve.bank

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.HttpUrl.Companion.toHttpUrl
import kotlin.math.floor

@Serializable data class Account(val id: String, val accountNumber: String, val accountHolderName: String, val balance: Double, val currency: String)
@Serializable data class NewAccount(val accountHolderName: String, val balance: Double, val currency: String)
@Serializable data class TransactionItem(val assetCode: String, val assetName: String, val value: Int)
data class AssetAvailability(val assetCode: String, val isAssetAvailable: Boolean)
class ApiError(val status: Int, message: String) : Exception(message)

interface BankApi {
    suspend fun accounts(): List<Account>
    suspend fun create(account: NewAccount): String
    suspend fun delete(holder: String): String
    suspend fun deposit(id: String, amount: Double): Account
    suspend fun transaction(items: List<TransactionItem>): String
    suspend fun assets(codes: List<String>): List<AssetAvailability>
}

class HttpBankApi(private val baseUrl: String, private val token: String? = null, private val client: OkHttpClient = OkHttpClient()) : BankApi {
    private val json = Json { ignoreUnknownKeys = true }
    private suspend fun request(path: String, method: String = "GET", body: String? = null): String = withContext(Dispatchers.IO) {
        val builder = Request.Builder().url(baseUrl + path)
        if (!token.isNullOrEmpty()) builder.header("Authorization", "Bearer $token")
        builder.method(method, body?.toRequestBody("application/json".toMediaType()))
        client.newCall(builder.build()).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw ApiError(response.code, text.ifEmpty { response.message })
            text
        }
    }
    override suspend fun accounts(): List<Account> = json.decodeFromString(request("/api/account"))
    override suspend fun create(account: NewAccount) = request("/api/account", "POST", json.encodeToString(account))
    override suspend fun delete(holder: String) = request("/api/account", "DELETE", buildJsonObject { put("accountHolderName", holder) }.toString())
    override suspend fun deposit(id: String, amount: Double): Account {
        val path = "http://localhost/api/account/".toHttpUrl().newBuilder().addPathSegment(id).addPathSegment("deposit").build().encodedPath
        return json.decodeFromString(request(path, "POST", buildJsonObject { put("amount", amount) }.toString()))
    }
    override suspend fun transaction(items: List<TransactionItem>) = request("/api/transaction", "POST", buildJsonObject { put("transactionItemsDtoList", json.encodeToJsonElement(items)) }.toString())
    override suspend fun assets(codes: List<String>): List<AssetAvailability> {
        val url = (baseUrl + "/api/asset-management").toHttpUrl().newBuilder()
        codes.forEach { url.addQueryParameter("assetCode", it) }
        return json.parseToJsonElement(request(url.build().toString().removePrefix(baseUrl))).jsonArray.map {
            val obj = it.jsonObject
            AssetAvailability(obj.getValue("assetCode").jsonPrimitive.content,
                obj["isAssetAvailable"]?.jsonPrimitive?.booleanOrNull ?: obj["assetAvailable"]?.jsonPrimitive?.booleanOrNull ?: false)
        }
    }
}

class MockBankApi : BankApi {
    private val store = mutableListOf<Account>()
    private var nextId = 4
    private var nextNumber = 1004
    private val available = setOf("PETR4", "VALE3", "AAPL", "BOND-BR-2030")
    init { reset() }
    @Synchronized fun reset() {
        store.clear()
        store.addAll(listOf(Account("1", "1001", "Alice Johnson", 12500.5, "USD"), Account("2", "1002", "Bruno Souza", 8400.0, "BRL"), Account("3", "1003", "Carla Mendes", 3200.0, "EUR")))
        nextId = 4; nextNumber = 1004
    }
    override suspend fun accounts() = synchronized(this) { store.toList() }
    override suspend fun create(account: NewAccount): String = synchronized(this) {
        store.add(Account((nextId++).toString(), (nextNumber++).toString(), account.accountHolderName, account.balance, account.currency))
        "Successfully set up a new bank account for ${account.accountHolderName}."
    }
    override suspend fun delete(holder: String): String = synchronized(this) {
        val index = store.indexOfFirst { it.accountHolderName == holder }
        if (index < 0) throw ApiError(404, "No bank account found for $holder.")
        store.removeAt(index)
        "Successfully deleted the bank account for $holder."
    }
    override suspend fun deposit(id: String, amount: Double): Account = synchronized(this) {
        if (amount.isNaN() || amount <= 0) throw ApiError(400, "The deposit amount must be greater than zero.")
        val index = store.indexOfFirst { it.id == id }
        if (index < 0) throw ApiError(404, "No bank account found with id $id.")
        val updated = store[index].copy(balance = floor((store[index].balance + amount) * 100 + 0.5) / 100)
        store[index] = updated
        updated
    }
    override suspend fun assets(codes: List<String>) = codes.map { AssetAvailability(it, it in available) }
    override suspend fun transaction(items: List<TransactionItem>) = if (items.all { it.assetCode in available }) "Transaction placed successfully." else "Oops! Something went wrong, please try again later!"
}
