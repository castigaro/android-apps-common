package com.appsonar.common.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Prüft anhand einer kleinen JSON-Datei auf der Website, ob eine neuere
 * Version der App verfügbar ist. Die Datei wird bei jedem CI-Build der
 * jeweiligen App automatisch mitveröffentlicht (siehe deren Build-Workflow).
 *
 * Die App liefert ihre Konfiguration selbst: die URL ihres Manifests und den
 * eigenen versionCode (BuildConfig.VERSION_CODE) — die Bibliothek kennt keine
 * konkrete App.
 */
class UpdateChecker(
    private val versionUrl: String,
    private val currentVersionCode: Int,
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    data class UpdateInfo(val versionName: String, val apkUrl: String)

    /** Liefert Update-Infos, wenn eine neuere Version verfügbar ist, sonst null. */
    suspend fun check(): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(versionUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body?.string() ?: return@use null
                val json = JSONObject(body)
                val remoteCode = json.optInt("versionCode", 0)
                if (remoteCode > currentVersionCode) {
                    UpdateInfo(
                        versionName = json.optString("versionName", "?"),
                        apkUrl = json.optString("apkUrl", "https://appsonar.de/"),
                    )
                } else {
                    null
                }
            }
        }.getOrNull()
    }
}
