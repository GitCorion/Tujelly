package com.example.tujelly.ui.screens.search

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.SpaceBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text

import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvIntegratedKeyboard(
    validNextChars: Set<Char>,
    isPredictiveActive: Boolean,
    query: String,
    focusColor: Color,
    focusContentColor: Color,
    onKeyClick: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    initialFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier
) {
    val keyboardRows = remember { KeyboardDirectionResolver.KEYBOARD_ROWS }
    val allChars = remember { KeyboardDirectionResolver.ALL_CHARS }

    // Requesters para cada tecla individual
    val keyFocusRequesters = remember {
        allChars.associateWith { FocusRequester() }
    }

    // Requesters para botones de acción inferiores
    val spaceFocusRequester = remember { FocusRequester() }
    val backspaceFocusRequester = remember { FocusRequester() }
    val clearFocusRequester = remember { FocusRequester() }

    var currentFocusedChar by remember { mutableStateOf<Char?>('A') }

    // Si no hay restricciones locales calculadas (ej: catálogo vacío o búsqueda remota),
    // no bloqueamos al usuario para permitir escribir libremente.
    val hasNarrowing = isPredictiveActive && validNextChars.isNotEmpty()

    val activeChars = remember(validNextChars, hasNarrowing) {
        if (hasNarrowing) {
            validNextChars.filter { it in allChars }.toSet()
        } else {
            allChars.toSet()
        }
    }

    val isSpaceEnabled = query.isNotEmpty() && (!hasNarrowing || (' ' in validNextChars))
    val isBackspaceEnabled = query.isNotEmpty()
    val isClearEnabled = query.isNotEmpty()

    val activeActions = remember(isSpaceEnabled, isBackspaceEnabled, isClearEnabled) {
        buildSet {
            if (isSpaceEnabled) add(KeyboardDirectionResolver.ActionKey.SPACE)
            if (isBackspaceEnabled) add(KeyboardDirectionResolver.ActionKey.BACKSPACE)
            if (isClearEnabled) add(KeyboardDirectionResolver.ActionKey.CLEAR)
        }
    }

    // Si la tecla actualmente enfocada deja de estar disponible (ej: tras pulsar una letra),
    // el foco salta automáticamente a la tecla activa más cercana para que NUNCA se pierda.
    LaunchedEffect(activeChars, hasNarrowing) {
        if (hasNarrowing) {
            val focused = currentFocusedChar
            if (focused != null && focused !in activeChars) {
                val currentPos = KeyboardDirectionResolver.KEY_POSITIONS[focused] ?: (0 to 0)
                val nearestChar = activeChars.minByOrNull { char ->
                    val pos = KeyboardDirectionResolver.KEY_POSITIONS[char] ?: (0 to 0)
                    kotlin.math.abs(pos.first - currentPos.first) + kotlin.math.abs(pos.second - currentPos.second)
                }
                if (nearestChar != null) {
                    delay(30)
                    try {
                        keyFocusRequesters[nearestChar]?.requestFocus()
                        currentFocusedChar = nearestChar
                    } catch (_: Exception) {}
                }
            }
        }
    }

    // Destinos direccionales hacia arriba para botones de acción
    val spaceUpTarget = KeyboardDirectionResolver.findNextLetterFromAction(
        KeyboardDirectionResolver.ActionKey.SPACE.col,
        activeChars
    )?.let { keyFocusRequesters[it] } ?: FocusRequester.Default

    val backspaceUpTarget = KeyboardDirectionResolver.findNextLetterFromAction(
        KeyboardDirectionResolver.ActionKey.BACKSPACE.col,
        activeChars
    )?.let { keyFocusRequesters[it] } ?: FocusRequester.Default

    val clearUpTarget = KeyboardDirectionResolver.findNextLetterFromAction(
        KeyboardDirectionResolver.ActionKey.CLEAR.col,
        activeChars
    )?.let { keyFocusRequesters[it] } ?: FocusRequester.Default

    Column(
        modifier = modifier.width(320.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Filas de Letras y Números (6 columnas)
        for (row in keyboardRows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally)
            ) {
                for (char in row) {
                    val isEnabled = !hasNarrowing || (char in validNextChars)
                    val isInitialTarget = char == 'A' && initialFocusRequester != null
                    val requester = keyFocusRequesters[char]

                    // Resolución direccional precisa guiando el foco hacia las teclas activas más cercanas
                    val rightTarget = if (isEnabled) {
                        KeyboardDirectionResolver.findNextKey(char, KeyboardDirectionResolver.Direction.RIGHT, activeChars)
                            ?.let { keyFocusRequesters[it] } ?: FocusRequester.Default
                    } else FocusRequester.Cancel

                    val leftTarget = if (isEnabled) {
                        KeyboardDirectionResolver.findNextKey(char, KeyboardDirectionResolver.Direction.LEFT, activeChars)
                            ?.let { keyFocusRequesters[it] } ?: FocusRequester.Cancel
                    } else FocusRequester.Cancel

                    val upTarget = if (isEnabled) {
                        KeyboardDirectionResolver.findNextKey(char, KeyboardDirectionResolver.Direction.UP, activeChars)
                            ?.let { keyFocusRequesters[it] } ?: FocusRequester.Default
                    } else FocusRequester.Cancel

                    val downTarget = if (isEnabled) {
                        val nextLetter = KeyboardDirectionResolver.findNextKey(char, KeyboardDirectionResolver.Direction.DOWN, activeChars)
                        if (nextLetter != null) {
                            keyFocusRequesters[nextLetter] ?: FocusRequester.Cancel
                        } else {
                            when (KeyboardDirectionResolver.findNextActionKey(char, activeActions)) {
                                KeyboardDirectionResolver.ActionKey.SPACE -> spaceFocusRequester
                                KeyboardDirectionResolver.ActionKey.BACKSPACE -> backspaceFocusRequester
                                KeyboardDirectionResolver.ActionKey.CLEAR -> clearFocusRequester
                                null -> FocusRequester.Cancel
                            }
                        }
                    } else FocusRequester.Cancel

                    TvKeyButton(
                        text = char.toString(),
                        isEnabled = isEnabled,
                        focusColor = focusColor,
                        focusContentColor = focusContentColor,
                        rightRequester = rightTarget,
                        leftRequester = leftTarget,
                        upRequester = upTarget,
                        downRequester = downTarget,
                        onClick = { onKeyClick(char) },
                        onFocused = { currentFocusedChar = char },
                        modifier = Modifier
                            .size(width = 46.dp, height = 36.dp)
                            .then(if (requester != null) Modifier.focusRequester(requester) else Modifier)
                            .then(if (isInitialTarget) Modifier.focusRequester(initialFocusRequester) else Modifier)
                    )
                }
            }
        }

        // Fila de Acciones: Espacio, Borrar, Limpiar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally)
        ) {
            // Espacio
            Button(
                onClick = { onKeyClick(' ') },
                enabled = isSpaceEnabled,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
                scale = ButtonDefaults.scale(focusedScale = 1.08f),
                colors = ButtonDefaults.colors(
                    containerColor = Color(0xFF1E2235),
                    contentColor = Color(0xFFE2E8F0),
                    focusedContainerColor = focusColor,
                    focusedContentColor = focusContentColor,
                    disabledContainerColor = Color(0xFF10121A).copy(alpha = 0.35f),
                    disabledContentColor = Color(0xFF475569).copy(alpha = 0.35f)
                ),
                border = ButtonDefaults.border(
                    border = Border(border = BorderStroke(1.dp, Color(0x35FFFFFF))),
                    focusedBorder = Border(border = BorderStroke(2.dp, focusColor)),
                    disabledBorder = Border.None
                ),
                modifier = Modifier
                    .weight(1.2f)
                    .height(36.dp)
                    .focusRequester(spaceFocusRequester)
                    .focusProperties {
                        canFocus = isSpaceEnabled
                        up = spaceUpTarget
                        left = FocusRequester.Cancel
                        right = when {
                            isBackspaceEnabled -> backspaceFocusRequester
                            isClearEnabled -> clearFocusRequester
                            else -> FocusRequester.Default
                        }
                        down = FocusRequester.Cancel
                    }
                    .alpha(if (isSpaceEnabled) 1f else 0.22f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SpaceBar,
                        contentDescription = "Espacio",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ESPACIO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }

            // Borrar
            Button(
                onClick = onBackspace,
                enabled = isBackspaceEnabled,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
                scale = ButtonDefaults.scale(focusedScale = 1.08f),
                colors = ButtonDefaults.colors(
                    containerColor = Color(0xFF1E2235),
                    contentColor = Color(0xFFE2E8F0),
                    focusedContainerColor = focusColor,
                    focusedContentColor = focusContentColor,
                    disabledContainerColor = Color(0xFF10121A).copy(alpha = 0.35f),
                    disabledContentColor = Color(0xFF475569).copy(alpha = 0.35f)
                ),
                border = ButtonDefaults.border(
                    border = Border(border = BorderStroke(1.dp, Color(0x35FFFFFF))),
                    focusedBorder = Border(border = BorderStroke(2.dp, focusColor)),
                    disabledBorder = Border.None
                ),
                modifier = Modifier
                    .weight(1.0f)
                    .height(36.dp)
                    .focusRequester(backspaceFocusRequester)
                    .focusProperties {
                        canFocus = isBackspaceEnabled
                        up = backspaceUpTarget
                        left = if (isSpaceEnabled) spaceFocusRequester else FocusRequester.Cancel
                        right = if (isClearEnabled) clearFocusRequester else FocusRequester.Default
                        down = FocusRequester.Cancel
                    }
                    .alpha(if (isBackspaceEnabled) 1f else 0.22f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Backspace,
                        contentDescription = "Borrar",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "BORRAR",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }

            // Limpiar
            Button(
                onClick = onClear,
                enabled = isClearEnabled,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
                scale = ButtonDefaults.scale(focusedScale = 1.08f),
                colors = ButtonDefaults.colors(
                    containerColor = Color(0xFF1E2235),
                    contentColor = Color(0xFFE2E8F0),
                    focusedContainerColor = focusColor,
                    focusedContentColor = focusContentColor,
                    disabledContainerColor = Color(0xFF10121A).copy(alpha = 0.35f),
                    disabledContentColor = Color(0xFF475569).copy(alpha = 0.35f)
                ),
                border = ButtonDefaults.border(
                    border = Border(border = BorderStroke(1.dp, Color(0x35FFFFFF))),
                    focusedBorder = Border(border = BorderStroke(2.dp, focusColor)),
                    disabledBorder = Border.None
                ),
                modifier = Modifier
                    .weight(0.9f)
                    .height(36.dp)
                    .focusRequester(clearFocusRequester)
                    .focusProperties {
                        canFocus = isClearEnabled
                        up = clearUpTarget
                        left = when {
                            isBackspaceEnabled -> backspaceFocusRequester
                            isSpaceEnabled -> spaceFocusRequester
                            else -> FocusRequester.Cancel
                        }
                        right = FocusRequester.Default
                        down = FocusRequester.Cancel
                    }
                    .alpha(if (isClearEnabled) 1f else 0.22f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Clear,
                        contentDescription = "Limpiar",
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "LIMPIAR",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvKeyButton(
    text: String,
    isEnabled: Boolean,
    focusColor: Color,
    focusContentColor: Color,
    rightRequester: FocusRequester,
    leftRequester: FocusRequester,
    upRequester: FocusRequester,
    downRequester: FocusRequester,
    onClick: () -> Unit,
    onFocused: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = isEnabled,
        contentPadding = PaddingValues(0.dp),
        shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
        scale = ButtonDefaults.scale(focusedScale = 1.15f),
        colors = ButtonDefaults.colors(
            containerColor = Color(0xFF161926),
            contentColor = Color(0xFFE2E8F0),
            focusedContainerColor = focusColor,
            focusedContentColor = focusContentColor,
            disabledContainerColor = Color(0xFF0F111A).copy(alpha = 0.20f),
            disabledContentColor = Color(0xFF334155).copy(alpha = 0.20f)
        ),
        border = ButtonDefaults.border(
            border = Border(border = BorderStroke(1.dp, Color(0x20FFFFFF))),
            focusedBorder = Border(border = BorderStroke(2.dp, focusColor)),
            disabledBorder = Border.None
        ),
        modifier = modifier
            .focusProperties {
                canFocus = isEnabled
                right = rightRequester
                left = leftRequester
                up = upRequester
                down = downRequester
            }
            .onFocusChanged { if (it.isFocused) onFocused() }
            .alpha(if (isEnabled) 1f else 0.12f)
    ) {
        Box(
            modifier = Modifier.size(width = 46.dp, height = 36.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
