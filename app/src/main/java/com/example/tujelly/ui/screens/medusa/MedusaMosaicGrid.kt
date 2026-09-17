package com.example.tujelly.ui.screens.medusa

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp

/**
 * Muro Mosaico TV ultra-fluido que organiza las cápsulas temáticas
 * en una cuadrícula equilibrada adaptativa para pantalla 16:9.
 */
@Composable
fun MedusaMosaicGrid(
    tags: List<MedusaMosaicTag>,
    onTagClick: (MedusaMosaicTag) -> Unit,
    modifier: Modifier = Modifier,
    firstChipFocusRequester: FocusRequester? = null,
    isMonochrome: Boolean = false,
    onTagFocused: (MedusaMosaicTag) -> Unit = {}
) {
    val gridState = rememberLazyGridState()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 185.dp),
        state = gridState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 40.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(
            items = tags,
            key = { _, tag -> tag.id }
        ) { index, tag ->
            val chipModifier = if (index == 0 && firstChipFocusRequester != null) {
                Modifier.focusRequester(firstChipFocusRequester)
            } else {
                Modifier
            }

            MedusaMosaicChip(
                tag = tag,
                onClick = { onTagClick(tag) },
                modifier = chipModifier,
                isMonochrome = isMonochrome,
                onFocusChange = { focused ->
                    if (focused) onTagFocused(tag)
                }
            )
        }
    }
}
