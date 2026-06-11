package io.gitrad.sdk.domain.usecases

import io.gitrad.sdk.domain.entities.TranslationEntry
import io.gitrad.sdk.domain.entities.TranslationPayload
import io.gitrad.sdk.domain.support.PluralRules

internal class ResolveTranslationUseCase {

    fun resolve(key: String, count: Int?, language: String, payload: TranslationPayload): String? {
        val base = baseLang(language)
        val entry = payload.translations[language]?.get(key)
            ?: payload.translations[base]?.get(key)
            ?: payload.translations["en"]?.get(key)
            ?: return null

        return when (entry) {
            is TranslationEntry.Str -> entry.value
            is TranslationEntry.Plurals -> {
                if (count != null) PluralRules.form(count, entry.map, language)
                else entry.map["other"]
            }
        }
    }

    fun execute(key: String, count: Int?, language: String, payload: TranslationPayload): String =
        resolve(key, count, language, payload) ?: key

    private fun baseLang(lang: String): String = lang.substringBefore("-")
}
