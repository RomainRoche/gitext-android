package io.gitext.sdk.domain.entities

internal data class TranslationPayload(
    val translations: Map<String, Map<String, TranslationEntry>>,
    val namespaces: List<String> = emptyList(),
) {
    val isEmpty: Boolean get() = translations.isEmpty()

    companion object {
        val empty = TranslationPayload(translations = emptyMap())
    }
}
