package composables.editor.single

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import composables.editor.NbtEditor
import composables.global.ThemedColor
import doodler.editor.StandaloneNbtEditor
import doodler.editor.states.NbtState
import doodler.logger.RecomposeLogger
import doodler.minecraft.DatWorker
import doodler.minecraft.structures.DatFileType
import java.io.File

@Composable
fun SingleEditor(
    nbtPath: String
) {
    RecomposeLogger.log("SingleEditor")

    val file = File(nbtPath)
    val nbt = DatWorker.read(file.readBytes())
    val editor by remember { mutableStateOf(StandaloneNbtEditor(NbtState.new(nbt, file, DatFileType), file)) }

    Box(modifier = Modifier.fillMaxSize().background(ThemedColor.EditorArea)) {
        NbtEditor(editor)
    }
}