package io.gitext.sdk.data

import android.content.Context
import io.gitext.sdk.data.datasources.BundleTranslationDataSource
import io.gitext.sdk.data.datasources.LocalTranslationDataSource
import io.gitext.sdk.data.datasources.RemoteTranslationDataSource
import io.gitext.sdk.data.repositories.DefaultTranslationRepository
import io.gitext.sdk.domain.repositories.TranslationRepository

internal object TranslationRepositoryFactory {

    fun make(context: Context, apiKey: String, baseUrl: String): TranslationRepository =
        DefaultTranslationRepository(
            remote = RemoteTranslationDataSource(apiKey, baseUrl),
            local = LocalTranslationDataSource(context, cacheId(apiKey)),
            bundle = BundleTranslationDataSource(context),
        )

    /** FNV-1a 64-bit hash of the API key, expressed as a lowercase hex string. */
    fun cacheId(apiKey: String): String {
        var hash = -3750763034362895579L // 14695981039346656037 as signed Long
        for (byte in apiKey.toByteArray()) {
            hash = hash xor byte.toLong()
            hash *= 1099511628211L
        }
        return java.lang.Long.toUnsignedString(hash, 16)
    }
}
