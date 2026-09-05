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
    val iconRes: Int
)

val BRAND_TILES = listOf(
    BrandTile("netflix", "NETFLIX", R.drawable.ic_brand_netflix),
    BrandTile("disney", "DISNEY+", R.drawable.ic_brand_disney),
    BrandTile("max", "MAX", R.drawable.ic_brand_max),
    BrandTile("prime", "PRIME VIDEO", R.drawable.ic_brand_prime),
    BrandTile("apple", "APPLE TV+", R.drawable.ic_brand_apple)
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
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
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

    Card(
        onClick = onClick,
        colors = CardDefaults.colors(
            containerColor = if (isFocused) Color(0xFF1E222D) else Color(0xFF111319),
            focusedContainerColor = Color(0xFF1E222D)
        ),
        scale = CardDefaults.scale(focusedScale = 1.0f),
        modifier = modifier
            .width(145.dp)
            .height(68.dp)
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = brand.iconRes),
                contentDescription = brand.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
