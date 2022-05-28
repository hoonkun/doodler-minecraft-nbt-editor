package composables.selector

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import composables.global.ThemedColor
import doodler.minecraft.DatWorker
import doodler.nbt.tag.CompoundTag
import doodler.nbt.tag.StringTag
import java.io.File


@Composable
fun WorldSelector(onSelect: (String, String) -> Unit) {

    val selected = mutableStateOf<File?>(null)

    val onWorldSelect = open@ {
        val file = selected.value ?: return@open

        val path = file.absolutePath
        val levelName = DatWorker.read(File("$path/level.dat").readBytes())["Data"]
            ?.getAs<CompoundTag>()
            ?.get("LevelName")
            ?.getAs<StringTag>()
            ?.value ?: throw Exception("Invalid World Data")

        onSelect(levelName, path)
    }

    Box(modifier = Modifier.background(ThemedColor.EditorArea).padding(start = 30.dp, end = 30.dp)) {
        Selector(selected)
    }
}