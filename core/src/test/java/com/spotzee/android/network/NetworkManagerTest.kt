package com.spotzee.android.network

import com.spotzee.android.Alias
import com.spotzee.android.Config
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import okhttp3.Headers.Companion.headersOf
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
            assertEquals("Bearer public-test-key", firstRequest.headers["Authorization"])
            assertEquals(user.anonymousId, firstRequest.headers["x-anonymous-id"])
            assertEquals(user.externalId, firstRequest.headers["x-external-id"])
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

    @Test
    fun clickTrackingDoesNotSendIdentityHeadersOrFollowRedirects() = runBlocking {
        MockWebServer().use { trackingServer ->
            MockWebServer().use { destinationServer ->
                trackingServer.start()
                destinationServer.start()
                trackingServer.enqueue(
                    MockResponse(
                        code = 303,
                        headers = headersOf(
                            "Location",
                            destinationServer.url("/destination").toString(),
                        ),
                    ),
                )
                val manager = NetworkManager(Config(apiKey = "public-test-key"))

                val result = manager.trackClick(
                    trackingServer.url("/c?r=https%3A%2F%2Fexample.com").toString(),
                )

                assertTrue(result.isSuccess)
                val request = requireNotNull(trackingServer.takeRequest(1, TimeUnit.SECONDS))
                assertNull(request.headers["Authorization"])
                assertNull(request.headers["x-anonymous-id"])
                assertNull(request.headers["x-external-id"])
                assertNull(destinationServer.takeRequest(250, TimeUnit.MILLISECONDS))
            }
        }
    }

    @Test
    fun clickTrackingAcceptsSuccessfulTrackingResponse() = runBlocking {
        MockWebServer().use { trackingServer ->
            trackingServer.start()
            trackingServer.enqueue(MockResponse(code = 204))
            val manager = NetworkManager(Config(apiKey = "public-test-key"))

            val result = manager.trackClick(
                trackingServer.url("/c?r=https%3A%2F%2Fexample.com").toString(),
            )

            assertTrue(result.isSuccess)
            val request = requireNotNull(trackingServer.takeRequest(1, TimeUnit.SECONDS))
            assertEquals("/c", request.url.encodedPath)
        }
    }

    private data class ResponsePayload(
        val value: String,
    )
}
