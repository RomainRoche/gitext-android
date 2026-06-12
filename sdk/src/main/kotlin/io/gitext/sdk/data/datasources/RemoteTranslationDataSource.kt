package io.gitext.sdk.data.datasources

import io.gitext.sdk.data.dtos.TranslationPayloadDto
import io.gitext.sdk.domain.entities.TranslationFetchError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

internal class RemoteTranslationDataSource(
    private val apiKey: String,
    private val baseUrl: String,
) {
    private val backoffDelays = listOf(2_000L, 4_000L, 8_000L, 16_000L)

    suspend fun fetch(): TranslationPayloadDto {
        var lastError: Throwable = TranslationFetchError.Network(RuntimeException("Unknown error"))

        for (attempt in 0..4) {
            try {
                return withContext(Dispatchers.IO) { performFetch() }
            } catch (e: TranslationFetchError.Unauthorized) {
                throw e
            } catch (e: TranslationFetchError.SubscriptionInactive) {
                throw e
            } catch (e: TranslationFetchError.Parse) {
                throw e
            } catch (e: TranslationFetchError.RateLimited) {
                lastError = e
                delay(e.retryAfter * 1_000L)
                continue
            } catch (e: Throwable) {
                lastError = e
            }
            if (attempt < 4) delay(backoffDelays[attempt])
        }
        throw lastError
    }

    private fun performFetch(): TranslationPayloadDto {
        val base = baseUrl.trimEnd('/')
        val connection = URL("$base/api/ota/download").openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("x-api-key", apiKey)
        connection.instanceFollowRedirects = true
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000

        try {
            val status = connection.responseCode
            return when (status) {
                in 200..299 -> {
                    val body = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                    try {
                        TranslationPayloadDto.fromJson(body)
                    } catch (e: Exception) {
                        throw TranslationFetchError.Parse(e)
                    }
                }
                401 -> throw TranslationFetchError.Unauthorized
                403 -> throw TranslationFetchError.SubscriptionInactive
                429 -> {
                    val retryAfter = connection.getHeaderField("Retry-After")?.toLongOrNull() ?: 60L
                    throw TranslationFetchError.RateLimited(retryAfter)
                }
                else -> throw TranslationFetchError.Network(RuntimeException("HTTP $status"))
            }
        } finally {
            connection.disconnect()
        }
    }
}
