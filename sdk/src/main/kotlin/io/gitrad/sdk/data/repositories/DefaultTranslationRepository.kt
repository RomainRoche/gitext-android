package io.gitrad.sdk.data.repositories

import io.gitrad.sdk.data.datasources.BundleTranslationDataSource
import io.gitrad.sdk.data.datasources.LocalTranslationDataSource
import io.gitrad.sdk.data.datasources.RemoteTranslationDataSource
import io.gitrad.sdk.data.mappers.TranslationPayloadMapper
import io.gitrad.sdk.domain.entities.TranslationPayload
import io.gitrad.sdk.domain.repositories.TranslationRepository

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
