package doodler.editor.states

import doodler.minecraft.structures.BlockLocation
import doodler.minecraft.structures.ChunkLocation
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.TextFieldValue

@Stable
class McaEditorState (
    selectedChunk: MutableState<ChunkLocation?> = mutableStateOf(null),
    chunkXValue: MutableState<TextFieldValue> = mutableStateOf(TextFieldValue("")),
    chunkZValue: MutableState<TextFieldValue> = mutableStateOf(TextFieldValue("")),
    blockXValue: MutableState<TextFieldValue> = mutableStateOf(TextFieldValue("-")),
    blockZValue: MutableState<TextFieldValue> = mutableStateOf(TextFieldValue("-")),
    yLimit: MutableState<Int>
) {
    var selectedChunk by selectedChunk
    var chunkXValue by chunkXValue
    var chunkZValue by chunkZValue
    var blockXValue by blockXValue
    var blockZValue by blockZValue
    var yLimit by yLimit

    companion object {
        fun new(initialPos: BlockLocation?, yLimit: Int) =
            McaEditorState(
                blockXValue = mutableStateOf(TextFieldValue(initialPos?.x?.toString() ?: "-")),
                blockZValue = mutableStateOf(TextFieldValue(initialPos?.z?.toString() ?: "-")),
                chunkXValue = mutableStateOf(TextFieldValue(initialPos?.toChunkLocation()?.x?.toString() ?: "-")),
                chunkZValue = mutableStateOf(TextFieldValue(initialPos?.toChunkLocation()?.z?.toString() ?: "-")),
                selectedChunk = mutableStateOf(initialPos?.toChunkLocation()),
                yLimit = mutableStateOf(yLimit)
            )

        fun new(initialChunk: ChunkLocation?, yLimit: Int) =
            McaEditorState(
                chunkXValue = mutableStateOf(TextFieldValue(initialChunk?.x?.toString() ?: "-")),
                chunkZValue = mutableStateOf(TextFieldValue(initialChunk?.z?.toString() ?: "-")),
                selectedChunk = mutableStateOf(initialChunk),
                yLimit = mutableStateOf(yLimit)
            )
    }
}