package com.example.tujelly.data.remote

import com.example.tujelly.util.DeviceUtils
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object NetworkClientFactory {

    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Google TV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "application/json, text/plain, */*")
                .build()
            chain.proceed(request)
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    fun normalizeUrl(rawUrl: String): String {
        var url = rawUrl.trim()
        if (url.isEmpty()) return ""

        // Strip fragments (#...) and query parameters (?...)
        url = url.substringBefore("#").substringBefore("?")

        // Determine if it looks like an IP or localhost
        val withoutProto = url.substringAfter("://")
        val hostPart = withoutProto.substringBefore("/").substringBefore(":")
        val isIpOrLocalhost = hostPart.equals("localhost", ignoreCase = true)
                || hostPart.startsWith("127.0.0.1")
                || hostPart.startsWith("10.0.2.2")
                || hostPart.matches(Regex("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$"))

        // If scheme is missing:
        // Domains (e.g. jellyfin.example.com) default to https://
        // Local IPs/localhost default to http://
        if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
            url = if (isIpOrLocalhost) "http://$url" else "https://$url"
        }

        url = url.trimEnd('/')

        // Strip web interface paths if user copied URL from browser (e.g. /web, /web/index.html)
        url = url.replace(Regex("/web(/index\\.html)?/?$", RegexOption.IGNORE_CASE), "")
        url = url.trimEnd('/')

        val scheme = if (url.startsWith("https://", ignoreCase = true)) "https://" else "http://"
        var rest = url.substring(scheme.length)

        // If on emulator and user entered localhost or 127.0.0.1, auto-map to 10.0.2.2 (host machine)
        if (DeviceUtils.isEmulator()) {
            if (rest.startsWith("localhost", ignoreCase = true)) {
                rest = "10.0.2.2" + rest.substring("localhost".length)
            } else if (rest.startsWith("127.0.0.1")) {
                rest = "10.0.2.2" + rest.substring("127.0.0.1".length)
            }
        }

        // Only append port 8096 for local HTTP connections without a port (e.g. 192.168.1.50 or 10.0.2.2)
        // Never append port 8096 for HTTPS or external domain names (like jellyfin.example.com)
        val currentHost = rest.substringBefore("/").substringBefore(":")
        val isLocalTarget = currentHost.equals("localhost", ignoreCase = true)
                || currentHost.startsWith("10.0.2.2")
                || currentHost.startsWith("127.0.0.1")
                || currentHost.matches(Regex("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$"))

        if (scheme == "http://" && isLocalTarget && !rest.contains(":")) {
            val h = rest.substringBefore("/")
            val p = if (rest.contains("/")) "/" + rest.substringAfter("/") else ""
            rest = "$h:8096$p"
        }

        return "$scheme$rest"
    }

    fun <T> createService(baseUrl: String, serviceClass: Class<T>): T {
        val cleanUrl = normalizeUrl(baseUrl)
        val validUrl = if (cleanUrl.endsWith("/")) cleanUrl else "$cleanUrl/"
        val contentType = "application/json".toMediaType()

        return Retrofit.Builder()
            .baseUrl(validUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(serviceClass)
    }
}
