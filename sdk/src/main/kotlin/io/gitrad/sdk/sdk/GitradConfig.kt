package io.gitrad.sdk.sdk

data class GitradConfig(
    val apiKey: String,
    val baseUrl: String,
    val maxCacheAge: Long = 3600L,
    val namespace: String? = null,
)
