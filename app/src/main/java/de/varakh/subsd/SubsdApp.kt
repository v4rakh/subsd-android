package de.varakh.subsd

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import de.varakh.subsd.data.api.SubsdApi
import de.varakh.subsd.data.api.SubsdWebSocket
import de.varakh.subsd.data.prefs.Prefs

class SubsdApp : Application() {
    companion object {
        lateinit var instance: SubsdApp
            private set
    }

    lateinit var prefs: Prefs
        private set
    lateinit var api: SubsdApi
        private set
    lateinit var webSocket: SubsdWebSocket
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = Prefs(this)
        api = SubsdApi("")
        webSocket = SubsdWebSocket(api)

        // Coil uses the same OkHttp client as the API so session cookies are forwarded
        // automatically for cover art requests.
        SingletonImageLoader.setSafe {
            ImageLoader.Builder(this)
                .components { add(OkHttpNetworkFetcherFactory(callFactory = api.client)) }
                .crossfade(true)
                .build()
        }
    }
}
