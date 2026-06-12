package io.gitext.sdk.domain.repositories

import io.gitext.sdk.domain.entities.TranslationPayload

internal interface TranslationRepository {
    suspend fun fetchRemote(): TranslationPayload
    fun loadCached(): TranslationPayload?
    fun save(payload: TranslationPayload)
    fun loadBundled(): TranslationPayload?
    fun cacheModificationDate(): Long?
}
