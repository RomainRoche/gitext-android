package io.gitext.sdk.data.repositories

import io.gitext.sdk.data.datasources.BundleTranslationDataSource
import io.gitext.sdk.data.datasources.LocalTranslationDataSource
import io.gitext.sdk.data.datasources.RemoteTranslationDataSource
import io.gitext.sdk.data.mappers.TranslationPayloadMapper
import io.gitext.sdk.domain.entities.TranslationPayload
import io.gitext.sdk.domain.repositories.TranslationRepository

internal class DefaultTranslationRepository(
    private val remote: RemoteTranslationDataSource,
    private val local: LocalTranslationDataSource,
    private val bundle: BundleTranslationDataSource,
) : TranslationRepository {

    override suspend fun fetchRemote(): TranslationPayload =
        TranslationPayloadMapper.toDomain(remote.fetch())

    override fun loadCached(): TranslationPayload? =
        local.read()?.let(TranslationPayloadMapper::toDomain)

    override fun save(payload: TranslationPayload) =
        local.write(TranslationPayloadMapper.toDto(payload))

    override fun loadBundled(): TranslationPayload? =
        bundle.load()?.let(TranslationPayloadMapper::toDomain)

    override fun cacheModificationDate(): Long? = local.modificationDate()
}
