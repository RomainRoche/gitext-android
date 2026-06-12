package io.gitext.sdk.data

import io.gitext.sdk.data.datasources.RemoteTranslationDataSource
import io.gitext.sdk.domain.entities.TranslationFetchError
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RemoteTranslationDataSourceTest {

    private val server = MockWebServer()

    @Before fun setUp() = server.start()
    @After fun tearDown() = server.shutdown()

    private fun source() = RemoteTranslationDataSource("test-key", server.url("/").toString())

    @Test fun sends_api_key_header() = runTest {
        server.enqueue(successResponse())
        source().fetch()
        assertEquals("test-key", server.takeRequest().getHeader("x-api-key"))
    }

    @Test fun parses_valid_200_response() = runTest {
        server.enqueue(successResponse())
        val dto = source().fetch()
        assertTrue(dto.translations.containsKey("en"))
        assertEquals(2, dto.translations["en"]?.size)
    }

    @Test fun throws_unauthorized_on_401() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))
        try {
            source().fetch()
            error("expected Unauthorized")
        } catch (_: TranslationFetchError.Unauthorized) {}
        assertEquals(1, server.requestCount)
    }

    @Test fun throws_subscription_inactive_on_403() = runTest {
        server.enqueue(MockResponse().setResponseCode(403))
        try {
            source().fetch()
            error("expected SubscriptionInactive")
        } catch (_: TranslationFetchError.SubscriptionInactive) {}
        assertEquals(1, server.requestCount)
    }

    @Test fun throws_parse_error_on_malformed_json() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("not json {{"))
        try {
            source().fetch()
            error("expected ParseError")
        } catch (_: TranslationFetchError.Parse) {}
        assertEquals(1, server.requestCount)
    }

    @Test fun retries_on_500_and_succeeds() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        server.enqueue(successResponse())
        val dto = source().fetch()
        assertEquals(2, server.requestCount)
        assertTrue(dto.translations.containsKey("en"))
    }

    @Test fun exhausts_retries_and_throws_network_error_on_persistent_500() = runTest {
        repeat(5) { server.enqueue(MockResponse().setResponseCode(500)) }
        try {
            source().fetch()
            error("expected NetworkError")
        } catch (_: TranslationFetchError.Network) {}
        assertEquals(5, server.requestCount)
    }

    @Test fun retries_on_429_and_succeeds() = runTest {
        server.enqueue(MockResponse().setResponseCode(429).addHeader("Retry-After", "1"))
        server.enqueue(successResponse())
        source().fetch()
        assertEquals(2, server.requestCount)
    }

    @Test fun reads_retry_after_header_from_429() = runTest {
        repeat(5) { server.enqueue(MockResponse().setResponseCode(429).addHeader("Retry-After", "42")) }
        try {
            source().fetch()
            error("expected RateLimited")
        } catch (e: TranslationFetchError.RateLimited) {
            assertEquals(42L, e.retryAfter)
        }
    }

    private fun successResponse() = MockResponse()
        .setResponseCode(200)
        .setBody("""{"en":{"greeting.hello":"Hello","items.count":{"one":"%d item","other":"%d items"}}}""")
}
