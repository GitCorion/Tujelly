package com.example.tujelly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.NonInteractiveSurfaceDefaults
import androidx.tv.material3.Surface
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
                        com.example.tujelly.ui.theme.LocalPlatformLogoStyle provides platformLogoStyle
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
}
