package io.gitrad.sdk.domain.repositories

import io.gitrad.sdk.domain.entities.TranslationPayload

internal interface TranslationRepository {
    suspend fun fetchRemote(): TranslationPayload
    fun loadCached(): TranslationPayload?
    fun save(payload: TranslationPayload)
    fun loadBundled(): TranslationPayload?
    fun cacheModificationDate(): Long?
}
