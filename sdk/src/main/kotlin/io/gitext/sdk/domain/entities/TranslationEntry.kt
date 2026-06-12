package io.gitext.sdk.domain.entities

internal sealed class TranslationEntry {
    data class Str(val value: String) : TranslationEntry()
    data class Plurals(val map: Map<String, String>) : TranslationEntry()
}
