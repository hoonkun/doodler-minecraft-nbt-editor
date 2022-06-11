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
            is NbtOpenRequest -> {
                if (state.manager.hasItem(request.ident)) {
                    state.manager.select(request.ident)
                } else {
                    state.manager.open(StandaloneNbtEditor.fromFile(request.file))
                }
            }
            is GlobalMcaRequest -> {
                if (state.manager.hasItem(GlobalMcaEditor.Identifier)) {
                    state.manager.select(GlobalMcaEditor.Identifier)
                } else {
                    state.manager.open(GlobalMcaEditor())
                }
            }
            is SingleMcaRequest -> {
                if (state.manager.hasItem(SingleMcaEditor.Identifier)) {
                    (state.manager[SingleMcaEditor.Identifier] as SingleMcaEditor).payload = request
                    state.manager.select(SingleMcaEditor.Identifier)
                } else {
                    state.manager.open(SingleMcaEditor(request))
                }
            }
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
            if (!hasChanges) DoodlerTheme.Colors.Text.IdeGeneral
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
fun RowScope.BottomBarText(text: String) = Text(text , fontSize = 8.sp, color = DoodlerTheme.Colors.OnBackground)
