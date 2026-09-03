package com.yomixhub.android

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.yomixhub.android.data.AppState
import com.yomixhub.android.data.AuthRepository
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * App-level wiring. Provides a Coil [ImageLoader] with sane timeouts
 * (Referer/User-Agent headers for source images are attached per request –
 * see [com.yomixhub.android.ui.components.CoverImage]), restores the
 * persisted downloads/bookmarks from DataStore on startup and starts the
 * Firebase auth listener (which in turn drives bookmark cloud sync).
 */
class YomixApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        AppState.attach(this)
        // After AppState so cloud sync awaits the local bookmark restore.
        AuthRepository.attach(this)
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .crossfade(true)
            .okHttpClient(::imageHttpClient)
            .build()

    private fun imageHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
}
