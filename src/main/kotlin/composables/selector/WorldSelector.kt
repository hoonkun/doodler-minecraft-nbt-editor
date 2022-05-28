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
import doodler.nbt.tag.CompoundTag
import doodler.nbt.tag.StringTag
import java.io.File


@Composable
fun WorldSelector(window: DoodlerWindow, onSelect: (String, String) -> Unit) {

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

    Box(modifier = Modifier.background(ThemedColor.EditorArea).padding(start = 30.dp, end = 30.dp).fillMaxSize()) {
        Selector(onWorldSelect)
    }
}