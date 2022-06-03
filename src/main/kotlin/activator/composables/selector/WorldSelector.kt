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
import activator.doodler.minecraft.DatWorker
import activator.doodler.nbt.tag.CompoundTag
import activator.doodler.nbt.tag.StringTag
import java.io.File


@Composable
fun WorldSelector(window: DoodlerWindow, onSelect: (String, String) -> Unit) {
    DoodlerLogger.recomposition("WorldSelector")


    val onWorldSelect: (File) -> Unit = open@ { file ->
        val path = file.absolutePath
        val levelName = DatWorker.read(File("$path/level.dat").readBytes())["Data"]
            ?.getAs<CompoundTag>()
            ?.get("LevelName")
            ?.getAs<StringTag>()
            ?.value ?: throw Exception("Invalid World Data")

        onSelect(levelName, path)
        window.close()
    }

    val selectable: (File) -> Boolean = selectable@ { file ->
        val level = File("${file.absolutePath}/level.dat")
        if (!level.exists()) return@selectable false

        DatWorker.read(level.readBytes())["Data"]
            ?.getAs<CompoundTag>()
            ?.get("LevelName")
            ?.getAs<StringTag>()
            ?.value
            ?: return@selectable false

        true
    }

    Box(modifier = Modifier.background(ThemedColor.EditorArea).padding(start = 30.dp, end = 30.dp).fillMaxSize()) {
        Selector(onWorldSelect, selectable)
    }
}