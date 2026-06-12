package io.gitrad.sdk.sdk

/**
 * Immutable configuration for the Gitrad SDK. Pass an instance to
 * [Gitrad.configure] at app startup.
 *
 * @param apiKey       Your Gitrad project API key. Keep this out of version
 *                     control; inject it via `BuildConfig` or a secrets manager.
 * @param baseUrl      Base URL of the Gitrad server
 *                     (e.g. `"https://app.gitrad.io"`). Must not end with `/`.
 * @param maxCacheAge  How long (in seconds) the on-disk translation cache is
 *                     considered fresh before [Gitrad.refresh] triggers a remote
 *                     fetch. Set to `0` to always fetch. Defaults to `3600`.
 * @param namespace    Optional key prefix applied automatically by [Gitrad.string].
 *                     When set, `string("title")` first tries `"$namespace.title"`,
 *                     then falls back to the bare key.
 */
data class GitradConfig(
    val apiKey: String,
    val baseUrl: String,
    val maxCacheAge: Long = 3600L,
    val namespace: String? = null,
)
