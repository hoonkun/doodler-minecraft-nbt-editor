package composables.stateful.editor

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import composables.themed.JetBrainsMono
import composables.themed.ThemedColor
import doodler.file.WorldDimension
import doodler.file.WorldTree
import java.io.File


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BoxScope.WorldTreeView(worldName: String, tree: WorldTree) {

    val fileTree by remember(tree) { mutableStateOf(DirectoryItem(worldName, -1, createWorldTreeItems(tree))) }

    val items by remember(fileTree.expandedChildrenSize) { mutableStateOf(fileTree.list()) }

    var selected by remember { mutableStateOf<WorldTreeItem?>(null) }

    val vs = rememberScrollState()
    val hs = rememberScrollState()

    Box(
        modifier = Modifier
            .requiredWidthIn(400.dp)
            .verticalScroll(vs)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            for (item in items) {
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth()
                        .background(if (selected == item) Color(0xFF49544A) else Color.Transparent)
                ) { }
            }
        }
    }

    Box(
        modifier = Modifier
            .requiredWidth(400.dp)
            .verticalScroll(vs)
            .horizontalScroll(hs)
    ) {
        Column {
            for (item in items) {
                Box(
                    modifier = Modifier
                        .height(40.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(start = ((item.depth + 1) * 40).dp, top = 7.dp, bottom = 7.dp)
                    ) {
                        Text(
                            item.name,
                            color = Color(0xFFBBBBBB),
                            fontFamily = JetBrainsMono,
                            fontSize = 20.sp,
                            fontWeight = if (items.indexOf(item) == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .requiredWidthIn(400.dp)
            .verticalScroll(vs)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            for (item in items) {
                Box(
                    modifier = Modifier
                        .mouseClickable {
                            if (item is DirectoryItem) item.toggle()
                            selected = item
                        }
                        .height(40.dp)
                        .fillMaxWidth()
                ) { }
            }
        }
    }


    VerticalScrollbar(
        ScrollbarAdapter(vs),
        style = ScrollbarStyle(
            minimalHeight = 70.dp,
            thickness = 18.dp,
            shape = RectangleShape,
            hoverDurationMillis = 1,
            unhoverColor = ThemedColor.ScrollBarNormal,
            hoverColor = ThemedColor.ScrollBarHover
        ),
        modifier = Modifier.align(Alignment.TopEnd)
    )

    HorizontalScrollbar(
        ScrollbarAdapter(hs),
        style = ScrollbarStyle(
            minimalHeight = 70.dp,
            thickness = 18.dp,
            shape = RectangleShape,
            hoverDurationMillis = 1,
            unhoverColor = ThemedColor.ScrollBarNormal,
            hoverColor = ThemedColor.ScrollBarHover
        ),
        modifier = Modifier.align(Alignment.BottomStart)
    )

}

fun createWorldTreeItems(tree: WorldTree): List<WorldTreeItem> {
    val result = mutableListOf<WorldTreeItem>()
    val rootDepth = 0
    val depth = 1
    result.add(DirectoryItem("dimensions", rootDepth, mutableListOf<WorldTreeItem>().apply {
        add(DirectoryItem("overworld", depth, tree.listWorldFiles(WorldDimension.OVERWORLD)))
        add(DirectoryItem("nether", depth, tree.listWorldFiles(WorldDimension.NETHER)))
        add(DirectoryItem("the_end", depth, tree.listWorldFiles(WorldDimension.THE_END)))
    }))
    result.add(DirectoryItem("advancements", rootDepth, tree.advancements.map { FileItem(it.name, depth, it) }))
    result.add(DirectoryItem("playerdata", rootDepth, tree.players.filter { !it.name.contains(".dat_old") }.map { FileItem(it.name, depth, it) }))
    result.add(DirectoryItem("stats", rootDepth, tree.stats.map { FileItem(it.name, depth, it) }))
    result.add(FileItem("icon.png", rootDepth, tree.icon))
    result.add(FileItem("level.dat", rootDepth, tree.level))
    return result
}

open class WorldTreeItem(
    val name: String,
    val depth: Int
)

class FileItem(
    name: String,
    depth: Int,
    file: File
): WorldTreeItem(name, depth)

class DirectoryItem(
    name: String,
    depth: Int,
    private val children: List<WorldTreeItem>
): WorldTreeItem(name, depth) {
    private var expanded by mutableStateOf(false)

    val expandedChildrenSize: Int
        get() {
            var result = 0
            if (expanded) result++
            children.forEach { if (it is DirectoryItem) result += it.expandedChildrenSize }
            return result
        }

    private val expandedItems = mutableListOf<WorldTreeItem>()
    private val collapsedItems = children.toMutableList()

    fun list(): List<WorldTreeItem> {
        return mutableListOf<WorldTreeItem>().apply {
            add(this@DirectoryItem)
            expandedItems.sortedBy { it !is DirectoryItem }
                .map { if (it is DirectoryItem) addAll(it.list()) else add(it) }
        }
    }

    fun toggle() {
        if (expanded) collapse()
        else expand()
    }

    private fun expand() {
        if (expanded) return

        expandedItems.addAll(collapsedItems)
        collapsedItems.clear()

        expanded = true
    }

    private fun collapse() {
        if (!expanded) return

        collapsedItems.addAll(expandedItems)
        expandedItems.clear()

        expanded = false
    }
}
