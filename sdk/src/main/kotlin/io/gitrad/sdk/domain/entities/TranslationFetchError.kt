package io.gitrad.sdk.domain.entities

internal sealed class TranslationFetchError : Exception() {
    object Unauthorized : TranslationFetchError()
    object SubscriptionInactive : TranslationFetchError()
    data class RateLimited(val retryAfter: Long) : TranslationFetchError()
    data class Network(override val cause: Throwable) : TranslationFetchError()
    data class Parse(override val cause: Throwable) : TranslationFetchError()
}
