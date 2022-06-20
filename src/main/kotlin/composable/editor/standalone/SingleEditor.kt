package composable.editor.standalone

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import composable.editor.NbtEditor
import composable.editor.world.Breadcrumb
import composable.editor.world.HierarchyBar
import doodler.doodle.structures.TagDoodle
import doodler.editor.StandaloneNbtEditor
import doodler.editor.states.NbtEditorState
import doodler.file.toStateFile
import doodler.minecraft.DatWorker
import doodler.minecraft.structures.DatFileType
import doodler.theme.DoodlerTheme
import doodler.unit.ddp
import java.io.File


@Composable
fun StandaloneNbtEditor(
    path: String
) {
    val file = File(path)
    val nbt = DatWorker.read(file.readBytes())
    val editor by remember {
        mutableStateOf(
            StandaloneNbtEditor(
                file,
                NbtEditorState(TagDoodle(nbt, -1, null), file.toStateFile(), DatFileType)
            )
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(DoodlerTheme.Colors.Background)) {
        HierarchyBar {
            Breadcrumb(editor.breadcrumb, disableFirstSlash = true)
            Spacer(modifier = Modifier.width(50.ddp))
        }
        Box(modifier = Modifier.fillMaxSize()) {
            NbtEditor(editor)
        }
    }
}