package com.ipdial.ui.screens

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AlphabetIndexer(
    alphabet: List<Char>,
    letterToFirstIndex: Map<Char, Int>,
    onLetterSelected: (Char, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var containerHeight by remember { mutableStateOf(1f) }
    
    val itemHeight = remember(containerHeight, alphabet.size) {
        (containerHeight / alphabet.size).coerceAtLeast(1f)
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(28.dp)
            .onGloballyPositioned { coordinates ->
                containerHeight = coordinates.size.height.toFloat()
            }
            .pointerInput(alphabet, letterToFirstIndex, itemHeight) {
                detectTapGestures(
                    onPress = { offset ->
                        val index = (offset.y / itemHeight).toInt().coerceIn(0, alphabet.size - 1)
                        if (index in alphabet.indices) {
                            // Find closest letter with contacts starting with it
                            val actualChar = alphabet.drop(index).firstOrNull { letterToFirstIndex.containsKey(it) }
                                ?: alphabet.take(index).lastOrNull { letterToFirstIndex.containsKey(it) }
                            
                            if (actualChar != null) {
                                letterToFirstIndex[actualChar]?.let { targetIdx ->
                                    onLetterSelected(actualChar, targetIdx)
                                }
                            }
                        }
                    }
                )
            }
            .pointerInput(alphabet, letterToFirstIndex, itemHeight) {
                detectDragGestures(
                    onDrag = { change, _ ->
                        change.consume()
                        val index = (change.position.y / itemHeight).toInt().coerceIn(0, alphabet.size - 1)
                        if (index in alphabet.indices) {
                            // Find closest letter with contacts starting with it
                            val actualChar = alphabet.drop(index).firstOrNull { letterToFirstIndex.containsKey(it) }
                                ?: alphabet.take(index).lastOrNull { letterToFirstIndex.containsKey(it) }
                            
                            if (actualChar != null) {
                                letterToFirstIndex[actualChar]?.let { targetIdx ->
                                    onLetterSelected(actualChar, targetIdx)
                                }
                            }
                        }
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            alphabet.forEach { char ->
                val hasContacts = letterToFirstIndex.containsKey(char)
                Text(
                    text = char.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (hasContacts) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 10.sp
                    ),
                    color = if (hasContacts) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    },
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }
    }
}
