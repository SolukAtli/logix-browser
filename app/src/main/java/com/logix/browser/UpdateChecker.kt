package com.logix.browser

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** GitHub Releases üzerinden uygulama-içi güncelleme denetimi. */
object UpdateChecker {

    data class Release(
        val version: String,
        val notes: String,
        val downloadUrl: String,
    )

    suspend fun latest(): Release? = withContext(Dispatchers.IO) {
        val url = URL("https://api.github.com/repos/SolukAtli/logix-browser/releases/latest")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/vnd.github+json")
        }
        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
            val json = JSONObject(conn.inputStream.bufferedReader().readText())
            val tag = json.optString("tag_name").trim().removePrefix("v")
            if (tag.isBlank()) return@withContext null
            val assets = json.optJSONArray("assets")
            var apkUrl = ""
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url")
                        break
                    }
                }
            }
            Release(
                version = tag,
                notes = json.optString("body"),
                downloadUrl = apkUrl.ifBlank { json.optString("html_url") },
            )
        } catch (e: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    /** "1.2" > "1.1" gibi sayısal sürüm karşılaştırması. */
    fun isNewer(latest: String, current: String): Boolean {
        val l = latest.split(".", "-").mapNotNull { it.toIntOrNull() }
        val c = current.split(".", "-").mapNotNull { it.toIntOrNull() }
        val size = maxOf(l.size, c.size)
        for (i in 0 until size) {
            val diff = (l.getOrElse(i) { 0 }) - (c.getOrElse(i) { 0 })
            if (diff != 0) return diff > 0
        }
        return false
    }
}

sealed interface UpdateStatus {
    data object Idle : UpdateStatus
    data object Checking : UpdateStatus
    data object Latest : UpdateStatus
    data class Available(
        val version: String,
        val notes: String,
        val downloadUrl: String,
    ) : UpdateStatus
    data class Failed(val reason: String) : UpdateStatus
}
