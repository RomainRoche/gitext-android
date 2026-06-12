package io.gitext.sdk.sdk

import io.gitext.sdk.domain.entities.TranslationFetchError

/**
 * Errors reported by the Gitext SDK via [GitextEvent.FetchFailed].
 *
 * Use a `when` expression to handle each case exhaustively:
 * ```kotlin
 * Gitext.onEvent { event ->
 *     if (event is GitextEvent.FetchFailed) when (event.error) {
 *         is GitextError.Unauthorized       -> { /* check API key */ }
 *         is GitextError.SubscriptionInactive -> { /* check subscription */ }
 *         is GitextError.RateLimited        -> { /* back off */ }
 *         is GitextError.NetworkError       -> { /* transient; will retry */ }
 *         is GitextError.ParseError         -> { /* contact support */ }
 *         is GitextError.NotConfigured      -> { /* call Gitext.configure() first */ }
 *     }
 * }
 * ```
 */
sealed class GitextError : Exception() {
    /** The API key was rejected (HTTP 401). Verify [GitextConfig.apiKey]. */
    object Unauthorized : GitextError()

    /** The Gitext subscription is inactive or over quota (HTTP 403). */
    object SubscriptionInactive : GitextError()

    /**
     * The server responded with HTTP 429.
     * @param retryAfter Seconds to wait before retrying, from the `Retry-After` header.
     */
    data class RateLimited(val retryAfter: Long) : GitextError()

    /** A network or HTTP error occurred. The fetch will be retried with backoff. */
    data class NetworkError(override val cause: Throwable) : GitextError()

    /** The server response could not be parsed. */
    data class ParseError(override val cause: Throwable) : GitextError()

    /** [Gitext.configure] has not been called yet. */
    object NotConfigured : GitextError()

    companion object {
        internal fun from(error: Throwable): GitextError = when (error) {
            is TranslationFetchError.Unauthorized -> Unauthorized
            is TranslationFetchError.SubscriptionInactive -> SubscriptionInactive
            is TranslationFetchError.RateLimited -> RateLimited(error.retryAfter)
            is TranslationFetchError.Network -> NetworkError(error.cause)
            is TranslationFetchError.Parse -> ParseError(error.cause)
            is GitextError -> error
            else -> NetworkError(error)
        }
    }
}
