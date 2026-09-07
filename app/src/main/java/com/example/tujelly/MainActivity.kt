package com.example.tujelly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.NonInteractiveSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import kotlinx.coroutines.launch
import com.example.tujelly.ui.screens.medusa.MedusaScreen
import com.example.tujelly.ui.screens.brand.BrandScreen
import com.example.tujelly.ui.screens.detail.DetailScreen
import com.example.tujelly.ui.screens.favorites.FavoritesScreen
import com.example.tujelly.ui.screens.genre.GenreScreen
import com.example.tujelly.ui.screens.home.HomeScreen
import com.example.tujelly.ui.screens.onboarding.PlatformSelectionScreen
import com.example.tujelly.ui.screens.player.PlayerScreen
import com.example.tujelly.ui.screens.search.SearchScreen
import com.example.tujelly.ui.screens.settings.SettingsScreen
import com.example.tujelly.ui.theme.TujellyTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val imageLoader = coil.ImageLoader.Builder(this)
            .okHttpClient { com.example.tujelly.data.remote.NetworkClientFactory.okHttpClient }
            .crossfade(true)
            .build()
        coil.Coil.setImageLoader(imageLoader)

        setContent {
            val userPrefsRepo = remember { com.example.tujelly.data.local.UserPreferencesRepository(applicationContext) }
            val currentPrefs by userPrefsRepo.userPreferencesFlow.collectAsState(initial = null)

            val prefs = currentPrefs
            if (prefs == null) {
                // Fondo neutro mientras se cargan las preferencias de almacenamiento para evitar el parpadeo de onboarding
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0C0C12))
                )
                return@setContent
            }

            val isMonochrome = prefs.isMonochrome
            val indicatorTheme = if (isMonochrome) com.example.tujelly.data.local.INDICATOR_THEME_MONOCHROME else prefs.indicatorTheme
            val platformLogoStyle = if (isMonochrome) com.example.tujelly.data.local.PLATFORM_LOGO_MONOCHROME else prefs.platformLogoStyle

            val accentColor = if (isMonochrome) com.example.tujelly.data.local.ACCENT_WHITE else prefs.accentColor

            TujellyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape,
                    colors = NonInteractiveSurfaceDefaults.colors(
                        containerColor = Color(0xFF0C0C12)
                    )
                ) {
                    androidx.compose.runtime.CompositionLocalProvider(
                        com.example.tujelly.ui.theme.LocalIsMonochromeTheme provides isMonochrome,
                        com.example.tujelly.ui.theme.LocalIndicatorTheme provides indicatorTheme,
                        com.example.tujelly.ui.theme.LocalPlatformLogoStyle provides platformLogoStyle,
                        com.example.tujelly.ui.theme.LocalAccentColor provides accentColor
                    ) {
                        TujellyApp(startOnboarding = !prefs.hasCompletedOnboarding)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TujellyApp(startOnboarding: Boolean = false) {
    val navController = rememberNavController()
    val startDestination = if (startOnboarding) "onboarding" else "home"

    val context = androidx.compose.ui.platform.LocalContext.current
    val appUpdateManager = remember { com.example.tujelly.data.remote.github.AppUpdateManager(context.applicationContext) }
    val updateInfo by com.example.tujelly.data.remote.github.AppUpdateManager.updateInfo.collectAsState()
    val isDownloading by com.example.tujelly.data.remote.github.AppUpdateManager.isDownloading.collectAsState()
    val downloadProgress by com.example.tujelly.data.remote.github.AppUpdateManager.downloadProgress.collectAsState()

    var showUpdateBanner by remember { mutableStateOf(false) }
    var updateDismissed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        appUpdateManager.checkAutomatically()
    }

    LaunchedEffect(updateInfo) {
        if (updateInfo?.hasUpdate == true && !updateDismissed) {
            showUpdateBanner = true
        }
    }

    val currentEntry by navController.currentBackStackEntryAsState()
    val isPlayerActive = currentEntry?.destination?.route?.startsWith("player") == true

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
        composable("onboarding") {
            PlatformSelectionScreen(
                onDone = {
                    navController.navigate("home") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            HomeScreen(
                onPlayMedia = { itemId ->
                    navController.navigate("player/$itemId")
                },
                onDetailMedia = { itemId ->
                    navController.navigate("detail/$itemId")
                },
                onOpenSettings = {
                    navController.navigate("settings")
                },
                onOpenSearch = {
                    navController.navigate("search")
                },
                onOpenFavorites = {
                    navController.navigate("favorites")
                },
                onOpenBrand = { brandId ->
                    navController.navigate("brand/$brandId")
                },
                onOpenGenre = { genreName ->
                    navController.navigate("genre/$genreName")
                },
                onOpenMedusa = {
                    navController.navigate("medusa")
                }
            )
        }

        composable("medusa") {
            MedusaScreen(
                onNavigateHome = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onOpenFavorites = {
                    navController.navigate("favorites")
                },
                onOpenSearch = {
                    navController.navigate("search")
                },
                onOpenSettings = {
                    navController.navigate("settings")
                },
                onPlayMedia = { itemId ->
                    navController.navigate("player/$itemId")
                },
                onDetailMedia = { itemId ->
                    navController.navigate("detail/$itemId")
                }
            )
        }

        composable("favorites") {
            FavoritesScreen(
                onBack = {
                    navController.popBackStack()
                },
                onPlayMedia = { itemId ->
                    navController.navigate("player/$itemId")
                },
                onDetailMedia = { itemId ->
                    navController.navigate("detail/$itemId")
                },
                onNavigateHome = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onOpenMedusa = {
                    navController.navigate("medusa")
                },
                onOpenSearch = {
                    navController.navigate("search")
                },
                onOpenSettings = {
                    navController.navigate("settings")
                }
            )
        }

        composable("search") {
            SearchScreen(
                onBack = {
                    navController.popBackStack()
                },
                onPlayMedia = { itemId ->
                    navController.navigate("player/$itemId")
                },
                onDetailMedia = { itemId ->
                    navController.navigate("detail/$itemId")
                }
            )
        }

        composable(
            route = "brand/{brandId}",
            arguments = listOf(navArgument("brandId") { type = NavType.StringType })
        ) { backStackEntry ->
            val brandId = backStackEntry.arguments?.getString("brandId") ?: "netflix"
            BrandScreen(
                brandId = brandId,
                onBack = {
                    navController.popBackStack()
                },
                onPlayMedia = { itemId ->
                    navController.navigate("player/$itemId")
                },
                onDetailMedia = { itemId ->
                    navController.navigate("detail/$itemId")
                },
                onNavigateHome = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onOpenMedusa = {
                    navController.navigate("medusa")
                },
                onOpenFavorites = {
                    navController.navigate("favorites")
                },
                onOpenSearch = {
                    navController.navigate("search")
                },
                onOpenSettings = {
                    navController.navigate("settings")
                }
            )
        }

        composable(
            route = "genre/{genreName}",
            arguments = listOf(navArgument("genreName") { type = NavType.StringType })
        ) { backStackEntry ->
            val genreName = backStackEntry.arguments?.getString("genreName") ?: "Acción"
            GenreScreen(
                genreName = genreName,
                onBack = {
                    navController.popBackStack()
                },
                onPlayMedia = { itemId ->
                    navController.navigate("player/$itemId")
                },
                onDetailMedia = { itemId ->
                    navController.navigate("detail/$itemId")
                },
                onNavigateHome = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onOpenMedusa = {
                    navController.navigate("medusa")
                },
                onOpenFavorites = {
                    navController.navigate("favorites")
                },
                onOpenSearch = {
                    navController.navigate("search")
                },
                onOpenSettings = {
                    navController.navigate("settings")
                }
            )
        }

        composable(
            route = "detail/{itemId}",
            arguments = listOf(navArgument("itemId") { type = NavType.StringType })
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
            DetailScreen(
                itemId = itemId,
                onPlay = { id ->
                    navController.navigate("player/$id")
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "player/{itemId}",
            arguments = listOf(navArgument("itemId") { type = NavType.StringType })
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
            PlayerScreen(
                itemId = itemId,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("settings") {
            SettingsScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }

    if (showUpdateBanner && updateInfo?.hasUpdate == true && !isPlayerActive && !updateDismissed) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC000000))
                .padding(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(560.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xF50D111A))
                    .border(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(Color(0x8000E5FF), Color(0x30C084FC))
                        ),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(28.dp)
            ) {
                val coroutineScope = rememberCoroutineScope()
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color(0xFF10B981), androidx.compose.foundation.shape.CircleShape)
                        )
                        Text(
                            text = "ACTUALIZACIÓN DISPONIBLE",
                            color = Color(0xFF00E5FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Nueva versión ${updateInfo?.latestVersion}",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    val notes = updateInfo?.releaseNotes
                    if (!notes.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 140.dp)
                                .background(Color(0x22FFFFFF), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = notes,
                                color = Color(0xFFCBD5E1),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    if (isDownloading) {
                        Spacer(modifier = Modifier.height(16.dp))
                        val progress = downloadProgress ?: 0f
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF00E5FF),
                            trackColor = Color(0x30FFFFFF)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Descargando actualización: ${(progress * 100).toInt()}%",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.tv.material3.Button(
                            onClick = {
                                val apkUrl = updateInfo?.apkUrl
                                if (apkUrl != null) {
                                    coroutineScope.launch {
                                        appUpdateManager.downloadAndInstall(apkUrl)
                                    }
                                }
                            },
                            enabled = !isDownloading,
                            colors = androidx.tv.material3.ButtonDefaults.colors(
                                containerColor = Color(0xFF00E5FF),
                                contentColor = Color(0xFF05070B),
                                focusedContainerColor = Color.White,
                                focusedContentColor = Color.Black
                            ),
                            shape = androidx.tv.material3.ButtonDefaults.shape(RoundedCornerShape(12.dp))
                        ) {
                            Text(
                                text = if (isDownloading) "Descargando..." else "Actualizar ahora",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        androidx.tv.material3.Button(
                            onClick = {
                                showUpdateBanner = false
                                updateDismissed = true
                            },
                            enabled = !isDownloading,
                            colors = androidx.tv.material3.ButtonDefaults.colors(
                                containerColor = Color(0x18FFFFFF),
                                contentColor = Color(0xFFCBD5E1),
                                focusedContainerColor = Color(0x40FFFFFF),
                                focusedContentColor = Color.White
                            ),
                            shape = androidx.tv.material3.ButtonDefaults.shape(RoundedCornerShape(12.dp))
                        ) {
                            Text(
                                text = "Más tarde",
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
}
