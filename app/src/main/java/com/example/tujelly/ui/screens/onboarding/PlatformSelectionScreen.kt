package com.example.tujelly.ui.screens.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.example.tujelly.R
import com.example.tujelly.data.model.StreamPlatform
import com.example.tujelly.data.model.SUPPORTED_PLATFORMS

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlatformSelectionScreen(
    onDone: () -> Unit,
    viewModel: PlatformSelectionViewModel = viewModel()
) {
    val selected by viewModel.selected.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D0E17),
                        Color(0xFF07080E)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 28.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_tujelly_header),
                    contentDescription = "TuJelly",
                    modifier = Modifier.height(34.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { viewModel.selectAll() },
                        colors = ButtonDefaults.colors(
                            containerColor = Color(0x1AFFFFFF),
                            contentColor = Color.White,
                            focusedContainerColor = Color(0x33FFFFFF),
                            focusedContentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("Marcar todas", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = { viewModel.deselectAll() },
                        colors = ButtonDefaults.colors(
                            containerColor = Color(0x1AFFFFFF),
                            contentColor = Color(0xFF94A3B8),
                            focusedContainerColor = Color(0x33FFFFFF),
                            focusedContentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("Desmarcar todas", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Elige tus plataformas",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Selecciona las plataformas que usas. Sus novedades y tendencias se cruzarán con tu biblioteca Jellyfin.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Grid of platforms taking weight(1f)
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(SUPPORTED_PLATFORMS, key = { it.id }) { platform ->
                    PlatformTileCard(
                        platform = platform,
                        isSelected = platform.id in selected,
                        onToggle = { viewModel.togglePlatform(platform.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Sticky Bottom Confirmation Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF131724))
                    .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(16.dp))
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Configuración de inicio",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${selected.size} de ${SUPPORTED_PLATFORMS.size} plataformas activas",
                        fontSize = 14.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = { viewModel.onContinue(onDone) },
                    colors = ButtonDefaults.colors(
                        containerColor = Color(0xFF4F46E5),
                        contentColor = Color.White,
                        focusedContainerColor = Color(0xFF6366F1),
                        focusedContentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 28.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Aceptar y continuar",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PlatformTileCard(
    platform: StreamPlatform,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val logoStyle = com.example.tujelly.ui.theme.LocalPlatformLogoStyle.current
    val indicatorTheme = com.example.tujelly.ui.theme.LocalIndicatorTheme.current
    val isMonochrome = com.example.tujelly.ui.theme.LocalIsMonochromeTheme.current ||
            logoStyle == com.example.tujelly.data.local.PLATFORM_LOGO_MONOCHROME ||
            (logoStyle == com.example.tujelly.data.local.PLATFORM_LOGO_COLOR && indicatorTheme == com.example.tujelly.data.local.INDICATOR_THEME_MONOCHROME)

    val iconRes = if (isMonochrome) platform.iconMonoRes else platform.iconRes

    Card(
        onClick = onToggle,
        colors = CardDefaults.colors(
            containerColor = if (isSelected) Color(0xFF141C2E) else Color(0xFF0F111A),
            focusedContainerColor = Color(0xFF1E283D)
        ),
        scale = CardDefaults.scale(focusedScale = 1.04f),
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = when {
                    isFocused -> Color.White
                    isSelected -> Color(0xFF4F46E5)
                    else -> Color(0x14FFFFFF)
                },
                shape = RoundedCornerShape(14.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = platform.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Checkmark indicator chip
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) Color(0xFF4F46E5) else Color(0x10FFFFFF)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) Color(0xFF6366F1) else Color(0x30FFFFFF),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Text(
                        text = "✓",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
