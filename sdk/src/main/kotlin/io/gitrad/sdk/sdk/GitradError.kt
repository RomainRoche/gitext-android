package io.gitrad.sdk.sdk

import io.gitrad.sdk.domain.entities.TranslationFetchError

/**
 * Errors reported by the Gitrad SDK via [GitradEvent.FetchFailed].
 *
 * Use a `when` expression to handle each case exhaustively:
 * ```kotlin
 * Gitrad.onEvent { event ->
 *     if (event is GitradEvent.FetchFailed) when (event.error) {
 *         is GitradError.Unauthorized       -> { /* check API key */ }
 *         is GitradError.SubscriptionInactive -> { /* check subscription */ }
 *         is GitradError.RateLimited        -> { /* back off */ }
 *         is GitradError.NetworkError       -> { /* transient; will retry */ }
 *         is GitradError.ParseError         -> { /* contact support */ }
 *         is GitradError.NotConfigured      -> { /* call Gitrad.configure() first */ }
 *     }
 * }
 * ```
 */
sealed class GitradError : Exception() {
    /** The API key was rejected (HTTP 401). Verify [GitradConfig.apiKey]. */
    object Unauthorized : GitradError()

    /** The Gitrad subscription is inactive or over quota (HTTP 403). */
    object SubscriptionInactive : GitradError()

    /**
     * The server responded with HTTP 429.
     * @param retryAfter Seconds to wait before retrying, from the `Retry-After` header.
     */
    data class RateLimited(val retryAfter: Long) : GitradError()

    /** A network or HTTP error occurred. The fetch will be retried with backoff. */
    data class NetworkError(override val cause: Throwable) : GitradError()

    /** The server response could not be parsed. */
    data class ParseError(override val cause: Throwable) : GitradError()

    /** [Gitrad.configure] has not been called yet. */
    object NotConfigured : GitradError()

    companion object {
        internal fun from(error: Throwable): GitradError = when (error) {
            is TranslationFetchError.Unauthorized -> Unauthorized
            is TranslationFetchError.SubscriptionInactive -> SubscriptionInactive
            is TranslationFetchError.RateLimited -> RateLimited(error.retryAfter)
            is TranslationFetchError.Network -> NetworkError(error.cause)
            is TranslationFetchError.Parse -> ParseError(error.cause)
            is GitradError -> error
            else -> NetworkError(error)
        }
    }
}
