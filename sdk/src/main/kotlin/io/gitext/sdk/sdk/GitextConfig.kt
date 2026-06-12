package io.gitext.sdk.sdk

/**
 * Immutable configuration for the Gitext SDK. Pass an instance to
 * [Gitext.configure] at app startup.
 *
 * @param apiKey       Your Gitext project API key. Keep this out of version
 *                     control; inject it via `BuildConfig` or a secrets manager.
 * @param baseUrl      Base URL of the Gitext server
 *                     (e.g. `"https://app.gitext.io"`). Must not end with `/`.
 * @param maxCacheAge  How long (in seconds) the on-disk translation cache is
 *                     considered fresh before [Gitext.refresh] triggers a remote
 *                     fetch. Set to `0` to always fetch. Defaults to `3600`.
 * @param namespace    Optional key prefix applied automatically by [Gitext.string].
 *                     When set, `string("title")` first tries `"$namespace.title"`,
 *                     then falls back to the bare key.
 */
data class GitextConfig(
    val apiKey: String,
    val baseUrl: String,
    val maxCacheAge: Long = 3600L,
    val namespace: String? = null,
)
