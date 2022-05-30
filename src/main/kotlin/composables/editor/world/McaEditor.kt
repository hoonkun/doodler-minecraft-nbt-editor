package composables.editor.world

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import doodler.doodle.DoodleException
import doodler.editor.McaEditor
import doodler.editor.McaPayload
import doodler.logger.RecomposeLogger
import doodler.minecraft.McaWorker
import doodler.minecraft.structures.*
import doodler.nbt.tag.CompoundTag
import doodler.nbt.tag.DoubleTag
import doodler.nbt.tag.ListTag
import doodler.nbt.tag.StringTag
import java.io.File

@Composable
fun BoxScope.McaEditor(
    levelInfo: CompoundTag?,
    selector: McaEditor,
    tree: WorldHierarchy,
    onOpenRequest: (ChunkLocation, File) -> Unit,
    onUpdateRequest: (GlobalAnvilUpdateRequest) -> Unit
) {
    RecomposeLogger.log("McaEditor")

    val request = selector.from

    val data by remember(request) {
        mutableStateOf(
            when (request) {
                is McaAnvilRequest -> {
                    val file = request.file
                    val location = request.location

                    val dimension = WorldDimension[file.parentFile.parentFile.name]

                    val type = McaType[file.parentFile.name]

                    val chunks = McaWorker.loadChunkList(location, file.readBytes())

                    Pair(chunks, McaPayload(request, dimension, type, location, file))
                }
                is GlobalAnvilInitRequest -> {
                    val player = levelInfo?.get("Player")?.getAs<CompoundTag>()
                    val dimensionId = player?.get("Dimension")?.getAs<StringTag>()?.value

                    val pos = player?.get("Pos")?.getAs<ListTag>()
                    val x = pos?.get(0)?.getAs<DoubleTag>()?.value?.toInt()
                    val z = pos?.get(2)?.getAs<DoubleTag>()?.value?.toInt()

                    if (player == null || dimensionId == null || x == null || z == null)
                        throw DoodleException("Internal Error", null, "Could not find dimension data of Player.")

                    val dimension = WorldDimension.namespace(dimensionId)
                    val type = McaType.TERRAIN
                    val initial = BlockLocation(x, z)
                    val location = initial.toChunkLocation().toAnvilLocation()
                    val file = tree[dimension][McaType.TERRAIN.pathName]
                        .find { it.name == "r.${location.x}.${location.z}.mca" }
                        ?: throw DoodleException(
                            "Internal Error",
                            null,
                            "Could not find terrain region file which player exists."
                        )

                    val chunks = tree[dimension][McaType.TERRAIN.pathName].map {
                        val segments = it.name.split(".")
                        val itLocation = AnvilLocation(segments[1].toInt(), segments[2].toInt())
                        McaWorker.loadChunkList(itLocation, it.readBytes())
                    }.toList().flatten()

                    val payload = McaPayload(request, dimension, type, location, file, initial)
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
        )
    }

    val (chunks, payload) = data

    Column(modifier = Modifier.fillMaxSize()) {
        ChunkSelector(chunks, tree, selector, payload, onOpenRequest, onUpdateRequest)
    }
}