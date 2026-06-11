package io.gitrad.sdk.sdk

sealed class GitradEvent {
    object FetchStarted : GitradEvent()
    data class FetchSucceeded(val languages: Int, val ms: Long) : GitradEvent()
    data class FetchFailed(val error: GitradError) : GitradEvent()
    object CacheHit : GitradEvent()
    object BundleFallback : GitradEvent()
}
