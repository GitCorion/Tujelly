package com.example.tujelly.data.remote.github

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.tujelly.data.remote.NetworkClientFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.http.GET
import retrofit2.http.Path
import java.io.File
import java.io.FileOutputStream

@Serializable
data class GitHubAssetDto(
    @SerialName("name") val name: String = "",
    @SerialName("browser_download_url") val browserDownloadUrl: String = "",
    @SerialName("size") val size: Long = 0L
)

@Serializable
data class GitHubReleaseDto(
    @SerialName("tag_name") val tagName: String = "",
    @SerialName("name") val name: String? = null,
    @SerialName("body") val body: String? = null,
    @SerialName("assets") val assets: List<GitHubAssetDto> = emptyList()
)

interface GitHubApiService {
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): GitHubReleaseDto
}

data class UpdateInfo(
    val hasUpdate: Boolean,
    val currentVersion: String,
    val latestVersion: String,
    val releaseNotes: String?,
    val apkUrl: String?
)

class AppUpdateManager(private val context: Context) {

    private val api: GitHubApiService = NetworkClientFactory.createService("https://api.github.com/", GitHubApiService::class.java)

    companion object {
        private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
        val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

        private val _downloadProgress = MutableStateFlow<Float?>(null)
        val downloadProgress: StateFlow<Float?> = _downloadProgress.asStateFlow()

        private val _isDownloading = MutableStateFlow(false)
        val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

        private var hasAutoChecked = false
    }

    suspend fun checkAutomatically(owner: String = "GitCorion", repo: String = "Tujelly") {
        if (hasAutoChecked) return
        hasAutoChecked = true
        checkForUpdates(owner, repo)
    }

    suspend fun checkForUpdates(owner: String = "GitCorion", repo: String = "Tujelly"): Result<UpdateInfo> {
        return runCatching {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val currentVer = pInfo.versionName ?: "1.0"
            val release = api.getLatestRelease(owner, repo)
            val cleanTag = release.tagName.removePrefix("v").trim()
            val cleanCurrent = currentVer.removePrefix("v").trim()

            val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
            val isNewer = isVersionNewer(cleanCurrent, cleanTag)

            val info = UpdateInfo(
                hasUpdate = isNewer && apkAsset != null,
                currentVersion = currentVer,
                latestVersion = release.tagName,
                releaseNotes = release.body,
                apkUrl = apkAsset?.browserDownloadUrl
            )
            _updateInfo.value = info
            info
        }
    }

    private fun isVersionNewer(current: String, latest: String): Boolean {
        if (current == latest) return false
        val curParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val latParts = latest.split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(curParts.size, latParts.size)
        for (i in 0 until maxLen) {
            val c = curParts.getOrElse(i) { 0 }
            val l = latParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    suspend fun downloadAndInstall(apkUrl: String, onProgress: (Float) -> Unit = {}): Result<Unit> {
        _isDownloading.value = true
        _downloadProgress.value = 0f
        return withContext(Dispatchers.IO) {
            runCatching {
                val client = OkHttpClient()
                val request = Request.Builder().url(apkUrl).build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) throw Exception("Error al descargar APK: HTTP ${response.code}")

                val body = response.body ?: throw Exception("Respuesta vacía al descargar APK")
                val totalBytes = body.contentLength()

                val apkFile = File(context.cacheDir, "update.apk")
                if (apkFile.exists()) apkFile.delete()

                body.byteStream().use { input ->
                    FileOutputStream(apkFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var downloaded = 0L
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloaded += bytesRead
                            if (totalBytes > 0) {
                                val prog = downloaded.toFloat() / totalBytes
                                _downloadProgress.value = prog
                                onProgress(prog)
                            }
                        }
                        output.flush()
                    }
                }

                // Iniciar instalación nativa de Android
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(intent)
            }.also {
                _isDownloading.value = false
            }
        }
    }
}
