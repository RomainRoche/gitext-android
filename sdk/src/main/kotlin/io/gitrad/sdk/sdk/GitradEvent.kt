package io.gitrad.sdk.sdk

/**
 * Lifecycle events emitted by the Gitrad SDK. Register a handler with
 * [Gitrad.onEvent] to observe them.
 */
sealed class GitradEvent {
    /** A remote fetch has started. */
    object FetchStarted : GitradEvent()

    /**
     * A remote fetch completed successfully.
     * @param languages Number of language entries in the payload.
     * @param ms        Round-trip duration in milliseconds.
     */
    data class FetchSucceeded(val languages: Int, val ms: Long) : GitradEvent()

    /**
     * A remote fetch failed. Transient errors (network, rate-limit) are
     * retried automatically; permanent errors (auth, subscription) are not.
     * @param error The reason for the failure.
     */
    data class FetchFailed(val error: GitradError) : GitradEvent()

    /** Translations were loaded from the on-disk cache during [Gitrad.configure]. */
    object CacheHit : GitradEvent()

    /** No cache was available; translations were loaded from the bundled baseline asset. */
    object BundleFallback : GitradEvent()
}
