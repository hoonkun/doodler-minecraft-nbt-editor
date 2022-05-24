package composables.stateful.editor

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import composables.states.editor.world.EditorItem
import composables.states.editor.world.StandaloneNbtItem
import composables.themed.JetBrainsMono
import composables.themed.ThemedColor
import doodler.anvil.AnvilLocation
import doodler.file.WorldDimension
import doodler.file.WorldTree
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skiko.toBufferedImage
import java.io.File


val EXTENSION_ALIAS = mapOf(
    "mca" to "AVL",
    "dat" to "NBT",
    "png" to "IMG",
    "jpg" to "IMG",
    "json" to "JSN"
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BoxScope.WorldTreeView(worldName: String, tree: WorldTree, onOpen: (OpenRequest) -> Unit) {

    val width = 400.dp
    val height = 45.dp

    val fileTree by remember(tree) { mutableStateOf(DirectoryItem(worldName, -1, createWorldTreeItems(tree)).apply { toggle() }) }

    val items by remember(fileTree.expandedChildrenSize) { mutableStateOf(fileTree.list()) }

    var selected by remember { mutableStateOf<WorldTreeItem?>(null) }

    val image by remember { mutableStateOf(loadImageFile(tree.icon)) }

    val iconModifier = remember { Modifier.width(35.dp).height(35.dp) }
    val expanderModifier = remember { iconModifier.alpha(0.6f).padding(4.dp) }

    val itemPaddingStart: (WorldTreeItem) -> Int = { item ->
        ((item.depth + 1) * 30 - (if (item is DirectoryItem) 35 else 0)).coerceAtLeast(0)
    }

    val vs = rememberScrollState()
    val hs = rememberScrollState()

    Box(
        modifier = Modifier
            .requiredWidthIn(width)
            .verticalScroll(vs)
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            for (item in items) {
                Box(
                    modifier = Modifier
                        .height(height)
                        .fillMaxWidth()
                        .background(if (selected == item) Color(0xFF49544A) else Color.Transparent)
                ) { }
            }
        }
    }

    Box(
        modifier = Modifier
            .requiredWidth(width)
            .verticalScroll(vs)
            .horizontalScroll(hs)
    ) {
        Column {
            for (item in items) {
                Box(
                    modifier = Modifier.height(height)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(start = itemPaddingStart(item).dp, end = 15.dp)
                            .align(Alignment.CenterStart),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(10.dp))
                        when (item) {
                            is DirectoryItem -> {
                                if (item.depth >= 0) {
                                    Image(
                                        painterResource("/icons/icon_${if (item.expanded) "collapse" else "expand"}.png"),
                                        null,
                                        modifier = expanderModifier
                                    )
                                }
                                if (item == fileTree) {
                                    Box(modifier = Modifier.padding(2.dp).wrapContentSize()) {
                                        Image(image, null, modifier = iconModifier.clip(RoundedCornerShape(3.dp)))
                                    }
                                } else {
                                    Image(
                                        painterResource("/icons/editor_folder.png"),
                                        null,
                                        modifier = iconModifier.padding(4.dp),
                                    )
                                }
                            }
                            is FileItem -> {
                                Box(
                                    modifier = Modifier.width(35.dp).height(35.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.width(30.dp).height(18.dp)
                                            .align(Alignment.Center)
                                            .background(
                                                when (item.file.extension) {
                                                    "dat" -> ThemedColor.Aside.DAT
                                                    "mca" -> ThemedColor.Aside.MCA
                                                    "png" -> ThemedColor.Aside.PNG
                                                    "json" -> ThemedColor.Aside.JSON
                                                    else -> Color.Transparent
                                                }
                                            )
                                    )
                                    TreeViewIndicatorText(EXTENSION_ALIAS[item.file.extension] ?: "")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(15.dp))
                        TreeViewText(item.name, items.indexOf(item) == 0)
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.requiredWidthIn(width).verticalScroll(vs).fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            for (item in items) {
                Box(modifier = Modifier.mouseClickable {
                    selected = item
                    if (item !is FileItem) return@mouseClickable
                    when (item.file.extension) {
                        "dat" -> onOpen(NbtOpenRequest(StandaloneNbtItem.fromFile(item.file)))
                        "mca" -> onOpen(AnvilOpenRequest(item.file.name.split(".").let { AnvilLocation(it[1].toInt(), it[2].toInt()) }, item.file))
                    }
                }.height(height).fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .padding(start = itemPaddingStart(item).dp, end = 15.dp)
                            .align(Alignment.CenterStart),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(10.dp))
                        if (item is DirectoryItem && item.depth >= 0) {
                            Box(modifier = iconModifier.absoluteOffset(x = ((-hs.value).dp)).mouseClickable { item.toggle() })
                        }
                    }
                }
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

@Composable
fun TreeViewText(text: String, bold: Boolean = false) {
    Text(
        text,
        color = Color(0xFFBBBBBB),
        fontFamily = JetBrainsMono,
        fontSize = 20.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
    )
}

@Composable
fun BoxScope.TreeViewIndicatorText(text: String) {
    Text(
        text,
        color = Color(0xFFBBBBBB),
        fontFamily = JetBrainsMono,
        fontSize = 12.sp,
        modifier = Modifier.align(Alignment.Center)
    )
}

fun loadImageFile(file: File): ImageBitmap {
    val image = org.jetbrains.skia.Image.makeFromEncoded(file.readBytes())
    val bitmap = Bitmap()
    bitmap.allocPixels(ImageInfo(64, 64, ColorType.N32, ColorAlphaType.OPAQUE))
    image.readPixels(bitmap)
    return bitmap.toBufferedImage().toComposeImageBitmap()
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
    val file: File
): WorldTreeItem(name, depth)

class DirectoryItem(
    name: String,
    depth: Int,
    private val children: List<WorldTreeItem>
): WorldTreeItem(name, depth) {
    var expanded by mutableStateOf(false)
        private set

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

open class OpenRequest

class NbtOpenRequest(
    val target: EditorItem
): OpenRequest()

class AnvilOpenRequest(
    val location: AnvilLocation,
    val file: File
): OpenRequest()
