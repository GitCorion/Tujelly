package com.example.tujelly.data.medusa

import android.content.Context
import com.example.tujelly.data.remote.NetworkClientFactory
import com.example.tujelly.data.remote.github.GitHubApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/**
 * Carga el cerebro de la Medusa con esta prioridad:
 *   1. Caché en disco (filesDir/medusa_brain.json) — actualizada desde GitHub.
 *   2. Asset embebido (assets/medusa_brain.json) — fallback offline.
 *
 * La descarga runtime reutiliza la infraestructura de GitHub Releases ya
 * existente, de modo que las mejoras colaborativas del cerebro llegan sin
 * necesidad de publicar un APK nuevo.
 */
class MedusaBrainRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val cacheFile: File get() = File(context.filesDir, "medusa_brain.json")

    suspend fun loadBrain(): MedusaBrain? = withContext(Dispatchers.IO) {
        val cached = readCache()
        val embedded = readAsset()
        if (cached != null && embedded != null) {
            if (embedded.brainVersion > cached.brainVersion) {
                // El asset embebido es más reciente: invalidar la caché obsoleta
                runCatching { cacheFile.delete() }
                embedded
            } else {
                cached
            }
        } else {
            cached ?: embedded
        }
    }

    fun embeddedBrainVersion(): Int = readAsset()?.brainVersion ?: 0

    fun cachedBrainVersion(): Int = readCache()?.brainVersion ?: 0

    /**
     * Descarga la última versión del cerebro desde GitHub Releases y la cachea
     * únicamente si es estrictamente superior a la versión local (embebida o en caché).
     */
    suspend fun refreshFromGitHub(
        owner: String = "GitCorion",
        repo: String = "Tujelly"
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val api = NetworkClientFactory.createService("https://api.github.com/", GitHubApiService::class.java)
            val release = api.getLatestRelease(owner, repo)
            val asset = release.assets.firstOrNull { it.name.equals("medusa_brain.json", ignoreCase = true) }
                ?: return@runCatching currentBrainVersion()

            val request = Request.Builder().url(asset.browserDownloadUrl).build()
            OkHttpClient().newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Error descargando cerebro: HTTP ${response.code}")
                val body = response.body?.string() ?: throw Exception("Cerebro vacío")
                val brain = json.decodeFromString(MedusaBrain.serializer(), body)
                if (brain.brainVersion > currentBrainVersion()) {
                    val tmp = File(cacheFile.parentFile, "medusa_brain.tmp")
                    tmp.writeText(body)
                    if (!tmp.renameTo(cacheFile)) {
                        cacheFile.delete()
                        tmp.copyTo(cacheFile, overwrite = true)
                    }
                    brain.brainVersion
                } else {
                    currentBrainVersion()
                }
            }
        }
    }

    private fun readCache(): MedusaBrain? = runCatching {
        if (!cacheFile.exists()) return@runCatching null
        json.decodeFromString(MedusaBrain.serializer(), cacheFile.readText())
    }.getOrNull()

    private fun readAsset(): MedusaBrain? = runCatching {
        val text = context.assets.open("medusa_brain.json").bufferedReader().use { it.readText() }
        json.decodeFromString(MedusaBrain.serializer(), text)
    }.getOrNull()

    private fun currentBrainVersion(): Int = cachedBrainVersion().takeIf { it > 0 } ?: embeddedBrainVersion()
}
