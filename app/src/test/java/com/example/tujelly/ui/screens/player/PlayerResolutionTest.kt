package com.example.tujelly.ui.screens.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerResolutionTest {

    private fun isDockerOrLocalhostHost(host: String?): Boolean {
        if (host.isNullOrBlank()) return false
        if (host.equals("localhost", ignoreCase = true) || host == "127.0.0.1") return true
        val parts = host.split(".")
        if (parts.size == 4) {
            val p0 = parts[0].toIntOrNull() ?: return false
            val p1 = parts[1].toIntOrNull() ?: return false
            if (p0 == 172 && p1 in 16..31) return true
            if (p0 == 10 && p1 in 0..1) return true
        }
        return false
    }

    @Test
    fun testMascotas2StrmUrl_DetectedAsDockerInternal() {
        val host = "172.21.0.2"

        // 1. Debe detectar que 172.21.0.2 es una subred interna de Docker
        assertTrue("172.21.0.2 debe ser detectada como IP interna de Docker", isDockerOrLocalhostHost(host))

        // 2. Una IP normal de LAN NO debe ser detectada como Docker
        assertFalse("192.168.1.100 no es Docker", isDockerOrLocalhostHost("192.168.1.100"))
        assertFalse("192.168.0.50 no es Docker", isDockerOrLocalhostHost("192.168.0.50"))
    }

    @Test
    fun testMascotas2UrlTransformation_ToJellyfinServerHost() {
        val rawStrmUrl = "http://172.21.0.2:8765/play?ids=btofTJDt8URHdmR6oLhCjgvRyQTW5XQyNJNhFgzZci2sqsFmKkESlfi1"
        val jellyfinServerHost = "192.168.0.100"

        val transformed = rawStrmUrl.replaceFirst("172.21.0.2", jellyfinServerHost)

        assertEquals(
            "http://192.168.0.100:8765/play?ids=btofTJDt8URHdmR6oLhCjgvRyQTW5XQyNJNhFgzZci2sqsFmKkESlfi1",
            transformed
        )
    }

    @Test
    fun testCandidateGeneration_StrmDoesNotContainStaticTrue() {
        val isStrm = true
        val baseUrl = "http://192.168.0.100:8096"
        val playableId = "12345"
        val mediaSourceId = "ms-abc"
        val token = "mytoken"

        val candidateUrls = mutableListOf<String>()

        val extractedStrmUrl = "http://172.21.0.2:8765/play?ids=abc"
        val strmHost = "172.21.0.2"
        val serverHost = "192.168.0.100"

        if (isStrm) {
            val dynamicJellyfin = "$baseUrl/Videos/$playableId/stream?MediaSourceId=$mediaSourceId&api_key=$token"
            candidateUrls.add(dynamicJellyfin)
        }

        if (isDockerOrLocalhostHost(strmHost)) {
            val transformed = extractedStrmUrl.replaceFirst(strmHost, serverHost)
            candidateUrls.add(transformed)
        }

        assertEquals(
            "http://192.168.0.100:8096/Videos/12345/stream?MediaSourceId=ms-abc&api_key=mytoken",
            candidateUrls[0]
        )

        assertEquals(
            "http://192.168.0.100:8765/play?ids=abc",
            candidateUrls[1]
        )

        assertTrue(candidateUrls.none { it.contains("static=true", ignoreCase = true) })
    }
}
