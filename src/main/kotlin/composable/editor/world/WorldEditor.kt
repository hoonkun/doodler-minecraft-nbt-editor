package composable.editor.world

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import doodler.editor.*
import doodler.exceptions.DoodleException
import doodler.minecraft.structures.McaType
import doodler.theme.DoodlerTheme
import doodler.unit.dp
import doodler.unit.sp

@Composable
fun WorldEditor(
    worldPath: String
) {

    val state = rememberWorldEditorState(worldPath)

    val onOpenRequest: (OpenRequest) -> Unit = handleRequest@ { request ->
        when (request) {
            is NbtOpenRequest -> openNbtEditor(state, request)
            is GlobalOpenRequest -> openGlobalMcaEditor(state)
            is SingleOpenRequest -> openSingleMcaEditor(state, request)
        }
    }

    WorldEditorRoot {
        HierarchyBar {
            HierarchyBarText(state.worldSpec.name)
        }

        MainArea {
            WorldHierarchyArea {
                WorldHierarchy(name = state.worldSpec.name, hierarchy = state.worldSpec.tree, open = onOpenRequest)
            }
            EditorArea {
                if (state.manager.editors.size != 0) EditorManager(state)
            }
        }

        BottomBar {
            Spacer(modifier = Modifier.width(5.dp))
            BottomBarText("TODO: print  log here.")
            Spacer(modifier = Modifier.weight(1f))
            BottomBarText("by kiwicraft")
            Spacer(modifier = Modifier.width(5.dp))
        }
    }
}

fun openNbtEditor(state: WorldEditorState, request: NbtOpenRequest) {
    if (state.manager.hasItem(request.ident)) {
        state.manager.select(request.ident)
    } else {
        state.manager.open(StandaloneNbtEditor.fromFile(request.file))
    }
}

fun openGlobalMcaEditor(state: WorldEditorState) {
    if (state.manager.hasItem(GlobalMcaEditor.Identifier)) {
        state.manager.select(GlobalMcaEditor.Identifier)
    } else {
        val worldSpec = state.worldSpec

        val (dimension, block) = worldSpec.playerPos
            ?: throw DoodleException("Internal Error", null, "Cannot read player data from level.dat")

        val type = McaType.TERRAIN
        val anvil = block.toChunkLocation().toAnvilLocation()
        val file = worldSpec.tree[dimension][type].find { it.name == "r.${anvil.x}.${anvil.z}.mca" }
            ?: throw DoodleException("Internal Error", null, "Cannot find region file which player exists")

        state.manager.open(GlobalMcaEditor(
            McaPayload(
                dimension = dimension,
                type = type,
                location = anvil,
                file = file
            )
        ))
    }
}

fun openSingleMcaEditor(state: WorldEditorState, request: SingleOpenRequest) {
    if (state.manager.hasItem(SingleMcaEditor.Identifier)) {
        (state.manager[SingleMcaEditor.Identifier] as SingleMcaEditor).payload = request.payload
        state.manager.select(SingleMcaEditor.Identifier)
    } else {
        state.manager.open(SingleMcaEditor(initialPayload = request.payload))
    }
}


@Composable
fun WorldEditorRoot(content: @Composable ColumnScope.() -> Unit) =
    Column(modifier = Modifier.fillMaxSize(), content = content)

@Composable
fun ColumnScope.HierarchyBar(content: @Composable RowScope.() -> Unit) =
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .drawBehind {
                drawRect(DoodlerTheme.Colors.SecondaryBackground)
                drawLine(
                    color = DoodlerTheme.Colors.SecondaryBackgroundBorder,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height)
                )
            },
        content = {
            Spacer(modifier = Modifier.width(10.dp))
            content()
        }
    )

@Composable
fun RowScope.HierarchyBarText(text: String, hasChanges: Boolean = false) =
    Text(
        text = text,
        color =
            if (!hasChanges) DoodlerTheme.Colors.HierarchyView.TextColor
            else DoodlerTheme.Colors.Editor.TabHasChanges,
        fontSize = MaterialTheme.typography.h6.fontSize
    )

@Composable
fun ColumnScope.MainArea(content: @Composable RowScope.() -> Unit) =
    Row(
        modifier = Modifier.fillMaxWidth().weight(1f).background(DoodlerTheme.Colors.Background),
        content = content
    )

@Composable
fun RowScope.WorldHierarchyArea(content: @Composable BoxScope.() -> Unit) =
    Box(
        modifier = Modifier.width(Width).fillMaxHeight().background(DoodlerTheme.Colors.SecondaryBackground),
        content = content
    )

@Composable
fun RowScope.EditorArea(content: @Composable BoxScope.() -> Unit) =
    Box(
        modifier = Modifier.fillMaxHeight().weight(1f),
        content = content
    )

@Composable
fun ColumnScope.BottomBar(content: @Composable RowScope.() -> Unit) =
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(18.dp)
            .drawBehind {
                drawRect(DoodlerTheme.Colors.SecondaryBackground)
                drawLine(
                    color = DoodlerTheme.Colors.SecondaryBackgroundBorder,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f)
                )
            },
        content = content
    )

@Composable
fun RowScope.BottomBarText(text: String) =
    Text(
        text = text,
        fontSize = 8.sp,
        color = DoodlerTheme.Colors.HierarchyView.TextColor
    )
