package composables.stateful.editor.world

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.BottomAppBar
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import composables.stateless.editor.*
import composables.themed.ThemedColor
import composables.themed.border
import doodler.editor.McaEditor
import doodler.editor.StandaloneNbtEditor
import doodler.editor.states.rememberWorldEditorState
import doodler.minecraft.WorldUtils
import doodler.nbt.tag.CompoundTag
import doodler.nbt.tag.StringTag

@Composable
fun WorldEditor(
    worldPath: String
) {
    val states = rememberWorldEditorState()

    if (states.worldSpec.tree == null)
        states.worldSpec.tree = WorldUtils.load(worldPath)

    val levelInfo = WorldUtils.readLevel(states.worldSpec.requireTree.level.readBytes())["Data"]?.getAs<CompoundTag>()

    if (states.worldSpec.name == null)
        states.worldSpec.name = levelInfo!!["LevelName"]
            ?.getAs<StringTag>()
            ?.value

    if (states.worldSpec.tree == null || states.worldSpec.name == null) {
        // TODO: Handle Loading or Parse Error here
        return
    }

    val tree = states.worldSpec.requireTree
    val name = states.worldSpec.requireName

    val onOpenRequest: (OpenRequest) -> Unit = handleRequest@ { request ->
        when (request) {
            is NbtOpenRequest -> {
                if (states.manager.hasItem(request.ident)) {
                    states.manager.select(states.manager[request.ident]!!)
                } else {
                    states.manager.open(StandaloneNbtEditor.fromFile(request.file))
                }
            }
            is AnvilOpenRequest -> {
                if (!states.manager.hasItem("ANVIL_SELECTOR")) {
                    states.manager.open(McaEditor().apply { from = request })
                } else {
                    val selector = states.manager["ANVIL_SELECTOR"] ?: return@handleRequest
                    if (selector !is McaEditor) return@handleRequest

                    if (request is GlobalAnvilInitRequest && selector.globalMcaPayload != null)
                        selector.from = GlobalAnvilUpdateRequest()
                    else
                        selector.from = request

                    states.manager.select(selector)
                }
            }
        }
    }

    MaterialTheme {
        MainColumn {
            TopBar { TopBarText(name) }

            MainArea {
                MainFiles {
                    WorldTreeView(name, tree, onOpenRequest)
                }
                MainContents {
                    if (states.manager.editors.size == 0) {
                        NoFileSelected(name)
                    } else {
                        EditorManager(levelInfo, tree, states.manager)
                    }
                }
            }

            BottomBar {
                Spacer(modifier = Modifier.weight(1f))
                BottomBarText("by kiwicraft")
            }
        }
    }
}

@Composable
fun MainColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), content = content)
}

@Composable
fun ColumnScope.TopBar(content: @Composable RowScope.() -> Unit) {
    TopAppBar(
        elevation = 0.dp,
        backgroundColor = ThemedColor.ActionBar,
        contentPadding = PaddingValues(start = 25.dp, top = 10.dp, bottom = 10.dp),
        modifier = Modifier
            .height(60.dp)
            .zIndex(1f)
            .drawBehind(border(bottom = Pair(1f, ThemedColor.TopBar))),
        content = content
    )
}

@Composable
fun RowScope.TopBarText(text: String) {
    Text(text, color = Color.White, fontSize = 25.sp)
}

@Composable
fun ColumnScope.MainArea(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .background(ThemedColor.EditorArea)
            .zIndex(0f),
        content = content
    )
}

@Composable
fun RowScope.MainFiles(content: @Composable BoxScope.() -> Unit) {
    Box(modifier = Modifier.fillMaxHeight().requiredWidth(400.dp).background(ThemedColor.TaskArea), content = content)
}

@Composable
fun RowScope.MainContents(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .weight(0.7f),
        content = content
    )
}

@Composable
fun ColumnScope.BottomBar(content: @Composable RowScope.() -> Unit) {
    BottomAppBar(
        elevation = 0.dp,
        backgroundColor = ThemedColor.TaskArea,
        contentPadding = PaddingValues(top = 0.dp, bottom = 0.dp, start = 25.dp, end = 25.dp),
        modifier = Modifier
            .height(40.dp)
            .zIndex(1f)
            .drawBehind(border(top = Pair(1f, ThemedColor.TopBarBorder))),
        content = content
    )
}

@Composable
fun RowScope.BottomBarText(text: String) {
    Text(
        text,
        color = ThemedColor.Copyright,
        fontSize = 14.sp
    )
}