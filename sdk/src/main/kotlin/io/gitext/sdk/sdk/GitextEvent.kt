package io.gitext.sdk.sdk

/**
 * Lifecycle events emitted by the Gitext SDK. Register a handler with
 * [Gitext.onEvent] to observe them.
 */
sealed class GitextEvent {
    /** A remote fetch has started. */
    object FetchStarted : GitextEvent()

    /**
     * A remote fetch completed successfully.
     * @param languages Number of language entries in the payload.
     * @param ms        Round-trip duration in milliseconds.
     */
    data class FetchSucceeded(val languages: Int, val ms: Long) : GitextEvent()

    /**
     * A remote fetch failed. Transient errors (network, rate-limit) are
     * retried automatically; permanent errors (auth, subscription) are not.
     * @param error The reason for the failure.
     */
    data class FetchFailed(val error: GitextError) : GitextEvent()

    /** Translations were loaded from the on-disk cache during [Gitext.configure]. */
    object CacheHit : GitextEvent()

    /** No cache was available; translations were loaded from the bundled baseline asset. */
    object BundleFallback : GitextEvent()
}
