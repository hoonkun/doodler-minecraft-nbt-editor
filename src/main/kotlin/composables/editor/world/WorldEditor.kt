package composables.editor.world

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.BottomAppBar
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import composables.global.LinkText
import composables.global.ThemedColor
import composables.global.border
import doodler.editor.McaEditor
import doodler.editor.StandaloneNbtEditor
import doodler.editor.states.rememberWorldEditorState
import doodler.logger.DoodlerLogger
import doodler.minecraft.DatWorker
import doodler.minecraft.WorldUtils
import doodler.nbt.tag.CompoundTag
import doodler.nbt.tag.StringTag
import java.awt.Desktop
import java.net.URI
import java.util.*

@Composable
fun WorldEditor(
    worldPath: String
) {
    DoodlerLogger.recomposition("WorldEditor")

    val states = rememberWorldEditorState()

    if (states.worldSpec.tree == null)
        states.worldSpec.tree = WorldUtils.load(worldPath)

    val levelInfo = DatWorker.read(states.worldSpec.requireTree.level.readBytes())["Data"]?.getAs<CompoundTag>()

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
    DoodlerLogger.recomposition("MainColumn")

    Column(modifier = Modifier.fillMaxSize(), content = content)
}

@Composable
fun ColumnScope.TopBar(content: @Composable RowScope.() -> Unit) {
    DoodlerLogger.recomposition("TopBar")

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
    DoodlerLogger.recomposition("TopBarText")

    Text(text, color = Color.White, fontSize = 25.sp)
}

@Composable
fun ColumnScope.MainArea(content: @Composable RowScope.() -> Unit) {
    DoodlerLogger.recomposition("MainArea")

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
    DoodlerLogger.recomposition("MainFiles")

    Box(modifier = Modifier.fillMaxHeight().requiredWidth(400.dp).background(ThemedColor.TaskArea), content = content)
}

@Composable
fun RowScope.MainContents(content: @Composable BoxScope.() -> Unit) {
    DoodlerLogger.recomposition("MainContents")

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .weight(0.7f),
        content = content
    )
}

@Composable
fun ColumnScope.BottomBar(content: @Composable RowScope.() -> Unit) {
    DoodlerLogger.recomposition("BottomBar")

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
    DoodlerLogger.recomposition("BottomBarText")

    Text(
        text,
        color = ThemedColor.Copyright,
        fontSize = 14.sp
    )
}

@Composable
fun BoxScope.NoFileSelected(worldName: String) {
    DoodlerLogger.recomposition("NoFileSelected")

    Column (
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            "< world >",
            color = ThemedColor.WhiteOthers,
            fontSize = 29.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            worldName,
            color = Color.White,
            fontSize = 38.sp
        )
        Spacer(modifier = Modifier.height(35.dp))
        Text(
            "Select Tab in Left Area!",
            color = ThemedColor.WhiteSecondary,
            fontSize = 33.sp
        )
        Spacer(modifier = Modifier.height(25.dp))
        WhatIsThis("")
    }
}

@Composable
fun WhatIsThis(link: String) {
    DoodlerLogger.recomposition("WhatIsThis")

    Spacer(modifier = Modifier.height(60.dp))
    Text(
        "What is this?",
        color = ThemedColor.DocumentationDescription,
        fontSize = 25.sp
    )
    Spacer(modifier = Modifier.height(10.dp))
    LinkText(
        "Documentation",
        color = ThemedColor.Link,
        fontSize = 22.sp
    ) {
        openBrowser(link)
    }
}

fun openBrowser(url: String) {
    val osName = System.getProperty("os.name").lowercase(Locale.getDefault())

    val others: () -> Unit = {
        val runtime = Runtime.getRuntime()
        if (osName.contains("mac")) {
            runtime.exec("open $url")
        } else if (osName.contains("nix") || osName.contains("nux")) {
            runtime.exec("xdg-open $url")
        }
    }

    try {
        if (Desktop.isDesktopSupported()) {
            val desktop = Desktop.getDesktop()
            if (desktop.isSupported(Desktop.Action.BROWSE)) desktop.browse(URI(url))
            else others()
        } else {
            others()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
