package io.gitrad.sdk.support

import io.gitrad.sdk.domain.entities.TranslationPayload
import io.gitrad.sdk.domain.repositories.TranslationRepository

internal class FakeTranslationRepository : TranslationRepository {

    var remoteFetch: suspend () -> TranslationPayload = {
        throw IllegalStateException("remoteFetch not configured")
    }

    var cached: TranslationPayload? = null
    var bundled: TranslationPayload? = null
    var saveTime: Long? = null

    override suspend fun fetchRemote(): TranslationPayload = remoteFetch()
    override fun loadCached(): TranslationPayload? = cached
    override fun loadBundled(): TranslationPayload? = bundled
    override fun cacheModificationDate(): Long? = saveTime

    override fun save(payload: TranslationPayload) {
        cached = payload
        saveTime = System.currentTimeMillis()
    }
}
