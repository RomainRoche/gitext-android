package io.gitext.sdk

import io.gitext.sdk.domain.entities.TranslationEntry
import io.gitext.sdk.domain.entities.TranslationFetchError
import io.gitext.sdk.domain.entities.TranslationPayload
import io.gitext.sdk.sdk.Gitext
import io.gitext.sdk.sdk.GitextConfig
import io.gitext.sdk.sdk.GitextError
import io.gitext.sdk.sdk.GitextEvent
import io.gitext.sdk.support.FakeTranslationRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GitextSingletonTest {

    private val config = GitextConfig(apiKey = "test-key", baseUrl = "http://localhost")

    @Before fun setUp() = Gitext.resetForTest()
    @After fun tearDown() = Gitext.resetForTest()

    // ── string() ───────────────────────────────────────────────────────────────

    @Test fun string_returns_key_when_not_configured() {
        assertEquals("app.name", Gitext.string("app.name"))
    }

    @Test fun string_returns_translation_after_configure() {
        val repo = FakeTranslationRepository()
        repo.cached = payload("en" to mapOf("greeting" to TranslationEntry.Str("Hello")))
        Gitext.configureForTest(config, repo)
        assertEquals("Hello", Gitext.string("greeting", language = "en"))
    }

    @Test fun string_returns_key_when_key_is_missing() {
        val repo = FakeTranslationRepository()
        repo.cached = payload("en" to mapOf("other" to TranslationEntry.Str("Other")))
        Gitext.configureForTest(config, repo)
        assertEquals("missing.key", Gitext.string("missing.key", language = "en"))
    }

    @Test fun configure_loads_initial_cache() {
        val repo = FakeTranslationRepository()
        repo.cached = payload("fr" to mapOf("bienvenue" to TranslationEntry.Str("Bienvenue")))
        Gitext.configureForTest(config, repo)
        assertEquals("Bienvenue", Gitext.string("bienvenue", language = "fr"))
    }

    // ── prefetch() ─────────────────────────────────────────────────────────────

    @Test fun prefetch_updates_payload_and_increments_revisionFlow() = runTest {
        val remotePayload = payload("en" to mapOf("title" to TranslationEntry.Str("Remote Title")))
        val repo = FakeTranslationRepository()
        repo.remoteFetch = { remotePayload }
        Gitext.configureForTest(config, repo)

        val before = Gitext.revisionFlow.value
        Gitext.prefetch()

        assertEquals(before + 1, Gitext.revisionFlow.value)
        assertEquals("Remote Title", Gitext.string("title", language = "en"))
    }

    @Test fun prefetch_emits_fetch_started_and_succeeded_events() = runTest {
        val events = mutableListOf<GitextEvent>()
        Gitext.onEvent { events += it }
        val repo = FakeTranslationRepository()
        repo.remoteFetch = { payload("en" to mapOf("k" to TranslationEntry.Str("v"))) }
        Gitext.configureForTest(config, repo)

        Gitext.prefetch()

        assertTrue(events.any { it is GitextEvent.FetchStarted })
        assertTrue(events.any { it is GitextEvent.FetchSucceeded })
    }

    @Test fun prefetch_emits_fetch_failed_on_network_error() = runTest {
        val events = mutableListOf<GitextEvent>()
        Gitext.onEvent { events += it }
        val repo = FakeTranslationRepository()
        repo.remoteFetch = { throw TranslationFetchError.Network(RuntimeException("no network")) }
        Gitext.configureForTest(config, repo)

        Gitext.prefetch()

        val failure = events.filterIsInstance<GitextEvent.FetchFailed>().firstOrNull()
        assertNotNull("Expected FetchFailed event", failure)
        assertTrue(failure!!.error is GitextError.NetworkError)
    }

    @Test fun prefetch_emits_not_configured_when_called_before_configure() = runTest {
        val events = mutableListOf<GitextEvent>()
        Gitext.onEvent { events += it }

        Gitext.prefetch()

        val failure = events.filterIsInstance<GitextEvent.FetchFailed>().firstOrNull()
        assertNotNull("Expected FetchFailed event", failure)
        assertTrue(failure!!.error is GitextError.NotConfigured)
    }

    @Test fun prefetch_does_not_revise_payload_on_failure() = runTest {
        val cached = payload("en" to mapOf("key" to TranslationEntry.Str("cached")))
        val repo = FakeTranslationRepository()
        repo.cached = cached
        repo.remoteFetch = { throw TranslationFetchError.Network(RuntimeException("down")) }
        Gitext.configureForTest(config, repo)

        val revisionBefore = Gitext.revisionFlow.value
        Gitext.prefetch()

        assertEquals(revisionBefore, Gitext.revisionFlow.value)
        assertEquals("cached", Gitext.string("key", language = "en"))
    }

    // ── refresh() (stale check) ─────────────────────────────────────────────────

    @Test fun refresh_skips_fetch_when_cache_is_fresh() = runTest {
        var fetchCount = 0
        val repo = FakeTranslationRepository()
        repo.cached = payload("en" to mapOf("k" to TranslationEntry.Str("cached")))
        repo.saveTime = System.currentTimeMillis() // fresh
        repo.remoteFetch = { fetchCount++; payload("en" to mapOf("k" to TranslationEntry.Str("remote"))) }
        Gitext.configureForTest(GitextConfig("key", "http://localhost", maxCacheAge = 3600L), repo)

        Gitext.refresh()

        assertEquals(0, fetchCount)
    }

    @Test fun refresh_fetches_when_cache_is_stale() = runTest {
        var fetchCount = 0
        val repo = FakeTranslationRepository()
        repo.cached = payload("en" to mapOf("k" to TranslationEntry.Str("old")))
        // Cache is 2 hours old, maxCacheAge = 1 hour → stale
        repo.saveTime = System.currentTimeMillis() - 7_200_000L
        repo.remoteFetch = { fetchCount++; payload("en" to mapOf("k" to TranslationEntry.Str("fresh"))) }
        Gitext.configureForTest(GitextConfig("key", "http://localhost", maxCacheAge = 3600L), repo)

        Gitext.refresh()

        assertEquals(1, fetchCount)
        assertEquals("fresh", Gitext.string("k", language = "en"))
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private fun payload(vararg entries: Pair<String, Map<String, TranslationEntry>>) =
        TranslationPayload(translations = mapOf(*entries))
}
