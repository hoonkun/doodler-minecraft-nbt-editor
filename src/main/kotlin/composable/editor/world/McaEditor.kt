package composable.editor.world

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import doodler.doodle.structures.TagDoodle
import doodler.editor.*
import doodler.editor.states.McaEditorState
import doodler.editor.states.NbtEditorState
import doodler.exceptions.InternalAssertionException
import doodler.file.toStateFile
import doodler.minecraft.McaWorker
import doodler.minecraft.SurfaceWorker
import doodler.minecraft.structures.*
import java.io.File


@Composable
fun BoxScope.McaEditor(
    manager: EditorManager,
    editor: McaEditor<*>,
    cache: TerrainCache,
    worldSpec: WorldSpecification,
) {

    val openChunkNbt: (ChunkLocation, File) -> Unit = openChunkNbt@ { location, file ->
        val ident = AnvilNbtEditor.ident(file, location)
        if (manager.hasItem(ident)) {
            manager.select(ident)
            return@openChunkNbt
        }

        val root = McaWorker.loadChunk(location, file.readBytes()) ?: return@openChunkNbt
        manager.open(
            AnvilNbtEditor(
                anvil = file,
                location = location,
                state = NbtEditorState(
                    root = TagDoodle(root, -1, null),
                    file = file.toStateFile(),
                    type = McaFileType(location)
                )
            )
        )
    }

    val update: (McaPayload) -> Unit = updateGlobal@ {
        if (editor !is GlobalMcaEditor)
            throw InternalAssertionException("GlobalMcaEditor", editor.javaClass.simpleName)

        SurfaceWorker.load(it.file, manager.cache, it.yLimit, it.location, it.dimension)

        editor.payload = it
    }

    val chunks by remember(editor) {
        derivedStateOf {
            val payload = editor.payload
            when (editor) {
                is SingleMcaEditor -> McaWorker.loadChunkList(payload.location, payload.file.readBytes())
                is GlobalMcaEditor -> worldSpec.tree[payload.dimension][payload.type].map(file2chunkList).flatten()
            }
        }
    }

    val defaultStateProvider: (McaPayload) -> McaEditorState =
        when (editor) {
            is SingleMcaEditor -> ({ payload ->
                payload.let { SurfaceWorker.load(it.file, manager.cache, it.yLimit, it.location, it.dimension) }
                McaEditorState.new(
                    initialChunk = chunks.find { it.isIn(AnvilLocation(0, 0)) } ?: chunks.firstOrNull(),
                    yLimit = payload.yLimit
                )
            })
            is GlobalMcaEditor -> ({ payload ->
                payload.let { SurfaceWorker.load(it.file, manager.cache, it.yLimit, it.location, it.dimension) }
                if (payload.dimension == worldSpec.playerPos?.first) McaEditorState.new(
                    initialPos = worldSpec.playerPos?.second,
                    yLimit = payload.yLimit
                )
                else McaEditorState.new(
                    initialChunk = chunks.find { it.isIn(AnvilLocation(0, 0)) } ?: chunks.firstOrNull(),
                    yLimit = payload.yLimit
                )
            })
        }

    ChunkSelectorColumn {
        ChunkSelector(
            editor = editor,
            terrains = worldSpec.tree[editor.payload.dimension][McaType.Terrain],
            chunks = chunks,
            terrainCache = cache,
            update = update,
            openChunkNbt = openChunkNbt,
            defaultStateProvider = defaultStateProvider
        )
    }
}

@Composable
fun ChunkSelectorColumn(content: @Composable ColumnScope.() -> Unit) =
    Column(modifier = Modifier.fillMaxSize(), content = content)

private val file2chunkList: (file: File) -> List<ChunkLocation> = {
    val segments = it.name.split(".")
    val itAnvil = AnvilLocation(segments[1].toInt(), segments[2].toInt())
    McaWorker.loadChunkList(itAnvil, it.readBytes())
}
