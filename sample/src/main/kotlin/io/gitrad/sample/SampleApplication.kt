package io.gitrad.sample

import android.app.Application
import android.util.Log
import io.gitrad.sdk.sdk.Gitrad
import io.gitrad.sdk.sdk.GitradEvent
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class SampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Gitrad.configure(
            context = this,
            apiKey = "z_xi-4hL3tojn7zldQqxN4BQ61UcCuFENb8CsAPosuQ",
            baseUrl = "https://gitrad--git18n.europe-west4.hosted.app",
            maxCacheAge = 3600L,
        )

        Gitrad.onEvent { event ->
            when (event) {
                GitradEvent.FetchStarted -> Log.d("Gitrad", "Fetch started")
                is GitradEvent.FetchSucceeded -> Log.d("Gitrad", "Fetched ${event.languages} languages in ${event.ms}ms")
                is GitradEvent.FetchFailed -> Log.e("Gitrad", "Fetch failed: ${event.error}")
                GitradEvent.CacheHit -> Log.d("Gitrad", "Loaded from cache")
                GitradEvent.BundleFallback -> Log.d("Gitrad", "Using bundled baseline")
            }
        }

        MainScope().launch { Gitrad.prefetch() }
    }
}
