package io.gitext.sample

import android.app.Application
import android.util.Log
import io.gitext.sdk.sdk.Gitext
import io.gitext.sdk.sdk.GitextEvent
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class SampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Gitext.configure(
            context = this,
            apiKey = BuildConfig.GITEXT_API_KEY,
            baseUrl = BuildConfig.GITEXT_BASE_URL,
            maxCacheAge = 3600L,
        )

        Gitext.onEvent { event ->
            when (event) {
                GitextEvent.FetchStarted -> Log.d("Gitext", "Fetch started")
                is GitextEvent.FetchSucceeded -> Log.d("Gitext", "Fetched ${event.languages} languages in ${event.ms}ms")
                is GitextEvent.FetchFailed -> Log.e("Gitext", "Fetch failed: ${event.error}")
                GitextEvent.CacheHit -> Log.d("Gitext", "Loaded from cache")
                GitextEvent.BundleFallback -> Log.d("Gitext", "Using bundled baseline")
            }
        }

        MainScope().launch { Gitext.prefetch() }
    }
}
