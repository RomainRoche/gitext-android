package io.gitext.sdk.domain.usecases

import io.gitext.sdk.domain.entities.TranslationPayload
import io.gitext.sdk.domain.repositories.TranslationRepository

internal class FetchTranslationsUseCase(
    private val repository: TranslationRepository,
) {
    suspend fun execute(): TranslationPayload {
        val payload = repository.fetchRemote()
        repository.save(payload)
        return payload
    }
}
