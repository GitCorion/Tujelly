package com.example.tujelly.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.example.tujelly.R

data class BrandTile(
    val id: String,
    val name: String,
    val iconRes: Int,
    val iconMonoRes: Int = iconRes
)

val BRAND_TILES = listOf(
    BrandTile("netflix", "NETFLIX", R.drawable.ic_brand_netflix, R.drawable.ic_brand_netflix_mono),
    BrandTile("disney", "DISNEY+", R.drawable.ic_brand_disney, R.drawable.ic_brand_disney_mono),
    BrandTile("max", "MAX", R.drawable.ic_brand_max, R.drawable.ic_brand_max_mono),
    BrandTile("prime", "PRIME VIDEO", R.drawable.ic_brand_prime, R.drawable.ic_brand_prime_mono),
    BrandTile("apple", "APPLE TV+", R.drawable.ic_brand_apple, R.drawable.ic_brand_apple_mono),
    BrandTile("movistar", "MOVISTAR+", R.drawable.ic_brand_movistar, R.drawable.ic_brand_movistar_mono)
)

@Composable
fun BrandTileRow(
    onSelectBrand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            items(BRAND_TILES, key = { it.id }) { brand ->
                BrandCard(
                    brand = brand,
                    onClick = { onSelectBrand(brand.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun BrandCard(
    brand: BrandTile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val logoStyle = com.example.tujelly.ui.theme.LocalPlatformLogoStyle.current
    val indicatorTheme = com.example.tujelly.ui.theme.LocalIndicatorTheme.current
    val isMonochrome = logoStyle == com.example.tujelly.data.local.PLATFORM_LOGO_MONOCHROME ||
            (logoStyle == com.example.tujelly.data.local.PLATFORM_LOGO_COLOR && indicatorTheme == com.example.tujelly.data.local.INDICATOR_THEME_MONOCHROME)

    val iconRes = if (isMonochrome) brand.iconMonoRes else brand.iconRes

    Card(
        onClick = onClick,
        colors = CardDefaults.colors(
            containerColor = if (isFocused) Color(0xFF1E222D) else Color(0xFF111319),
            focusedContainerColor = Color(0xFF1E222D)
        ),
        scale = CardDefaults.scale(focusedScale = 1.04f),
        modifier = modifier
            .width(132.dp)
            .height(64.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = if (isFocused) 2.dp else 0.75.dp,
                color = if (isFocused) Color.White else Color(0x18FFFFFF),
                shape = RoundedCornerShape(10.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isFocused) Color(0xFF1E222D) else Color(0xFF111319))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = brand.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
