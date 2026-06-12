package io.gitext.sdk

import io.gitext.sdk.data.dtos.TranslationEntryDto
import io.gitext.sdk.data.dtos.TranslationPayloadDto
import io.gitext.sdk.data.mappers.TranslationPayloadMapper
import io.gitext.sdk.domain.entities.TranslationEntry
import io.gitext.sdk.domain.entities.TranslationPayload
import org.junit.Assert.assertEquals
import org.junit.Test

class TranslationPayloadMapperTest {

    @Test fun maps_string_entry_to_domain() {
        val dto = TranslationPayloadDto(
            namespaces = emptyList(),
            translations = mapOf("en" to mapOf("hello" to TranslationEntryDto.Str("Hello"))),
        )
        val domain = TranslationPayloadMapper.toDomain(dto)
        assertEquals(TranslationEntry.Str("Hello"), domain.translations["en"]?.get("hello"))
    }

    @Test fun maps_plural_entry_to_domain() {
        val dto = TranslationPayloadDto(
            namespaces = emptyList(),
            translations = mapOf("en" to mapOf("count" to TranslationEntryDto.Plurals(mapOf("one" to "1 item", "other" to "%d items")))),
        )
        val domain = TranslationPayloadMapper.toDomain(dto)
        assertEquals(TranslationEntry.Plurals(mapOf("one" to "1 item", "other" to "%d items")), domain.translations["en"]?.get("count"))
    }

    @Test fun round_trips_domain_to_dto_and_back() {
        val original = TranslationPayload(
            translations = mapOf("fr" to mapOf("greeting" to TranslationEntry.Str("Bonjour"))),
            namespaces = listOf("app"),
        )
        val dto = TranslationPayloadMapper.toDto(original)
        val restored = TranslationPayloadMapper.toDomain(dto)
        assertEquals(original, restored)
    }

    @Test fun parses_json_with_plural_entry() {
        val json = """{"en":{"count":{"one":"1 item","other":"%d items"}}}"""
        val dto = TranslationPayloadDto.fromJson(json)
        val domain = TranslationPayloadMapper.toDomain(dto)
        assertEquals(
            TranslationEntry.Plurals(mapOf("one" to "1 item", "other" to "%d items")),
            domain.translations["en"]?.get("count"),
        )
    }
}
