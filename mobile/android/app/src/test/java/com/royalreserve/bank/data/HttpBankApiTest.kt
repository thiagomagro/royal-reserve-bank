package com.royalreserve.bank.data

import java.io.IOException
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class HttpBankApiTest {
    private lateinit var server: MockWebServer
    private val baseUrl
        get() = server.url("").toString().trimEnd('/')

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun nonemptyTokenAddsBearerHeaderWithoutAcceptOrGetBody() = runTest {
        server.enqueue(MockResponse().setBody("[]"))
        HttpBankApi(baseUrl, "jwt").accounts()
        val request = server.takeRequest()
        assertEquals("Bearer jwt", request.getHeader("Authorization"))
        assertNull(request.getHeader("Accept"))
        assertNull(request.getHeader("Content-Type"))
        assertEquals(0L, request.bodySize)
        assertEquals("/api/account", request.path)
    }

    @Test
    fun nullTokenDoesNotAddAuthorizationHeader() = runTest {
        server.enqueue(MockResponse().setBody("[]"))
        HttpBankApi(baseUrl, null).accounts()
        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun emptyTokenDoesNotAddAuthorizationHeader() = runTest {
        server.enqueue(MockResponse().setBody("[]"))
        HttpBankApi(baseUrl, "").accounts()
        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun createSendsJsonNumericBalanceAndParsesPlainText201() = runTest {
        val text = "Successfully set up a new bank account for Jane."
        server.enqueue(MockResponse().setResponseCode(201).setBody(text))
        assertEquals(text, HttpBankApi(baseUrl).create(NewAccount("Jane", 12.5, "USD")))
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertTrue(request.getHeader("Content-Type")!!.startsWith("application/json"))
        assertEquals(
            "{\"accountHolderName\":\"Jane\",\"balance\":12.5,\"currency\":\"USD\"}",
            request.body.readUtf8(),
        )
    }

    @Test
    fun deleteUsesJsonBodyAndReturnsBackendTextUnchanged() = runTest {
        val text = "Successfully deleted Jane's account."
        server.enqueue(MockResponse().setBody(text))
        assertEquals(text, HttpBankApi(baseUrl).delete("Jane"))
        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/api/account", request.path)
        assertTrue(request.getHeader("Content-Type")!!.startsWith("application/json"))
        assertEquals("{\"accountHolderName\":\"Jane\"}", request.body.readUtf8())
    }

    @Test
    fun depositEncodesIdAsOneSegmentAndSendsJsonAmount() = runTest {
        server.enqueue(
            MockResponse()
                .setBody(
                    "{\"id\":\"a/b c\",\"accountNumber\":\"1\",\"accountHolderName\":\"Jane\",\"balance\":2.5,\"currency\":\"USD\"}"
                )
        )
        val updated = HttpBankApi(baseUrl).deposit("a/b c", 2.5)
        assertEquals("a/b c", updated.id)
        val request = server.takeRequest()
        assertEquals("/api/account/a%2Fb%20c/deposit", request.path)
        assertEquals("POST", request.method)
        assertTrue(request.getHeader("Content-Type")!!.startsWith("application/json"))
        assertEquals("{\"amount\":2.5}", request.body.readUtf8())
    }

    @Test
    fun assetQueryRepeatsEncodedParametersInOriginalOrder() = runTest {
        server.enqueue(MockResponse().setBody("[]"))
        assertTrue(HttpBankApi(baseUrl).assets(listOf("A B", "a&b", "A B")).isEmpty())
        val request = server.takeRequest()
        assertEquals(
            "/api/asset-management?assetCode=A%20B&assetCode=a%26b&assetCode=A%20B",
            request.path,
        )
        assertEquals(
            listOf("A B", "a&b", "A B"),
            request.requestUrl!!.queryParameterValues("assetCode"),
        )
    }

    @Test
    fun availabilityAcceptsBothKeysAndDoesNotOverrideFalse() = runTest {
        server.enqueue(
            MockResponse()
                .setBody(
                    """
            [
              {"assetCode":"a","isAssetAvailable":false,"assetAvailable":true},
              {"assetCode":"b","assetAvailable":true},
              {"assetCode":"c"},
              {"assetCode":"d","isAssetAvailable":null,"assetAvailable":true},
              {"assetCode":"e","isAssetAvailable":true,"assetAvailable":false}
            ]
        """
                        .trimIndent()
                )
        )
        val results = HttpBankApi(baseUrl).assets(listOf("a", "b", "c", "d", "e"))
        assertEquals(listOf(false, true, false, true, true), results.map { it.isAssetAvailable })
    }

    @Test
    fun non2xxPreservesStatusAndEntireRawBody() = runTest {
        val raw = "{\"message\":\"denied\"}\nraw gateway detail"
        server.enqueue(MockResponse().setResponseCode(401).setBody(raw))
        try {
            HttpBankApi(baseUrl).accounts()
            fail("Expected ApiError")
        } catch (error: ApiError) {
            assertEquals(401, error.status)
            assertEquals(raw, error.message)
        }
        assertEquals(1, server.requestCount)
    }

    @Test
    fun emptyNon2xxBodyFallsBackToReasonPhrase() = runTest {
        server.enqueue(MockResponse().setStatus("HTTP/1.1 404 Custom reason").setBody(""))
        try {
            HttpBankApi(baseUrl).accounts()
            fail("Expected ApiError")
        } catch (error: ApiError) {
            assertEquals(404, error.status)
            assertEquals("Custom reason", error.message)
        }
    }

    @Test
    fun transactionSendsEnvelopeAndReturnsPlainText201() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(201).setBody("Transaction completed successfully!")
        )
        assertEquals(
            "Transaction completed successfully!",
            HttpBankApi(baseUrl).transaction(listOf(TransactionItem("AAPL", "Apple", 2))),
        )
        val request = server.takeRequest()
        assertEquals("/api/transaction", request.path)
        assertEquals("POST", request.method)
        assertTrue(request.getHeader("Content-Type")!!.startsWith("application/json"))
        assertEquals(
            "{\"transactionItemsDtoList\":[{\"assetCode\":\"AAPL\",\"assetName\":\"Apple\",\"value\":2}]}",
            request.body.readUtf8(),
        )
    }

    @Test
    fun transaction201OopsIsTransportSuccess() = runTest {
        val text = "Oops! Something went wrong, please try again later!"
        server.enqueue(MockResponse().setResponseCode(201).setBody(text))
        assertEquals(text, HttpBankApi(baseUrl).transaction(emptyList()))
    }

    @Test
    fun empty2xxTextIsTransportSuccess() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))
        assertEquals("", HttpBankApi(baseUrl).delete("Jane"))
    }

    @Test
    fun malformedJsonIsNotAnHttpApiError() = runTest {
        server.enqueue(MockResponse().setBody("not json"))
        try {
            HttpBankApi(baseUrl).accounts()
            fail("Expected decode error")
        } catch (error: Exception) {
            assertFalse(error is ApiError)
        }
    }

    @Test
    fun networkFailureIsNotAnHttpApiError() = runTest {
        val api = HttpBankApi(baseUrl)
        server.shutdown()
        try {
            api.accounts()
            fail("Expected network error")
        } catch (error: Exception) {
            assertTrue(error is IOException)
            assertFalse(error is ApiError)
        }
    }
}
