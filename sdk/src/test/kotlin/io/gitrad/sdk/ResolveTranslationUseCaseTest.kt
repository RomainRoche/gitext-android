package io.gitrad.sdk

import io.gitrad.sdk.domain.entities.TranslationEntry
import io.gitrad.sdk.domain.entities.TranslationPayload
import io.gitrad.sdk.domain.usecases.ResolveTranslationUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ResolveTranslationUseCaseTest {

    private val useCase = ResolveTranslationUseCase()

    @Test fun exact_language_match() {
        val payload = payload("fr-FR" to mapOf("greeting.hello" to TranslationEntry.Str("Salut")))
        assertEquals("Salut", useCase.execute("greeting.hello", null, "fr-FR", payload))
    }

    @Test fun base_language_fallback() {
        val payload = payload("fr" to mapOf("greeting.hello" to TranslationEntry.Str("Bonjour")))
        assertEquals("Bonjour", useCase.execute("greeting.hello", null, "fr-FR", payload))
    }

    @Test fun english_fallback() {
        val payload = payload("en" to mapOf("greeting.hello" to TranslationEntry.Str("Hello")))
        assertEquals("Hello", useCase.execute("greeting.hello", null, "de", payload))
    }

    @Test fun key_fallback_when_missing() {
        assertEquals("missing.key", useCase.execute("missing.key", null, "en", TranslationPayload.empty))
    }

    @Test fun resolve_returns_null_when_missing() {
        assertNull(useCase.resolve("missing.key", null, "en", TranslationPayload.empty))
    }

    @Test fun plural_resolution() {
        val pluralMap = mapOf(
            "zero" to "No notifications",
            "one" to "%d notification",
            "other" to "%d notifications",
        )
        val payload = payload("en" to mapOf("notifications.count" to TranslationEntry.Plurals(pluralMap)))
        assertEquals("No notifications", useCase.execute("notifications.count", 0, "en", payload))
        assertEquals("1 notification",   useCase.execute("notifications.count", 1, "en", payload))
        assertEquals("5 notifications",  useCase.execute("notifications.count", 5, "en", payload))
    }

    @Test fun plurals_without_count_returns_other() {
        val payload = payload("en" to mapOf("items" to TranslationEntry.Plurals(mapOf("one" to "1 item", "other" to "many items"))))
        assertEquals("many items", useCase.execute("items", null, "en", payload))
    }

    private fun payload(vararg entries: Pair<String, Map<String, TranslationEntry>>) =
        TranslationPayload(translations = mapOf(*entries))
}
