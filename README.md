# Gitext Android SDK

OTA translation SDK for Android. Downloads translations from the Gitext server, caches them locally, and exposes a simple string-lookup API with Jetpack Compose integration.

## Installation

Add JitPack to your repository list. In `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

In your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.RomainRoche.gitext-android:sdk:1.0.0")
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
        Gitext.configure(
            context     = this,
            apiKey      = BuildConfig.GITEXT_API_KEY,
            baseUrl     = "https://app.gitext.io",
            maxCacheAge = 3600L,           // seconds; 0 = always fetch
        )
        lifecycleScope.launch { Gitext.prefetch() }
    }
}
```

Deliver `GITEXT_API_KEY` via CI build variables, never in source control.

## String lookup

```kotlin
// Simple key
val title = Gitext.string("onboarding.welcome_title")

// With interpolation (SDK returns format string; app does substitution)
val template = Gitext.string("greeting.welcome")      // "Welcome, %s!"
val text     = String.format(template, user.firstName)

// Plural
val badge = Gitext.string("notifications.count", count = unreadCount)
// 0 → "No notifications", 1 → "1 notification", 5 → "5 notifications"
```

## Jetpack Compose integration

```kotlin
@Composable
fun WelcomeScreen() {
    val title = rememberGitextString("onboarding.welcome_title")
    Text(text = title)
}

// With plurals
@Composable
fun BadgeText(count: Int) {
    val text = rememberGitextString("notifications.count", count = count)
    Text(text = text)
}
```

`rememberGitextString` re-composes automatically after a remote refresh.

## Namespace support

For multi-module apps where different modules own different translation namespaces:

```kotlin
// Option 1: configure-level namespace (single namespace per app)
Gitext.configure(context = this, apiKey = key, baseUrl = url, namespace = "app")
Gitext.string("greeting.hello")   // looks up "app.greeting.hello"

// Option 2: scoped accessors (multi-namespace)
val payments = Gitext.scoped("payments")
payments.string("checkout.confirm")   // looks up "payments.checkout.confirm"
```

## Foreground refresh

```kotlin
override fun onResume() {
    super.onResume()
    lifecycleScope.launch { Gitext.refresh() }
}
```

## Background refresh with WorkManager

The SDK ships a `GitextSyncWorker` ready to use. Add WorkManager to your app's dependencies:

```kotlin
implementation("androidx.work:work-runtime-ktx:2.9.0")
```

Then schedule periodic sync in `Application.onCreate()` (after `Gitext.configure()`):

```kotlin
GitextSyncWorker.schedule(context, intervalHours = 12)
```

The worker runs only when the device has a network connection, and retries automatically on transient failures. Auth failures (`Unauthorized`, `SubscriptionInactive`) are treated as permanent and will not retry.

To cancel the scheduled sync:

```kotlin
GitextSyncWorker.cancel(context)
```

## Observability

```kotlin
Gitext.onEvent { event ->
    when (event) {
        GitextEvent.FetchStarted -> analytics.track("gitext_fetch_started")
        is GitextEvent.FetchSucceeded -> analytics.track("gitext_fetch_ok", mapOf("langs" to event.languages, "ms" to event.ms))
        is GitextEvent.FetchFailed -> Crashlytics.recordException(event.error)
        GitextEvent.CacheHit -> Unit
        GitextEvent.BundleFallback -> analytics.track("gitext_bundle_fallback")
    }
}
```

## Bundled baseline

Ship a `translations.json` snapshot with each release so the SDK can serve strings when offline and the disk cache is cold:

1. Place the file at `sdk/src/main/assets/gitext-baseline/translations.json`
2. Update it as part of each release by downloading from `GET /api/ota/download` with your production API key

## Architecture

Clean Architecture — three layers, each depending only inward:

```
sdk/
  domain/    ← entities, repository protocol, use cases, PluralRules (no Android deps)
  data/      ← DTOs, mapper, data sources (HTTP, disk, assets), repository impl
  sdk/       ← public facade (Gitext), config, error, event, Compose helpers
```

## Running tests

```bash
./gradlew :sdk:test
```
