package activator.composables.editor.world

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import activator.composables.editor.NbtEditor
import activator.doodler.editor.AnvilNbtEditor
import activator.doodler.editor.EditorManager
import activator.doodler.editor.McaEditor
import activator.doodler.editor.NbtEditor
import activator.doodler.editor.states.NbtState
import activator.doodler.logger.DoodlerLogger
import activator.doodler.minecraft.McaWorker
import activator.doodler.minecraft.structures.McaFileType
import activator.doodler.minecraft.structures.WorldHierarchy
import activator.doodler.nbt.tag.CompoundTag

@Composable
fun BoxScope.EditorManager(
    levelInfo: CompoundTag?,
    tree: WorldHierarchy,
    manager: EditorManager,
) {
    DoodlerLogger.recomposition("EditorManager")

    EditorManagerRoot {
        EditorTabs(
            manager.editors.map { TabData(
                manager.selected == it,
                it,
                if (it is NbtEditor) it.state.actions.history.lastActionUid != it.state.lastSaveUid else false
            ) },
            { manager.select(it) },
            { manager.close(it) }
        )
        Editors {
            val selected = manager.selected
            if (selected is NbtEditor) NbtEditor(selected)
            else if (selected is McaEditor) {
                McaEditor(
                    levelInfo, selected, tree,
                    open@ { location, file ->
                        if (manager.hasItem("${file.absolutePath}/c.${location.x}.${location.z}")) return@open

                        val root = McaWorker.loadChunk(location, file.readBytes()) ?: return@open
                        manager.open(AnvilNbtEditor(NbtState.new(root, file, McaFileType(location)), file, location))
                    },
                    update@ {
                        val selector = manager["ANVIL_SELECTOR"] ?: return@update
                        if (selector !is McaEditor) return@update

                        selector.from = it
                    }
                )
            }
        }
    }
}

@Composable
fun BoxScope.EditorManagerRoot(content: @Composable ColumnScope.() -> Unit) {
    DoodlerLogger.recomposition("EditorManagerRoot")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f),
        content = content
    )
}

@Composable
fun ColumnScope.Editors(content: @Composable BoxScope.() -> Unit) {
    DoodlerLogger.recomposition("Editors")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        content = content
    )
}