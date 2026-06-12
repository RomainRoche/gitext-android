package io.gitext.sdk.domain.usecases

import io.gitext.sdk.domain.entities.TranslationPayload
import io.gitext.sdk.domain.repositories.TranslationRepository

internal enum class InitialPayloadSource { CACHE, BUNDLE, EMPTY }

internal class LoadInitialTranslationsUseCase(
    private val repository: TranslationRepository,
) {
    fun execute(): Pair<TranslationPayload, InitialPayloadSource> {
        repository.loadCached()?.let { return it to InitialPayloadSource.CACHE }
        repository.loadBundled()?.let { return it to InitialPayloadSource.BUNDLE }
        return TranslationPayload.empty to InitialPayloadSource.EMPTY
    }
}
