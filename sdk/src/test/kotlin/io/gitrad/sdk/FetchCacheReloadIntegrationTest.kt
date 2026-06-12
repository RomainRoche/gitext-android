package io.gitrad.sdk

import io.gitrad.sdk.domain.entities.TranslationEntry
import io.gitrad.sdk.domain.entities.TranslationPayload
import io.gitrad.sdk.domain.usecases.FetchTranslationsUseCase
import io.gitrad.sdk.domain.usecases.InitialPayloadSource
import io.gitrad.sdk.domain.usecases.LoadInitialTranslationsUseCase
import io.gitrad.sdk.domain.usecases.ResolveTranslationUseCase
import io.gitrad.sdk.support.FakeTranslationRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FetchCacheReloadIntegrationTest {

    @Test fun fetch_saves_to_cache_and_reload_returns_same_payload() = runTest {
        val original = payload(
            "en" to mapOf(
                "app.name" to TranslationEntry.Str("MyApp"),
                "items" to TranslationEntry.Plurals(mapOf("one" to "%d item", "other" to "%d items")),
            ),
            "fr" to mapOf("app.name" to TranslationEntry.Str("MonApp")),
        )
        val repo = FakeTranslationRepository()
        repo.remoteFetch = { original }

        // Fetch and persist
        val fetched = FetchTranslationsUseCase(repo).execute()
        assertEquals(original, fetched)
        assertEquals(original, repo.cached)

        // Simulate app restart: reload from cache
        val (reloaded, source) = LoadInitialTranslationsUseCase(repo).execute()
        assertEquals(InitialPayloadSource.CACHE, source)
        assertEquals(fetched, reloaded)

        // String resolution works on the reloaded payload
        val resolver = ResolveTranslationUseCase()
        assertEquals("MyApp", resolver.execute("app.name", null, "en", reloaded))
        assertEquals("MonApp", resolver.execute("app.name", null, "fr", reloaded))
        assertEquals("3 items", resolver.execute("items", 3, "en", reloaded))
        assertEquals("1 item", resolver.execute("items", 1, "en", reloaded))
    }

    @Test fun bundle_fallback_when_no_cache() {
        val bundlePayload = payload("en" to mapOf("offline.key" to TranslationEntry.Str("Offline")))
        val repo = FakeTranslationRepository()
        repo.bundled = bundlePayload

        val (loaded, source) = LoadInitialTranslationsUseCase(repo).execute()
        assertEquals(InitialPayloadSource.BUNDLE, source)
        assertEquals(bundlePayload, loaded)
    }

    @Test fun cache_supersedes_bundle_when_both_available() {
        val cachePayload = payload("en" to mapOf("key" to TranslationEntry.Str("FromCache")))
        val bundlePayload = payload("en" to mapOf("key" to TranslationEntry.Str("FromBundle")))
        val repo = FakeTranslationRepository()
        repo.cached = cachePayload
        repo.bundled = bundlePayload

        val (loaded, source) = LoadInitialTranslationsUseCase(repo).execute()
        assertEquals(InitialPayloadSource.CACHE, source)
        assertEquals(cachePayload, loaded)
    }

    @Test fun empty_returned_when_no_cache_and_no_bundle() {
        val (loaded, source) = LoadInitialTranslationsUseCase(FakeTranslationRepository()).execute()
        assertEquals(InitialPayloadSource.EMPTY, source)
        assertEquals(TranslationPayload.empty, loaded)
    }

    private fun payload(vararg entries: Pair<String, Map<String, TranslationEntry>>) =
        TranslationPayload(translations = mapOf(*entries))
}
