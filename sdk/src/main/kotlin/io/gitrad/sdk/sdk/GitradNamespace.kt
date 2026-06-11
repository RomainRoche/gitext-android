package io.gitrad.sdk.sdk

class GitradNamespace internal constructor(private val prefix: String) {

    fun string(key: String, count: Int? = null, language: String? = null): String =
        Gitrad.string(prefixedKey = "$prefix.$key", originalKey = key, count = count, language = language)
}
