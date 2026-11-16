package com.royalreserve.bank.data

import kotlinx.serialization.Serializable

@Serializable
data class Account(
    val id: String,
    val accountNumber: String,
    val accountHolderName: String,
    val balance: Double,
    val currency: String,
)

@Serializable
data class NewAccount(val accountHolderName: String, val balance: Double, val currency: String)

@Serializable
data class TransactionItem(val assetCode: String, val assetName: String, val value: Int)

data class AssetAvailability(val assetCode: String, val isAssetAvailable: Boolean)

data class ItemRow(
    val code: String = "",
    val name: String = "",
    val value: String = "1",
    val availability: Boolean? = null,
)
