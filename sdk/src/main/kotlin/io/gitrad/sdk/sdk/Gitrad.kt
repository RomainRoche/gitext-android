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

object Gitrad {

    private const val SERVER_CACHE_MAX_AGE_MS = 60_000L

    private val lock = ReentrantLock()
    private var _config: GitradConfig? = null
    private var _repository: TranslationRepository? = null
    private var _loadInitial: LoadInitialTranslationsUseCase? = null
    private var _fetch: FetchTranslationsUseCase? = null
    private val _resolve = ResolveTranslationUseCase()
    private var _payload: TranslationPayload = TranslationPayload.empty
    private var _lastFetchTime: Long? = null
    private var _eventHandler: ((GitradEvent) -> Unit)? = null

    private val _revisionFlow = MutableStateFlow(0)
    val revisionFlow: StateFlow<Int> = _revisionFlow.asStateFlow()

    fun configure(
        context: Context,
        apiKey: String,
        baseUrl: String,
        maxCacheAge: Long = 3600L,
        namespace: String? = null,
    ) {
        configure(context, GitradConfig(apiKey, baseUrl, maxCacheAge, namespace))
    }

    fun configure(context: Context, config: GitradConfig) {
        val repo = TranslationRepositoryFactory.make(context.applicationContext, config.apiKey, config.baseUrl)
        val loadInitial = LoadInitialTranslationsUseCase(repo)
        val fetch = FetchTranslationsUseCase(repo)

        lock.withLock {
            _config = config
            _repository = repo
            _loadInitial = loadInitial
            _fetch = fetch
        }

        val (payload, source) = loadInitial.execute()
        lock.withLock { _payload = payload }

        when (source) {
            InitialPayloadSource.CACHE -> emit(GitradEvent.CacheHit)
            InitialPayloadSource.BUNDLE -> emit(GitradEvent.BundleFallback)
            InitialPayloadSource.EMPTY -> Unit
        }
    }

    suspend fun prefetch() = fetchAlways()

    suspend fun refresh() = fetchIfStale()

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

    fun scoped(namespace: String): GitradNamespace = GitradNamespace(namespace)

    val namespaces: List<String> get() = lock.withLock { _payload.namespaces }

    fun onEvent(handler: (GitradEvent) -> Unit) {
        lock.withLock { _eventHandler = handler }
    }

    private suspend fun fetchAlways() {
        val (fetch, lastFetch) = lock.withLock { _fetch to _lastFetchTime }
        fetch ?: return

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
        handler?.invoke(event)
    }

    private fun currentLanguage(): String {
        val locale = Locale.getDefault()
        return if (locale.country.isNotEmpty()) "${locale.language}-${locale.country}"
        else locale.language
    }
}

// Compose integration

@Composable
fun rememberGitradString(key: String, count: Int? = null, language: String? = null): String {
    val revision = Gitrad.revisionFlow.collectAsState()
    return remember(revision.value, key, count, language) {
        Gitrad.string(key, count, language)
    }
}

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

@Composable
fun rememberGitradStrings(namespace: String? = null): State<GitradStrings> {
    val revision = Gitrad.revisionFlow.collectAsState()
    return remember(revision.value, namespace) {
        derivedStateOf { GitradStrings(namespace) }
    }
}
