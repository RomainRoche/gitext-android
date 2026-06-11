package io.gitrad.sdk.data.mappers

import io.gitrad.sdk.data.dtos.TranslationEntryDto
import io.gitrad.sdk.data.dtos.TranslationPayloadDto
import io.gitrad.sdk.domain.entities.TranslationEntry
import io.gitrad.sdk.domain.entities.TranslationPayload

internal object TranslationPayloadMapper {

    fun toDomain(dto: TranslationPayloadDto): TranslationPayload {
        val translations = dto.translations.mapValues { (_, langDict) ->
            langDict.mapValues { (_, entry) ->
                when (entry) {
                    is TranslationEntryDto.Str -> TranslationEntry.Str(entry.value)
                    is TranslationEntryDto.Plurals -> TranslationEntry.Plurals(entry.map)
                }
            }
        }
        return TranslationPayload(translations = translations, namespaces = dto.namespaces)
    }

    fun toDto(payload: TranslationPayload): TranslationPayloadDto {
        val translations = payload.translations.mapValues { (_, langDict) ->
            langDict.mapValues { (_, entry) ->
                when (entry) {
                    is TranslationEntry.Str -> TranslationEntryDto.Str(entry.value)
                    is TranslationEntry.Plurals -> TranslationEntryDto.Plurals(entry.map)
                }
            }
        }
        return TranslationPayloadDto(namespaces = payload.namespaces, translations = translations)
    }
}
