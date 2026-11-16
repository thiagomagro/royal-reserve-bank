package com.royalreserve.bank

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test

class BankTest {
    @Test fun seedsCountersAndReset() = runBlocking {
        val api = MockBankApi()
        assertEquals(listOf("Alice Johnson", "Bruno Souza", "Carla Mendes"), api.accounts().map { it.accountHolderName })
        assertEquals(listOf(12500.5, 8400.0, 3200.0), api.accounts().map { it.balance })
        assertEquals("Successfully set up a new bank account for Jane.", api.create(NewAccount("Jane", 0.0, "GBP")))
        assertEquals("4", api.accounts().last().id); assertEquals("1004", api.accounts().last().accountNumber)
        api.delete("Jane"); api.create(NewAccount("Jane", 0.0, "USD")); assertEquals("5", api.accounts().last().id)
        api.reset(); assertEquals(3, api.accounts().size)
    }
    @Test fun deleteExactFirstAndEmpty() = runBlocking {
        val api = MockBankApi(); api.create(NewAccount("Alice Johnson", 1.0, "USD"))
        assertEquals("Successfully deleted the bank account for Alice Johnson.", api.delete("Alice Johnson"))
        assertEquals("4", api.accounts().last().id)
        try { api.delete("alice johnson"); fail() } catch (e: ApiError) { assertEquals(404, e.status); assertEquals("No bank account found for alice johnson.", e.message) }
        api.accounts().forEach { api.delete(it.accountHolderName) }; assertTrue(api.accounts().isEmpty())
    }
    @Test fun depositRoundingAndErrors() = runBlocking {
        val api = MockBankApi()
        assertEquals(12500.51, api.deposit("1", 0.005).balance, 0.00001)
        for (amount in listOf(0.0, -1.0, Double.NaN)) {
            try { api.deposit("missing", amount); fail() } catch (e: ApiError) { assertEquals(400, e.status); assertEquals("The deposit amount must be greater than zero.", e.message) }
        }
        try { api.deposit("missing", 1.0); fail() } catch (e: ApiError) { assertEquals(404, e.status); assertEquals("No bank account found with id missing.", e.message) }
    }
    @Test fun assetsAndTransaction() = runBlocking {
        val api = MockBankApi()
        assertEquals(listOf(true, false, true, false), api.assets(listOf("AAPL", "aapl", "AAPL", "unknown")).map { it.isAssetAvailable })
        assertTrue(api.assets(emptyList()).isEmpty())
        assertEquals("Transaction placed successfully.", api.transaction(emptyList()))
        assertEquals("Transaction placed successfully.", api.transaction(listOf(TransactionItem("BOND-BR-2030", "", 1))))
        assertEquals("Oops! Something went wrong, please try again later!", api.transaction(listOf(TransactionItem("bad", "", 1))))
        assertTrue(transactionIsError("Oops!")); assertFalse(transactionIsError("oops"))
    }
    @Test fun validationParsingAndFormatting() {
        assertTrue(validAccount(" Jane ", "0")); assertFalse(validAccount(" ", "0")); assertFalse(validAccount("Jane", "")); assertFalse(validAccount("Jane", "-1"))
        assertTrue(validDeposit("0.01")); assertFalse(validDeposit("0")); assertFalse(validDeposit("NaN"))
        assertEquals(listOf("AAPL", "aapl", "AAPL"), parseCodes(" AAPL, aapl\nAAPL "))
        assertTrue(parseCodes(" , ").isEmpty())
        assertNull(parseItems(emptyList())); assertNull(parseItems(listOf(ItemRow("AAPL", value = "0"))))
        assertEquals(TransactionItem("AAPL", "Apple", 2), parseItems(listOf(ItemRow(" AAPL ", " Apple ", "2.9")))!!.single())
        assertEquals("$1,250.50", formatCurrency(1250.5, "USD")); assertEquals("INVALID 1.50", formatCurrency(1.5, "INVALID"))
    }
    @Test fun httpHeadersBodiesAndText(): Unit = runBlocking {
        val server = MockWebServer(); server.start()
        try {
            val api = HttpBankApi(server.url("").toString().trimEnd('/'), "token")
            server.enqueue(MockResponse().setResponseCode(201).setBody("created"))
            assertEquals("created", api.create(NewAccount("Jane", 1.0, "USD")))
            val req = server.takeRequest(); assertEquals("Bearer token", req.getHeader("Authorization")); assertTrue(req.getHeader("Content-Type")!!.startsWith("application/json")); assertNull(req.getHeader("Accept")); assertTrue(req.body.readUtf8().contains("\"balance\":1.0"))
            server.enqueue(MockResponse().setBody("deleted")); assertEquals("deleted", api.delete("Jane")); val deletion = server.takeRequest(); assertEquals("DELETE", deletion.method); assertEquals("{\"accountHolderName\":\"Jane\"}", deletion.body.readUtf8())
            server.enqueue(MockResponse().setResponseCode(201).setBody("Oops!")); assertEquals("Oops!", api.transaction(emptyList())); server.takeRequest()
            server.enqueue(MockResponse().setResponseCode(400).setBody("{\"message\":\"raw\"}"))
            try { api.accounts(); fail() } catch (e: ApiError) { assertEquals(400, e.status); assertEquals("{\"message\":\"raw\"}", e.message) }; server.takeRequest()
            server.enqueue(MockResponse().setStatus("HTTP/1.1 404 Not Found").setBody(""))
            try { api.accounts(); fail() } catch (e: ApiError) { assertEquals("Not Found", e.message) }; server.takeRequest()
        } finally { server.shutdown() }
    }
    @Test fun httpEncodingNormalizationAndNoToken(): Unit = runBlocking {
        val server = MockWebServer(); server.start()
        try {
            val api = HttpBankApi(server.url("").toString().trimEnd('/'), "")
            server.enqueue(MockResponse().setBody("[{\"assetCode\":\"a\",\"isAssetAvailable\":false,\"assetAvailable\":true},{\"assetCode\":\"b\",\"assetAvailable\":true},{\"assetCode\":\"c\"},{\"assetCode\":\"d\",\"isAssetAvailable\":null,\"assetAvailable\":true}]"))
            assertEquals(listOf(false, true, false, true), api.assets(listOf("A B", "a&b", "A B")).map { it.isAssetAvailable })
            val req = server.takeRequest(); assertNull(req.getHeader("Authorization")); assertEquals(listOf("A B", "a&b", "A B"), req.requestUrl!!.queryParameterValues("assetCode"))
            server.enqueue(MockResponse().setBody("{\"id\":\"a/b ?\",\"accountNumber\":\"1\",\"accountHolderName\":\"A\",\"balance\":2,\"currency\":\"USD\"}"))
            api.deposit("a/b ?", 1.0); assertEquals("/api/account/a%2Fb%20%3F/deposit", server.takeRequest().path)
            server.enqueue(MockResponse().setBody("[]")); assertTrue(api.accounts().isEmpty()); server.takeRequest()
            server.enqueue(MockResponse().setBody("not json")); try { api.accounts(); fail() } catch (e: Exception) { assertFalse(e is ApiError) }; server.takeRequest()
        } finally { server.shutdown() }
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun viewModelRefreshesAndValidates() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val api = MockBankApi(); val store = AccountsStore { api }; val vm = AccountsViewModel({ api }, store)
            advanceUntilIdle(); assertEquals(3, store.state.value.accounts.size)
            vm.create(); assertTrue(vm.banner!!.error)
            vm.holder = " Jane "; vm.create(); advanceUntilIdle(); assertEquals(4, store.state.value.accounts.size); assertFalse(vm.creating)
            vm.depositTarget = api.accounts().last(); vm.amount = "2.50"; vm.deposit(); advanceUntilIdle(); assertEquals(2.5, store.state.value.accounts.last().balance, 0.0)
            vm.deleteTarget = api.accounts().last(); vm.delete(); advanceUntilIdle(); assertEquals(3, store.state.value.accounts.size)
            val tx = TransactionViewModel { api }; tx.rows = listOf(ItemRow("bad")); tx.submit(); advanceUntilIdle(); assertTrue(tx.banner!!.error)
        } finally { Dispatchers.resetMain() }
    }
}
