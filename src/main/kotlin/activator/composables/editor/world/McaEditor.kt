package activator.composables.editor.world

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import doodler.exceptions.DoodleException
import activator.doodler.editor.McaEditor
import activator.doodler.editor.McaPayload
import activator.doodler.logger.DoodlerLogger
import doodler.minecraft.McaWorker
import doodler.minecraft.structures.*
import java.io.File

@Composable
fun BoxScope.McaEditor(
    worldSpec: WorldSpecification,
    selector: McaEditor,
    onOpenRequest: (ChunkLocation, File) -> Unit,
    onUpdateRequest: (GlobalAnvilUpdateRequest) -> Unit
) {
    DoodlerLogger.recomposition("McaEditor")

    val tree = worldSpec.tree

    val data by remember {
        derivedStateOf {
            when (val request = selector.from) {
                is McaAnvilRequest -> {
                    val file = request.file
                    val location = request.location

                    val dimension = WorldDimension[file.parentFile.parentFile.name]

                    val type = McaType[file.parentFile.name]

                    val chunks = McaWorker.loadChunkList(location, file.readBytes())

                    Pair(chunks, McaPayload(request, dimension, type, location, file))
                }
                is GlobalAnvilInitRequest -> {
                    val (dimension, block) = worldSpec.playerPos
                        ?: throw DoodleException("Internal Error", null, "Could not find dimension data of Player.")

                    val type = McaType.Terrain
                    val location = block.toChunkLocation().toAnvilLocation()
                    val file = tree[dimension][McaType.Terrain.pathName]
                        .find { it.name == "r.${location.x}.${location.z}.mca" }
                        ?: throw DoodleException(
                            "Internal Error",
                            null,
                            "Could not find terrain region file which player exists."
                        )

                    val chunks = tree[dimension][McaType.Terrain.pathName].map {
                        val segments = it.name.split(".")
                        val itLocation = AnvilLocation(segments[1].toInt(), segments[2].toInt())
                        McaWorker.loadChunkList(itLocation, it.readBytes())
                    }.toList().flatten()

                    val payload = McaPayload(request, dimension, type, location, file, block)
                    selector.globalMcaPayload = payload

                    Pair(chunks, payload)
                }
                is GlobalAnvilUpdateRequest -> {
                    val base = selector.globalMcaPayload ?: throw DoodleException(
                        "Internal Error",
                        null,
                        "Failed to update null McaInfo"
                    )

                    val newMcaInfo = McaPayload.from(
                        base,
                        request = request,
                        dimension = request.dimension,
                        type = request.type,
                        location = request.region,
                        file = tree[request.dimension ?: base.dimension][(request.type ?: base.type).pathName].find {
                            val reg = request.region
                            if (reg != null) it.name == "r.${reg.x}.${reg.z}.mca" else false
                        },
                        initial = null
                    )

                    selector.globalMcaPayload = newMcaInfo

                    val chunks = tree[request.dimension ?: newMcaInfo.dimension][(request.type
                        ?: newMcaInfo.type).pathName].map {
                        val segments = it.name.split(".")
                        val itLocation = AnvilLocation(segments[1].toInt(), segments[2].toInt())
                        McaWorker.loadChunkList(itLocation, it.readBytes())
                    }.toList().flatten()

                    Pair(chunks, newMcaInfo)
                }
                else -> {
                    throw DoodleException("Internal Error", null, "Cannot initialize McaInfo.")
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ChunkSelector(data.first, tree, selector, data.second, onOpenRequest, onUpdateRequest)
    }
}