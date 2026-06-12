package io.gitrad.sdk.sdk

/**
 * A namespace-scoped view of the Gitrad translation table. Obtain an instance
 * via [Gitrad.scoped]. Every [string] call prefixes [key] with the namespace,
 * falling back to the unprefixed key when no match is found.
 */
class GitradNamespace internal constructor(private val prefix: String) {

    /**
     * Returns the translated string for `$prefix.$key`, with a fallback to the
     * bare [key]. Plural selection applies when [count] is non-null.
     */
    fun string(key: String, count: Int? = null, language: String? = null): String =
        Gitrad.string(prefixedKey = "$prefix.$key", originalKey = key, count = count, language = language)
}
