package io.gitrad.sdk.sdk

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import io.gitrad.sdk.data.TranslationRepositoryFactory
import io.gitrad.sdk.domain.entities.TranslationPayload
import io.gitrad.sdk.domain.repositories.TranslationRepository
import io.gitrad.sdk.domain.usecases.FetchTranslationsUseCase
import io.gitrad.sdk.domain.usecases.InitialPayloadSource
import io.gitrad.sdk.domain.usecases.LoadInitialTranslationsUseCase
import io.gitrad.sdk.domain.usecases.ResolveTranslationUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Central singleton for the Gitrad OTA translation SDK.
 *
 * Call [configure] once at app startup (e.g. in `Application.onCreate`).
 * After configuration, use [string] to look up translated strings, or the
 * Compose helpers [rememberGitradString] / [rememberGitradStrings] for
 * automatic recomposition when remote translations are refreshed.
 *
 * Background sync can be scheduled via [GitradSyncWorker].
 */
object Gitrad {

    private const val SERVER_CACHE_MAX_AGE_MS = 60_000L

    private val lock = ReentrantLock()
    private var _configEpoch = 0
    private var _config: GitradConfig? = null
    private var _repository: TranslationRepository? = null
    private var _loadInitial: LoadInitialTranslationsUseCase? = null
    private var _fetch: FetchTranslationsUseCase? = null
    private val _resolve = ResolveTranslationUseCase()
    private var _payload: TranslationPayload = TranslationPayload.empty
    private var _lastFetchTime: Long? = null
    private var _eventHandler: ((GitradEvent) -> Unit)? = null

    private val _revisionFlow = MutableStateFlow(0)

    /**
     * Increments each time a remote fetch succeeds. Compose helpers collect
     * this flow to trigger recomposition automatically.
     */
    val revisionFlow: StateFlow<Int> = _revisionFlow.asStateFlow()

    /**
     * Configures the SDK with individual parameters. Convenience overload of
     * [configure(Context, GitradConfig)].
     *
     * This call is synchronous: it immediately loads any cached or bundled
     * translations so that [string] returns a meaningful value before the
     * first network fetch completes.
     *
     * @param context Application context (only the application context is retained).
     * @param apiKey  Your Gitrad project API key.
     * @param baseUrl Base URL of the Gitrad server (e.g. `"https://app.gitrad.io"`).
     * @param maxCacheAge Seconds before the on-disk cache is considered stale and
     *   a remote fetch is triggered by [refresh]. Pass `0` to always fetch.
     *   Defaults to 3600 (one hour).
     * @param namespace Optional default namespace prefix applied to every [string] call.
     */
    fun configure(
        context: Context,
        apiKey: String,
        baseUrl: String,
        maxCacheAge: Long = 3600L,
        namespace: String? = null,
    ) {
        configure(context, GitradConfig(apiKey, baseUrl, maxCacheAge, namespace))
    }

    /**
     * Configures the SDK with a [GitradConfig] object. See the parameter-based
     * overload for full documentation.
     */
    fun configure(context: Context, config: GitradConfig) {
        val repo = TranslationRepositoryFactory.make(context.applicationContext, config.apiKey, config.baseUrl)
        val loadInitial = LoadInitialTranslationsUseCase(repo)
        val fetch = FetchTranslationsUseCase(repo)
        val epoch: Int

        lock.withLock {
            epoch = ++_configEpoch
            _config = config
            _repository = repo
            _loadInitial = loadInitial
            _fetch = fetch
            _payload = TranslationPayload.empty
            _lastFetchTime = null
        }

        // I/O happens outside the lock so it doesn't block callers reading _payload.
        // The epoch check below ensures a second configure() call invalidates this result.
        val (payload, source) = loadInitial.execute()

        lock.withLock {
            if (_configEpoch == epoch) _payload = payload
        }

        when (source) {
            InitialPayloadSource.CACHE -> emit(GitradEvent.CacheHit)
            InitialPayloadSource.BUNDLE -> emit(GitradEvent.BundleFallback)
            InitialPayloadSource.EMPTY -> Unit
        }
    }

    /**
     * Fetches translations from the remote server unconditionally (subject to a
     * 60-second server-side dedup window). Suspends until the fetch completes or
     * fails. Failures are reported via [onEvent] but do not throw.
     */
    suspend fun prefetch() = fetchAlways()

    /**
     * Fetches translations only when the on-disk cache is older than
     * [GitradConfig.maxCacheAge]. No-op if the cache is still fresh or if the
     * SDK is not configured. Failures are reported via [onEvent].
     */
    suspend fun refresh() = fetchIfStale()

    /**
     * Returns the translated string for [key] in [language] (defaults to the
     * device locale). If [count] is provided and the entry has plural forms,
     * the appropriate plural is selected using CLDR rules.
     *
     * Falls back through: exact locale → base language → English → [key] itself.
     *
     * When a [GitradConfig.namespace] is configured, the namespaced key is tried
     * first and the bare key is used as a fallback.
     *
     * @param key      Translation key (e.g. `"app.title"`).
     * @param count    Item count for plural selection; pass `null` for non-plural strings.
     * @param language BCP-47 language tag (e.g. `"fr"`, `"pt-BR"`). Defaults to device locale.
     */
    fun string(key: String, count: Int? = null, language: String? = null): String {
        val lang = language ?: currentLanguage()
        val (payload, namespace) = lock.withLock { _payload to _config?.namespace }
        return if (namespace != null) {
            _resolve.resolve("$namespace.$key", count, lang, payload)
                ?: _resolve.resolve(key, count, lang, payload)
                ?: key
        } else {
            _resolve.execute(key, count, lang, payload)
        }
    }

    internal fun string(prefixedKey: String, originalKey: String, count: Int?, language: String?): String {
        val lang = language ?: currentLanguage()
        val payload = lock.withLock { _payload }
        return _resolve.resolve(prefixedKey, count, lang, payload)
            ?: _resolve.resolve(originalKey, count, lang, payload)
            ?: originalKey
    }

    /**
     * Returns a [GitradNamespace] that prefixes every key with [namespace],
     * falling back to the unprefixed key when no match is found.
     */
    fun scoped(namespace: String): GitradNamespace = GitradNamespace(namespace)

    /** Namespaces defined in the currently loaded payload. */
    val namespaces: List<String> get() = lock.withLock { _payload.namespaces }

    /**
     * Registers an event handler invoked on SDK lifecycle events such as fetch
     * start/success/failure and cache hits. The handler is called on the caller's
     * thread; avoid blocking work inside it.
     *
     * @param handler Lambda receiving a [GitradEvent].
     */
    fun onEvent(handler: (GitradEvent) -> Unit) {
        lock.withLock { _eventHandler = handler }
    }

    private suspend fun fetchAlways() {
        val (fetch, lastFetch) = lock.withLock { _fetch to _lastFetchTime }
        if (fetch == null) {
            emit(GitradEvent.FetchFailed(GitradError.NotConfigured))
            return
        }

        if (lastFetch != null && System.currentTimeMillis() - lastFetch < SERVER_CACHE_MAX_AGE_MS) return

        emit(GitradEvent.FetchStarted)
        val start = System.currentTimeMillis()

        runCatching { fetch.execute() }
            .onSuccess { payload ->
                lock.withLock {
                    _payload = payload
                    _lastFetchTime = System.currentTimeMillis()
                }
                val ms = System.currentTimeMillis() - start
                emit(GitradEvent.FetchSucceeded(payload.translations.size, ms))
                _revisionFlow.value = _revisionFlow.value + 1
            }
            .onFailure { error ->
                emit(GitradEvent.FetchFailed(GitradError.from(error)))
            }
    }

    private suspend fun fetchIfStale() {
        val (repo, config) = lock.withLock { _repository to _config }
        repo ?: return
        config ?: return

        val cacheDate = repo.cacheModificationDate()
        val isStale = when {
            config.maxCacheAge == 0L -> true
            cacheDate == null -> true
            else -> System.currentTimeMillis() - cacheDate > config.maxCacheAge * 1_000L
        }
        if (isStale) fetchAlways()
    }

    private fun emit(event: GitradEvent) {
        val handler = lock.withLock { _eventHandler }
        if (handler != null) runCatching { handler(event) }
    }

    private fun currentLanguage(): String {
        val locale = Locale.getDefault()
        return if (locale.country.isNotEmpty()) "${locale.language}-${locale.country}"
        else locale.language
    }

    // Bypass Context/factory for JVM unit tests. Not part of the public API.
    internal fun configureForTest(config: GitradConfig, repository: TranslationRepository) {
        val loadInitial = LoadInitialTranslationsUseCase(repository)
        val fetch = FetchTranslationsUseCase(repository)
        val epoch: Int
        lock.withLock {
            epoch = ++_configEpoch
            _config = config
            _repository = repository
            _loadInitial = loadInitial
            _fetch = fetch
            _payload = TranslationPayload.empty
            _lastFetchTime = null
        }
        val (payload, _) = loadInitial.execute()
        lock.withLock { if (_configEpoch == epoch) _payload = payload }
    }

    internal fun resetForTest() {
        lock.withLock {
            ++_configEpoch
            _config = null
            _repository = null
            _loadInitial = null
            _fetch = null
            _payload = TranslationPayload.empty
            _lastFetchTime = null
            _eventHandler = null
        }
        _revisionFlow.value = 0
    }
}

// Compose integration

/**
 * Returns the translated string for [key], automatically recomposing when
 * remote translations are refreshed. Plural selection is applied when [count]
 * is non-null. [language] defaults to the device locale.
 */
@Composable
fun rememberGitradString(key: String, count: Int? = null, language: String? = null): String {
    val revision = Gitrad.revisionFlow.collectAsState()
    return remember(revision.value, key, count, language) {
        Gitrad.string(key, count, language)
    }
}

/**
 * Snapshot helper for accessing multiple translations inside a single Compose
 * scope. Obtain an instance via [rememberGitradStrings] and index into it with
 * `[]` for simple strings or [plural] for count-based lookups.
 */
data class GitradStrings(private val namespace: String? = null) {
    operator fun get(key: String): String = if (namespace != null) {
        Gitrad.string(prefixedKey = "$namespace.$key", originalKey = key, count = null, language = null)
    } else {
        Gitrad.string(key)
    }

    fun plural(key: String, count: Int, language: String? = null): String = if (namespace != null) {
        Gitrad.string(prefixedKey = "$namespace.$key", originalKey = key, count = count, language = language)
    } else {
        Gitrad.string(key, count, language)
    }
}

/**
 * Returns a [GitradStrings] snapshot that recomposes automatically when remote
 * translations are refreshed. Optionally scoped to [namespace].
 */
@Composable
fun rememberGitradStrings(namespace: String? = null): State<GitradStrings> {
    val revision = Gitrad.revisionFlow.collectAsState()
    return remember(revision.value, namespace) {
        derivedStateOf { GitradStrings(namespace) }
    }
}
