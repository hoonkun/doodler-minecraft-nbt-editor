package composable.editor.world

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextOverflow
import doodler.editor.*
import doodler.editor.states.*
import doodler.exceptions.DoodleException
import doodler.minecraft.structures.McaType
import doodler.theme.DoodlerTheme
import doodler.unit.ddp
import doodler.unit.dsp

@Composable
fun WorldEditor(
    state: WorldEditorState
) {

    val onOpenRequest: (OpenRequest) -> Unit = handleRequest@ { request ->
        when (request) {
            is NbtOpenRequest -> openNbtEditor(state, request)
            is GlobalOpenRequest -> openGlobalMcaEditor(state)
            is SingleOpenRequest -> openSingleMcaEditor(state, request)
        }
    }

    WorldEditorRoot {
        BreadcrumbsBar {
            RootBreadcrumbText(state.worldSpec.name)
            state.manager.selected?.let { Breadcrumb(it.breadcrumb) }
            Spacer(modifier = Modifier.width(50.ddp))
        }

        MainArea {
            WorldHierarchyArea {
                WorldHierarchy(
                    worldSpec = state.worldSpec,
                    open = onOpenRequest
                )
            }
            EditorArea {
                if (state.manager.editors.size != 0) EditorManager(state)
                else EmptyTab()
            }
        }

        BottomBar {
            Spacer(modifier = Modifier.width(5.ddp))
            BottomBarText(
                text = state.manager.globalLogs.lastOrNull()?.let {
                    "[${it.title}] ${it.summary?.plus(": ") ?: ""}${it.description?.replace("\n", " ")}"
                } ?: "",
                modifier = Modifier.requiredWidthIn(max = 550.ddp)
            )
            Spacer(modifier = Modifier.weight(1f))
            BottomBarText("by kiwicraft")
            Spacer(modifier = Modifier.width(5.ddp))
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

        val type = McaType.Terrain
        val anvil = block.toChunkLocation().toAnvilLocation()
        val file = worldSpec.tree[dimension][type].find { it.name == "r.${anvil.x}.${anvil.z}.mca" }
            ?: throw DoodleException("Internal Error", null, "Cannot find region file which player exists")

        state.manager.open(
            GlobalMcaEditor(
            McaPayload(
                dimension = dimension,
                type = type,
                location = anvil,
                file = file
            )
        )
        )
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
fun ColumnScope.BreadcrumbsBar(content: @Composable RowScope.() -> Unit) {
    val hs = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(DoodlerTheme.Colors.SecondaryBackground)
                drawLine(
                    color = DoodlerTheme.Colors.SecondaryBackgroundBorder,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 2.ddp.value
                )
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier
                .horizontalScroll(hs, reverseScrolling = true)
                .height(28.ddp),
            content = {
                Spacer(modifier = Modifier.width(10.ddp))
                content()
            }
        )
    }
}

@Composable
fun RowScope.RootBreadcrumbText(text: String) =
    Text(
        text = text,
        color =
            DoodlerTheme.Colors.Text.IdeGeneral,
        fontSize = 10.dsp
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
            .height(18.ddp)
            .drawBehind {
                drawRect(DoodlerTheme.Colors.SecondaryBackground)
                drawLine(
                    color = DoodlerTheme.Colors.SecondaryBackgroundBorder,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.ddp.value
                )
            },
        content = content
    )

@Composable
fun RowScope.BottomBarText(
    text: String,
    modifier: Modifier = Modifier
) =
    Text(
        text = text,
        fontSize = 8.dsp,
        color = DoodlerTheme.Colors.HierarchyView.TextColor,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        modifier = modifier
    )
