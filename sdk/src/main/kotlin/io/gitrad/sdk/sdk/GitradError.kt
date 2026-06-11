package io.gitrad.sdk.sdk

import io.gitrad.sdk.domain.entities.TranslationFetchError

sealed class GitradError : Exception() {
    object Unauthorized : GitradError()
    object SubscriptionInactive : GitradError()
    data class RateLimited(val retryAfter: Long) : GitradError()
    data class NetworkError(override val cause: Throwable) : GitradError()
    data class ParseError(override val cause: Throwable) : GitradError()
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
