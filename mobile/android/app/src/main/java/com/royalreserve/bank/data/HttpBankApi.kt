package com.royalreserve.bank.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class HttpBankApi(
    private val baseUrl: String,
    private val token: String? = null,
    private val client: OkHttpClient = OkHttpClient(),
) : BankApi {
    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun request(
        path: String,
        method: String = "GET",
        body: String? = null,
    ): String =
        withContext(Dispatchers.IO) {
            val builder = Request.Builder().url(baseUrl + path)
            if (!token.isNullOrEmpty()) builder.header("Authorization", "Bearer $token")
            builder.method(method, body?.toRequestBody("application/json".toMediaType()))
            client.newCall(builder.build()).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful)
                    throw ApiError(response.code, text.ifEmpty { response.message })
                text
            }
        }

    override suspend fun accounts(): List<Account> = json.decodeFromString(request("/api/account"))

    override suspend fun create(account: NewAccount) =
        request("/api/account", "POST", json.encodeToString(account))

    override suspend fun delete(holder: String) =
        request(
            "/api/account",
            "DELETE",
            buildJsonObject { put("accountHolderName", holder) }.toString(),
        )

    override suspend fun deposit(id: String, amount: Double): Account {
        val path =
            "http://localhost/api/account/"
                .toHttpUrl()
                .newBuilder()
                .addPathSegment(id)
                .addPathSegment("deposit")
                .build()
                .encodedPath
        return json.decodeFromString(
            request(path, "POST", buildJsonObject { put("amount", amount) }.toString())
        )
    }

    override suspend fun transaction(items: List<TransactionItem>) =
        request(
            "/api/transaction",
            "POST",
            buildJsonObject { put("transactionItemsDtoList", json.encodeToJsonElement(items)) }
                .toString(),
        )

    override suspend fun assets(codes: List<String>): List<AssetAvailability> {
        val url = (baseUrl + "/api/asset-management").toHttpUrl().newBuilder()
        codes.forEach { url.addQueryParameter("assetCode", it) }
        return json
            .parseToJsonElement(request(url.build().toString().removePrefix(baseUrl)))
            .jsonArray
            .map {
                val obj = it.jsonObject
                AssetAvailability(
                    obj.getValue("assetCode").jsonPrimitive.content,
                    obj["isAssetAvailable"]?.jsonPrimitive?.booleanOrNull
                        ?: obj["assetAvailable"]?.jsonPrimitive?.booleanOrNull
                        ?: false,
                )
            }
    }
}
