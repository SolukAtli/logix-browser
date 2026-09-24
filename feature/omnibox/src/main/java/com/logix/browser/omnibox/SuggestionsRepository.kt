package com.logix.browser.omnibox

import com.logix.browser.search.SearchEngine
import com.logix.browser.search.SearchEngineId
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

/**
 * Arama önerileri: motorun suggest servisine sorar, liste döner.
 * Ağ yoksa/bozuksa boş liste — öneriler opsiyoneldir, aramayı engellemez.
 */
@Singleton
class SuggestionsRepository @Inject constructor() {

    suspend fun fetch(engine: SearchEngine, query: String): List<String> {
        if (query.isBlank()) return emptyList()
        val template = engine.buildSuggestUrl(query) ?: return emptyList()
        return withContext(Dispatchers.IO) {
            runCatching {
                val conn = (URL(template).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 6_000
                    readTimeout = 6_000
                    setRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36",
                    )
                }
                try {
                    if (conn.responseCode != HttpURLConnection.HTTP_OK) return@runCatching emptyList()
                    parse(engine.id, conn.inputStream.bufferedReader().readText())
                } finally {
                    conn.disconnect()
                }
            }.getOrElse { emptyList() }
        }
    }

    private fun parse(id: SearchEngineId, body: String): List<String> = runCatching {
        when (id) {
            SearchEngineId.DUCKDUCKGO -> {
                val arr = JSONArray(body)
                (0 until arr.length())
                    .mapNotNull { arr.optJSONObject(it)?.optString("phrase")?.takeIf { s -> s.isNotBlank() } }
            }
            else -> {
                val arr = JSONArray(body)
                val suggs = arr.optJSONArray(1) ?: return emptyList()
                (0 until suggs.length())
                    .mapNotNull { suggs.optString(it).takeIf { s -> s.isNotBlank() } }
            }
        }
    }.getOrElse { emptyList() }
}
