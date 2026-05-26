package de.varakh.subsd.data.prefs

import android.content.Context
import android.os.Build
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "subsd_prefs")

data class Config(
    /** Base HTTP URL of the subsd daemon, e.g. "http://192.168.1.100:8080". Mandatory. */
    val httpUrl: String = "",
    /** Satellite name shown in the daemon UI. Defaults to device model name. */
    val deviceName: String = "",
    /** gRPC address "host:port". Blank → derived from httpUrl host + port 9090. */
    val grpcAddr: String = "",
    /** Shared secret sent as x-subsd-token gRPC metadata. Optional. */
    val grpcToken: String = "",
    /** Use TLS for the gRPC connection. */
    val grpcTls: Boolean = false,
    /** HTTP API token for POST /login. Optional; leave blank if auth is disabled. */
    val httpToken: String = "",
    /** Whether to act as a satellite (local audio playback via gRPC). False = remote-only. */
    val satelliteEnabled: Boolean = true
) {
    val isConfigured: Boolean get() = httpUrl.isNotBlank()

    /** Resolved gRPC address: explicit grpcAddr or host from httpUrl + :9090. */
    val resolvedGrpcAddr: String
        get() {
            if (grpcAddr.isNotBlank()) return grpcAddr
            return try {
                val host = java.net.URL(httpUrl).host
                if (host.isNullOrBlank()) ":9090" else "$host:9090"
            } catch (_: Exception) {
                ":9090"
            }
        }

    /** Resolved device name: explicit or derived from Build.MODEL. */
    val resolvedDeviceName: String
        get() {
            if (deviceName.isNotBlank()) return deviceName
            return "android-${Build.MODEL}".replace(" ", "-").lowercase()
        }
}

class Prefs(context: Context) {
    private val store = context.applicationContext.dataStore

    companion object {
        val HTTP_URL = stringPreferencesKey("http_url")
        val DEVICE_NAME = stringPreferencesKey("device_name")
        val GRPC_ADDR = stringPreferencesKey("grpc_addr")
        val GRPC_TOKEN = stringPreferencesKey("grpc_token")
        val GRPC_TLS = booleanPreferencesKey("grpc_tls")
        val HTTP_TOKEN = stringPreferencesKey("http_token")
        val SATELLITE_ENABLED = booleanPreferencesKey("satellite_enabled")
    }

    val config: Flow<Config> = store.data.map { p ->
        Config(
            httpUrl = p[HTTP_URL] ?: "",
            deviceName = p[DEVICE_NAME] ?: "",
            grpcAddr = p[GRPC_ADDR] ?: "",
            grpcToken = p[GRPC_TOKEN] ?: "",
            grpcTls = p[GRPC_TLS] ?: false,
            httpToken = p[HTTP_TOKEN] ?: "",
            satelliteEnabled = p[SATELLITE_ENABLED] ?: true
        )
    }

    suspend fun save(config: Config) {
        store.edit { p ->
            p[HTTP_URL] = config.httpUrl
            p[DEVICE_NAME] = config.deviceName
            p[GRPC_ADDR] = config.grpcAddr
            p[GRPC_TOKEN] = config.grpcToken
            p[GRPC_TLS] = config.grpcTls
            p[HTTP_TOKEN] = config.httpToken
            p[SATELLITE_ENABLED] = config.satelliteEnabled
        }
    }
}
