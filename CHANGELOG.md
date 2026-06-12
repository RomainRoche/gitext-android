# Changelog

All notable changes to the Gitext Android SDK are documented here.
This project follows [Semantic Versioning](https://semver.org).

---

## [Unreleased]

### Added
- GitHub Actions CI pipeline running unit tests and release assembly on every push and pull request.
- `FetchCacheReloadIntegrationTest`: end-to-end test covering fetch → persist → reload → string resolution flow.
- `GitextSingletonTest`: unit tests for `Gitext` singleton covering `string()`, `prefetch()`, `refresh()`, and event emission.
- `FakeTranslationRepository` shared test helper for use-case and singleton tests.
- KDoc on all public API surfaces: `Gitext`, `GitextConfig`, `GitextError`, `GitextEvent`, `GitextSyncWorker`, `GitextNamespace`, `rememberGitextString`, `rememberGitextStrings`, `GitextStrings`.
- MIT `LICENSE` file.

---

## [1.0.0] — 2024

### Added
- Initial public release.
- OTA translation download with exponential-backoff retry (up to 5 attempts).
- Three-tier cache: memory → disk (`cacheDir`) → bundled asset baseline.
- API-key-hashed disk cache directory to isolate multiple configurations.
- `Gitext.configure()`, `Gitext.string()`, `Gitext.refresh()`, `Gitext.prefetch()`.
- Jetpack Compose helpers: `rememberGitextString`, `rememberGitextStrings`.
- Namespace scoping via `Gitext.scoped()` and `GitextConfig.namespace`.
- CLDR-compliant plural rules for 20+ language families.
- `GitextSyncWorker` for WorkManager-based periodic background sync.
- Observable SDK lifecycle events via `Gitext.onEvent()`.
- ProGuard / R8 consumer rules for all public API types.
- Published to JitPack as `io.gitext:sdk-android`.
