package io.gitrad.sdk.domain.usecases

import io.gitrad.sdk.domain.entities.TranslationPayload
import io.gitrad.sdk.domain.repositories.TranslationRepository

internal class FetchTranslationsUseCase(
    private val repository: TranslationRepository,
) {
    suspend fun execute(): TranslationPayload {
        val payload = repository.fetchRemote()
        repository.save(payload)
        return payload
    }
}
