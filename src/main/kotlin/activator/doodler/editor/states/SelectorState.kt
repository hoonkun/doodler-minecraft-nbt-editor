package activator.doodler.editor.states

import androidx.compose.runtime.*
import androidx.compose.ui.text.input.TextFieldValue
import activator.doodler.minecraft.structures.BlockLocation
import activator.doodler.minecraft.structures.ChunkLocation

@Stable
class SelectorState (
    selectedChunk: MutableState<ChunkLocation?> = mutableStateOf(null),
    chunkXValue: MutableState<TextFieldValue> = mutableStateOf(TextFieldValue("")),
    chunkZValue: MutableState<TextFieldValue> = mutableStateOf(TextFieldValue("")),
    blockXValue: MutableState<TextFieldValue> = mutableStateOf(TextFieldValue("-")),
    blockZValue: MutableState<TextFieldValue> = mutableStateOf(TextFieldValue("-")),
) {
    var selectedChunk by selectedChunk
    var chunkXValue by chunkXValue
    var chunkZValue by chunkZValue
    var blockXValue by blockXValue
    var blockZValue by blockZValue

    companion object {
        fun new(initialPos: BlockLocation?) =
            SelectorState(
                blockXValue = mutableStateOf(TextFieldValue(initialPos?.x?.toString() ?: "-")),
                blockZValue = mutableStateOf(TextFieldValue(initialPos?.z?.toString() ?: "-")),
                chunkXValue = mutableStateOf(TextFieldValue(initialPos?.toChunkLocation()?.x?.toString() ?: "-")),
                chunkZValue = mutableStateOf(TextFieldValue(initialPos?.toChunkLocation()?.z?.toString() ?: "-")),
                selectedChunk = mutableStateOf(initialPos?.toChunkLocation()),
            )

        fun new(initialChunk: ChunkLocation?) =
            SelectorState(
                chunkXValue = mutableStateOf(TextFieldValue(initialChunk?.x?.toString() ?: "-")),
                chunkZValue = mutableStateOf(TextFieldValue(initialChunk?.z?.toString() ?: "-")),
                selectedChunk = mutableStateOf(initialChunk),
            )
    }
}