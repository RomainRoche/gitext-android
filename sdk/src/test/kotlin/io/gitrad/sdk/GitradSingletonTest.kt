package io.gitrad.sdk

import io.gitrad.sdk.domain.entities.TranslationEntry
import io.gitrad.sdk.domain.entities.TranslationFetchError
import io.gitrad.sdk.domain.entities.TranslationPayload
import io.gitrad.sdk.sdk.Gitrad
import io.gitrad.sdk.sdk.GitradConfig
import io.gitrad.sdk.sdk.GitradError
import io.gitrad.sdk.sdk.GitradEvent
import io.gitrad.sdk.support.FakeTranslationRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GitradSingletonTest {

    private val config = GitradConfig(apiKey = "test-key", baseUrl = "http://localhost")

    @Before fun setUp() = Gitrad.resetForTest()
    @After fun tearDown() = Gitrad.resetForTest()

    // ── string() ───────────────────────────────────────────────────────────────

    @Test fun string_returns_key_when_not_configured() {
        assertEquals("app.name", Gitrad.string("app.name"))
    }

    @Test fun string_returns_translation_after_configure() {
        val repo = FakeTranslationRepository()
        repo.cached = payload("en" to mapOf("greeting" to TranslationEntry.Str("Hello")))
        Gitrad.configureForTest(config, repo)
        assertEquals("Hello", Gitrad.string("greeting", language = "en"))
    }

    @Test fun string_returns_key_when_key_is_missing() {
        val repo = FakeTranslationRepository()
        repo.cached = payload("en" to mapOf("other" to TranslationEntry.Str("Other")))
        Gitrad.configureForTest(config, repo)
        assertEquals("missing.key", Gitrad.string("missing.key", language = "en"))
    }

    @Test fun configure_loads_initial_cache() {
        val repo = FakeTranslationRepository()
        repo.cached = payload("fr" to mapOf("bienvenue" to TranslationEntry.Str("Bienvenue")))
        Gitrad.configureForTest(config, repo)
        assertEquals("Bienvenue", Gitrad.string("bienvenue", language = "fr"))
    }

    // ── prefetch() ─────────────────────────────────────────────────────────────

    @Test fun prefetch_updates_payload_and_increments_revisionFlow() = runTest {
        val remotePayload = payload("en" to mapOf("title" to TranslationEntry.Str("Remote Title")))
        val repo = FakeTranslationRepository()
        repo.remoteFetch = { remotePayload }
        Gitrad.configureForTest(config, repo)

        val before = Gitrad.revisionFlow.value
        Gitrad.prefetch()

        assertEquals(before + 1, Gitrad.revisionFlow.value)
        assertEquals("Remote Title", Gitrad.string("title", language = "en"))
    }

    @Test fun prefetch_emits_fetch_started_and_succeeded_events() = runTest {
        val events = mutableListOf<GitradEvent>()
        Gitrad.onEvent { events += it }
        val repo = FakeTranslationRepository()
        repo.remoteFetch = { payload("en" to mapOf("k" to TranslationEntry.Str("v"))) }
        Gitrad.configureForTest(config, repo)

        Gitrad.prefetch()

        assertTrue(events.any { it is GitradEvent.FetchStarted })
        assertTrue(events.any { it is GitradEvent.FetchSucceeded })
    }

    @Test fun prefetch_emits_fetch_failed_on_network_error() = runTest {
        val events = mutableListOf<GitradEvent>()
        Gitrad.onEvent { events += it }
        val repo = FakeTranslationRepository()
        repo.remoteFetch = { throw TranslationFetchError.Network(RuntimeException("no network")) }
        Gitrad.configureForTest(config, repo)

        Gitrad.prefetch()

        val failure = events.filterIsInstance<GitradEvent.FetchFailed>().firstOrNull()
        assertNotNull("Expected FetchFailed event", failure)
        assertTrue(failure!!.error is GitradError.NetworkError)
    }

    @Test fun prefetch_emits_not_configured_when_called_before_configure() = runTest {
        val events = mutableListOf<GitradEvent>()
        Gitrad.onEvent { events += it }

        Gitrad.prefetch()

        val failure = events.filterIsInstance<GitradEvent.FetchFailed>().firstOrNull()
        assertNotNull("Expected FetchFailed event", failure)
        assertTrue(failure!!.error is GitradError.NotConfigured)
    }

    @Test fun prefetch_does_not_revise_payload_on_failure() = runTest {
        val cached = payload("en" to mapOf("key" to TranslationEntry.Str("cached")))
        val repo = FakeTranslationRepository()
        repo.cached = cached
        repo.remoteFetch = { throw TranslationFetchError.Network(RuntimeException("down")) }
        Gitrad.configureForTest(config, repo)

        val revisionBefore = Gitrad.revisionFlow.value
        Gitrad.prefetch()

        assertEquals(revisionBefore, Gitrad.revisionFlow.value)
        assertEquals("cached", Gitrad.string("key", language = "en"))
    }

    // ── refresh() (stale check) ─────────────────────────────────────────────────

    @Test fun refresh_skips_fetch_when_cache_is_fresh() = runTest {
        var fetchCount = 0
        val repo = FakeTranslationRepository()
        repo.cached = payload("en" to mapOf("k" to TranslationEntry.Str("cached")))
        repo.saveTime = System.currentTimeMillis() // fresh
        repo.remoteFetch = { fetchCount++; payload("en" to mapOf("k" to TranslationEntry.Str("remote"))) }
        Gitrad.configureForTest(GitradConfig("key", "http://localhost", maxCacheAge = 3600L), repo)

        Gitrad.refresh()

        assertEquals(0, fetchCount)
    }

    @Test fun refresh_fetches_when_cache_is_stale() = runTest {
        var fetchCount = 0
        val repo = FakeTranslationRepository()
        repo.cached = payload("en" to mapOf("k" to TranslationEntry.Str("old")))
        // Cache is 2 hours old, maxCacheAge = 1 hour → stale
        repo.saveTime = System.currentTimeMillis() - 7_200_000L
        repo.remoteFetch = { fetchCount++; payload("en" to mapOf("k" to TranslationEntry.Str("fresh"))) }
        Gitrad.configureForTest(GitradConfig("key", "http://localhost", maxCacheAge = 3600L), repo)

        Gitrad.refresh()

        assertEquals(1, fetchCount)
        assertEquals("fresh", Gitrad.string("k", language = "en"))
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private fun payload(vararg entries: Pair<String, Map<String, TranslationEntry>>) =
        TranslationPayload(translations = mapOf(*entries))
}
