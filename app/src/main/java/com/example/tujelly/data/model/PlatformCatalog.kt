package com.example.tujelly.data.model

import androidx.compose.ui.graphics.Color
import com.example.tujelly.R

/**
 * Única fuente de verdad para las plataformas de streaming soportadas.
 * Lista curada a mano, ordenada por display_priority de TMDB (España),
 * con logos embebidos. No se consulta TMDB en runtime.
 */
data class StreamPlatform(
    val id: String,
    val name: String,
    val providerId: String,
    val iconRes: Int,
    val iconMonoRes: Int,
    val accentColor: Color,
    val gradientStart: Color
)

val SUPPORTED_PLATFORMS: List<StreamPlatform> = listOf(
    StreamPlatform("netflix", "NETFLIX", "8", R.drawable.ic_brand_netflix, R.drawable.ic_brand_netflix_mono, Color(0xFFE50914), Color(0xFF1E0507)),
    StreamPlatform("prime", "PRIME VIDEO", "119", R.drawable.ic_brand_prime, R.drawable.ic_brand_prime_mono, Color(0xFF00A8E1), Color(0xFF02141C)),
    StreamPlatform("disney", "DISNEY+", "337", R.drawable.ic_brand_disney, R.drawable.ic_brand_disney_mono, Color(0xFF1B4DFF), Color(0xFF040D2D)),
    StreamPlatform("apple", "APPLE TV+", "350", R.drawable.ic_brand_apple, R.drawable.ic_brand_apple_mono, Color(0xFFE2E8F0), Color(0xFF101116)),
    StreamPlatform("max", "MAX", "1899|384", R.drawable.ic_brand_max, R.drawable.ic_brand_max_mono, Color(0xFF002BE7), Color(0xFF020A24)),
    StreamPlatform("skyshowtime", "SKYSHOWTIME", "1773", R.drawable.ic_brand_skyshowtime, R.drawable.ic_brand_skyshowtime_mono, Color(0xFF101A78), Color(0xFF03051A)),
    StreamPlatform("movistar", "MOVISTAR+", "2241|149", R.drawable.ic_brand_movistar, R.drawable.ic_brand_movistar_mono, Color(0xFF019DF4), Color(0xFF020B20)),
    StreamPlatform("filmin", "FILMIN", "63", R.drawable.ic_brand_filmin, R.drawable.ic_brand_filmin_mono, Color(0xFF00FFA1), Color(0xFF00241A)),
    StreamPlatform("crunchyroll", "CRUNCHYROLL", "283", R.drawable.ic_brand_crunchyroll, R.drawable.ic_brand_crunchyroll_mono, Color(0xFFF78B25), Color(0xFF1C0D00))
)

fun platformById(id: String): StreamPlatform? = SUPPORTED_PLATFORMS.firstOrNull { it.id.equals(id, ignoreCase = true) }

val DEFAULT_SELECTED_PLATFORMS: Set<String> = SUPPORTED_PLATFORMS.map { it.id }.toSet()
