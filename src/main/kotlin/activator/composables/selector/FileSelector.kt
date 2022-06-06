package activator.composables.selector

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import activator.composables.global.ThemedColor
import activator.doodler.application.structures.DoodlerWindow
import activator.doodler.logger.DoodlerLogger
import doodler.minecraft.DatWorker
import java.io.File

@Composable
fun FileSelector(window: DoodlerWindow,  onSelect: (String, String) -> Unit) {
    DoodlerLogger.recomposition("FileSelector")


    val onFileSelect: (File) -> Unit = open@ { file ->
        onSelect(file.name, file.absolutePath)
        window.close()
    }

    val selectable: (File) -> Boolean = selectable@ { file ->
        try {
            DatWorker.read(file.readBytes())
        } catch (e: Exception) {
            return@selectable false
        }

        true
    }

    Box(modifier = Modifier.background(ThemedColor.EditorArea).padding(start = 30.dp, end = 30.dp).fillMaxSize()) {
        Selector(onFileSelect, selectable)
    }
}