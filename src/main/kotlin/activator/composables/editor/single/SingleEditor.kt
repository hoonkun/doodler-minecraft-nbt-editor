package activator.composables.editor.single

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import activator.composables.editor.NbtEditor
import activator.composables.global.ThemedColor
import activator.doodler.editor.StandaloneNbtEditor
import activator.doodler.editor.states.NbtState
import activator.doodler.logger.DoodlerLogger
import activator.doodler.minecraft.DatWorker
import activator.doodler.minecraft.structures.DatFileType
import java.io.File

@Composable
fun SingleEditor(
    nbtPath: String
) {
    DoodlerLogger.recomposition("SingleEditor")

    val file = File(nbtPath)
    val nbt = DatWorker.read(file.readBytes())
    val editor by remember { mutableStateOf(StandaloneNbtEditor(NbtState.new(nbt, file, DatFileType), file)) }

    Box(modifier = Modifier.fillMaxSize().background(ThemedColor.EditorArea)) {
        NbtEditor(editor)
    }
}