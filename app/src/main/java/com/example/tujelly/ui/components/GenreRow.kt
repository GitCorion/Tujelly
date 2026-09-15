package com.example.tujelly.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun GenreRow(
    genres: List<String>,
    onSelectGenre: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (genres.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        val isMonochrome = com.example.tujelly.ui.theme.LocalIsMonochromeTheme.current
        val accentKey = com.example.tujelly.ui.theme.LocalAccentColor.current
        val accentColor = com.example.tujelly.ui.theme.TvAccent.getColor(accentKey)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 48.dp, vertical = 6.dp)
        ) {
            if (!isMonochrome) {
                Box(
                    modifier = Modifier
                        .width(3.5.dp)
                        .height(18.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                listOf(
                                    Color(0xFF00E5FF),
                                    Color(0xFFA775F8)
                                )
                            ),
                            shape = RoundedCornerShape(2.dp)
                        )
                )
                Spacer(modifier = Modifier.width(10.dp))
            }

            if (isMonochrome) {
                Text(
                    text = "Tus Géneros Favoritos",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = "Tus Géneros Favoritos",
                    style = MaterialTheme.typography.titleLarge.copy(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            listOf(
                                Color(0xFFFFFFFF),
                                Color(0xFFE0F7FE),
                                Color(0xFFA775F8).copy(alpha = 0.9f)
                            )
                        )
                    ),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(genres) { genre ->
                GenreChip(
                    genreName = genre,
                    onClick = { onSelectGenre(genre) }
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun GenreChip(
    genreName: String,
    onClick: () -> Unit
) {
    val isMonochrome = com.example.tujelly.ui.theme.LocalIsMonochromeTheme.current
    val accentKey = com.example.tujelly.ui.theme.LocalAccentColor.current
    val accentColor = com.example.tujelly.ui.theme.TvAccent.getColor(accentKey)

    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isMonochrome) Color(0x10FFFFFF) else Color(0x1400A4DC),
            focusedContainerColor = if (isMonochrome) Color(0x28FFFFFF) else accentColor.copy(alpha = 0.25f)
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(border = BorderStroke(0.75.dp, if (isMonochrome) Color(0x18FFFFFF) else accentColor.copy(alpha = 0.25f))),
            focusedBorder = Border(border = BorderStroke(2.dp, if (isMonochrome) Color.White else accentColor))
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.0f)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = genreName,
                color = if (isMonochrome) Color.White else Color(0xFFF1F5F9),
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}
