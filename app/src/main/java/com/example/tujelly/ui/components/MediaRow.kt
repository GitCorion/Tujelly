package com.example.tujelly.ui.components

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.example.tujelly.domain.model.HomeSection
import com.example.tujelly.domain.model.MediaItem
import com.example.tujelly.ui.theme.TvPill

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MediaRow(
    section: HomeSection,
    onItemClick: (MediaItem) -> Unit,
    onItemFocus: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        val isMonochrome = com.example.tujelly.ui.theme.LocalIsMonochromeTheme.current
        val accentKey = com.example.tujelly.ui.theme.LocalAccentColor.current
        val accentColor = com.example.tujelly.ui.theme.TvAccent.getColor(accentKey)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 48.dp, vertical = 8.dp)
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
                                    accentColor
                                )
                            ),
                            shape = RoundedCornerShape(2.dp)
                        )
                )
                Spacer(modifier = Modifier.width(10.dp))
            }

            if (!section.badge.isNullOrBlank()) {
                TvPill(
                    text = section.badge,
                    containerColor = if (isMonochrome) Color(0x18FFFFFF) else accentColor.copy(alpha = 0.18f),
                    textColor = if (isMonochrome) Color(0xFFE2E8F0) else Color(0xFF7DD3FC),
                    borderColor = if (isMonochrome) Color(0x22FFFFFF) else accentColor.copy(alpha = 0.45f)
                )
                Spacer(modifier = Modifier.width(10.dp))
            }

            if (isMonochrome) {
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            listOf(
                                Color(0xFFFFFFFF),
                                Color(0xFFE0F7FE),
                                Color(0xFFBAE6FD)
                            )
                        )
                    ),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(section.items, key = { _, it -> it.id }) { index, item ->
                val displayItem = if (section.isRanked && item.rank == null) {
                    item.copy(rank = index + 1)
                } else {
                    item
                }
                MediaCard(
                    item = displayItem,
                    onClick = { onItemClick(displayItem) },
                    onFocus = { onItemFocus(displayItem) }
                )
            }
        }
    }
}
