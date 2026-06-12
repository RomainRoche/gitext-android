# Changelog

All notable changes to the Gitrad Android SDK are documented here.
This project follows [Semantic Versioning](https://semver.org).

---

## [Unreleased]

### Added
- GitHub Actions CI pipeline running unit tests and release assembly on every push and pull request.
- `FetchCacheReloadIntegrationTest`: end-to-end test covering fetch → persist → reload → string resolution flow.
- `GitradSingletonTest`: unit tests for `Gitrad` singleton covering `string()`, `prefetch()`, `refresh()`, and event emission.
- `FakeTranslationRepository` shared test helper for use-case and singleton tests.
- KDoc on all public API surfaces: `Gitrad`, `GitradConfig`, `GitradError`, `GitradEvent`, `GitradSyncWorker`, `GitradNamespace`, `rememberGitradString`, `rememberGitradStrings`, `GitradStrings`.
- MIT `LICENSE` file.

---

## [1.0.0] — 2024

### Added
- Initial public release.
- OTA translation download with exponential-backoff retry (up to 5 attempts).
- Three-tier cache: memory → disk (`cacheDir`) → bundled asset baseline.
- API-key-hashed disk cache directory to isolate multiple configurations.
- `Gitrad.configure()`, `Gitrad.string()`, `Gitrad.refresh()`, `Gitrad.prefetch()`.
- Jetpack Compose helpers: `rememberGitradString`, `rememberGitradStrings`.
- Namespace scoping via `Gitrad.scoped()` and `GitradConfig.namespace`.
- CLDR-compliant plural rules for 20+ language families.
- `GitradSyncWorker` for WorkManager-based periodic background sync.
- Observable SDK lifecycle events via `Gitrad.onEvent()`.
- ProGuard / R8 consumer rules for all public API types.
- Published to JitPack as `io.gitrad:sdk-android`.
