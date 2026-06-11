# Gitrad Android SDK

OTA translation SDK for Android. Downloads translations from the Gitrad server, caches them locally, and exposes a simple string-lookup API with Jetpack Compose integration.

## Installation

Add the SDK module to your project or publish it to your local Maven repository. In `settings.gradle.kts`:

```kotlin
include(":sdk")
```

In your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":sdk"))
    // or, once published:
    // implementation("io.gitrad:sdk-android:1.0.0")
}
```

Add `INTERNET` permission to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

## Configuration

Call `configure()` once in `Application.onCreate()`:

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Gitrad.configure(
            context     = this,
            apiKey      = BuildConfig.GITRAD_API_KEY,
            baseUrl     = "https://app.gitrad.io",
            maxCacheAge = 3600L,           // seconds; 0 = always fetch
        )
        lifecycleScope.launch { Gitrad.prefetch() }
    }
}
```

Deliver `GITRAD_API_KEY` via CI build variables, never in source control.

## String lookup

```kotlin
// Simple key
val title = Gitrad.string("onboarding.welcome_title")

// With interpolation (SDK returns format string; app does substitution)
val template = Gitrad.string("greeting.welcome")      // "Welcome, %s!"
val text     = String.format(template, user.firstName)

// Plural
val badge = Gitrad.string("notifications.count", count = unreadCount)
// 0 → "No notifications", 1 → "1 notification", 5 → "5 notifications"
```

## Jetpack Compose integration

```kotlin
@Composable
fun WelcomeScreen() {
    val title = rememberGitradString("onboarding.welcome_title")
    Text(text = title)
}

// With plurals
@Composable
fun BadgeText(count: Int) {
    val text = rememberGitradString("notifications.count", count = count)
    Text(text = text)
}
```

`rememberGitradString` re-composes automatically after a remote refresh.

## Namespace support

For multi-module apps where different modules own different translation namespaces:

```kotlin
// Option 1: configure-level namespace (single namespace per app)
Gitrad.configure(context = this, apiKey = key, baseUrl = url, namespace = "app")
Gitrad.string("greeting.hello")   // looks up "app.greeting.hello"

// Option 2: scoped accessors (multi-namespace)
val payments = Gitrad.scoped("payments")
payments.string("checkout.confirm")   // looks up "payments.checkout.confirm"
```

## Foreground refresh

```kotlin
override fun onResume() {
    super.onResume()
    lifecycleScope.launch { Gitrad.refresh() }
}
```

## Background refresh with WorkManager

```kotlin
val work = PeriodicWorkRequestBuilder<GitradRefreshWorker>(1, TimeUnit.HOURS)
    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
    .build()
WorkManager.getInstance(context)
    .enqueueUniquePeriodicWork("gitrad_refresh", ExistingPeriodicWorkPolicy.KEEP, work)

class GitradRefreshWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        Gitrad.refresh()
        return Result.success()
    }
}
```

## Observability

```kotlin
Gitrad.onEvent { event ->
    when (event) {
        GitradEvent.FetchStarted -> analytics.track("gitrad_fetch_started")
        is GitradEvent.FetchSucceeded -> analytics.track("gitrad_fetch_ok", mapOf("langs" to event.languages, "ms" to event.ms))
        is GitradEvent.FetchFailed -> Crashlytics.recordException(event.error)
        GitradEvent.CacheHit -> Unit
        GitradEvent.BundleFallback -> analytics.track("gitrad_bundle_fallback")
    }
}
```

## Bundled baseline

Ship a `translations.json` snapshot with each release so the SDK can serve strings when offline and the disk cache is cold:

1. Place the file at `sdk/src/main/assets/gitrad-baseline/translations.json`
2. Update it as part of each release by downloading from `GET /api/ota/download` with your production API key

## Architecture

Clean Architecture — three layers, each depending only inward:

```
sdk/
  domain/    ← entities, repository protocol, use cases, PluralRules (no Android deps)
  data/      ← DTOs, mapper, data sources (HTTP, disk, assets), repository impl
  sdk/       ← public facade (Gitrad), config, error, event, Compose helpers
```

## Running tests

```bash
./gradlew :sdk:test
```
