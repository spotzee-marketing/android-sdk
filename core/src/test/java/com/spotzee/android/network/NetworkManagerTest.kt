package com.spotzee.android.network

import com.spotzee.android.Alias
import com.spotzee.android.Config
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class NetworkManagerTest {
    private val user = Alias(
        anonymousId = "anonymous-test-id",
        externalId = "external-test-id",
    )

    @Test
    fun successfulUnitResponseReleasesConnectionForReuse() = runBlocking {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse(body = "unused response body"))
            server.enqueue(MockResponse(body = "second response body"))
            val manager = NetworkManager(Config(apiKey = "public-test-key"))

            val firstResult = manager.get<Unit>(
                path = server.url("/first").toString(),
                user = user,
                useBaseUri = false,
            )
            val secondResult = manager.get<Unit>(
                path = server.url("/second").toString(),
                user = user,
                useBaseUri = false,
            )

            assertTrue(firstResult.isSuccess)
            assertTrue(secondResult.isSuccess)
            val firstRequest = requireNotNull(server.takeRequest(1, TimeUnit.SECONDS))
            val secondRequest = requireNotNull(server.takeRequest(1, TimeUnit.SECONDS))
            assertEquals(firstRequest.connectionIndex, secondRequest.connectionIndex)
            assertEquals(1, secondRequest.exchangeIndex)
        }
    }

    @Test
    fun successfulParsedResponsesRemainDecodedAndReusable() = runBlocking {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse(body = """{"value":"first"}"""))
            server.enqueue(MockResponse(body = """{"value":"second"}"""))
            val manager = NetworkManager(Config(apiKey = "public-test-key"))

            val firstResult = manager.get<ResponsePayload>(
                path = server.url("/first").toString(),
                user = user,
                useBaseUri = false,
            )
            val secondResult = manager.get<ResponsePayload>(
                path = server.url("/second").toString(),
                user = user,
                useBaseUri = false,
            )

            assertEquals("first", firstResult.getOrThrow().value)
            assertEquals("second", secondResult.getOrThrow().value)
            val firstRequest = requireNotNull(server.takeRequest(1, TimeUnit.SECONDS))
            val secondRequest = requireNotNull(server.takeRequest(1, TimeUnit.SECONDS))
            assertEquals(firstRequest.connectionIndex, secondRequest.connectionIndex)
            assertEquals(1, secondRequest.exchangeIndex)
        }
    }

    private data class ResponsePayload(
        val value: String,
    )
}
