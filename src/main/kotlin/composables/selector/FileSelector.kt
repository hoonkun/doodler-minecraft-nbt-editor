package composables.selector

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import composables.global.ThemedColor
import doodler.application.structures.DoodlerWindow
import doodler.minecraft.DatWorker
import java.io.File

@Composable
fun FileSelector(window: DoodlerWindow,  onSelect: (String, String) -> Unit) {

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