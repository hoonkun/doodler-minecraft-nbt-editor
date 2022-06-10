package composable.editor.world

import activator.composables.global.ThemedColor
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import doodler.editor.GlobalInitRequest
import doodler.editor.NbtOpenRequest
import doodler.editor.OpenRequest
import doodler.editor.SingleMcaRequest
import doodler.minecraft.structures.AnvilLocation
import doodler.minecraft.structures.WorldDimension
import doodler.minecraft.structures.WorldHierarchy
import doodler.theme.DoodlerTheme
import doodler.types.BooleanProvider
import doodler.unit.dp
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skiko.toBufferedImage
import java.io.File


val Width = 325.dp
val ItemHeight = 37.dp

val ExtensionAlias = mapOf("mca" to "AVL", "dat" to "NBT", "png" to "IMG", "jpg" to "IMG", "json" to "JSN")

val HierarchyScrollBarStyle = ScrollbarStyle(
    minimalHeight = 70.dp,
    thickness = 18.dp,
    shape = RectangleShape,
    hoverDurationMillis = 1,
    unhoverColor = ThemedColor.ScrollBarNormal,
    hoverColor = ThemedColor.ScrollBarHover
)

@Composable
fun BoxScope.WorldHierarchy(
    name: String,
    hierarchy: WorldHierarchy,
    open: (OpenRequest) -> Unit
) {

    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    val root = remember { DirectoryHierarchyItem(createWorldTreeItems(hierarchy), name, -1).apply { toggle() } }
    val items by root.items

    var selected by remember { mutableStateOf<HierarchyItem?>(null) }

    val worldIcon = remember { loadImageFile(hierarchy.icon) }

    val onItemClick: (HierarchyItem) -> Unit = itemClick@ {
        if (it is FileHierarchyItem && it.file.length() == 0L) return@itemClick
        selected = it

        if (items.indexOf(it) == 1) {
            open(GlobalInitRequest)
            return@itemClick
        }

        if (it !is FileHierarchyItem) return@itemClick
        when (it.file.extension) {
            "dat" -> open(NbtOpenRequest(it.file))
            "mca" -> open(SingleMcaRequest(AnvilLocation.fromFileName(it.file.name), it.file))
        }
    }

    Box(modifier = Modifier.requiredWidth(Width).verticalScroll(verticalScrollState)) {

        val index = items.indexOf(selected)
        if (index >= 0) {
            Box(
                modifier = Modifier
                    .height(ItemHeight).fillMaxWidth()
                    .background(DoodlerTheme.Colors.HierarchyView.Selected)
                    .offset { IntOffset(0, (ItemHeight * index).value.toInt()) }
            )
        }

        HierarchyItemsColumn(
            horizontalScrollState = horizontalScrollState,
            items = items
        ) {
            if (it is DirectoryHierarchyItem) {
                if (it.depth >= 0) DirectoryToggleIcon { it.expanded }
                if (it.depth < 0) WorldIcon(worldIcon)
                else DirectoryIcon()
            } else if (it is FileHierarchyItem) {
                FileTypeIcon(it.file.extension)
            }
            Spacer(modifier = Modifier.width(15.dp))
            HierarchyText(
                text = it.name,
                bold = items.indexOf(it) == 0
            )
        }

        HierarchyItemsColumn(
            onClick = onItemClick,
            items = items
        ) {
            if (it !is DirectoryHierarchyItem) return@HierarchyItemsColumn
            if (it.depth >= 0) DirectoryToggleInteraction { it.toggle() }
        }
    }

    VerticalScrollbar(
        adapter = ScrollbarAdapter(verticalScrollState),
        style = HierarchyScrollBarStyle,
        modifier = Modifier.align(Alignment.TopEnd)
    )

    HorizontalScrollbar(
        adapter = ScrollbarAdapter(horizontalScrollState),
        style = HierarchyScrollBarStyle,
        modifier = Modifier.align(Alignment.BottomStart)
    )

}

@Composable
fun HierarchyItemsColumn(
    horizontalScrollState: ScrollState? = null,
    onClick: ((HierarchyItem) -> Unit)? = null,
    items: List<HierarchyItem>,
    content: @Composable RowScope.(HierarchyItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (horizontalScrollState != null) it.horizontalScroll(horizontalScrollState) else it }
    ) {
        for (item in items) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier
                    .padding(start = item.padding, end = 15.dp)
                    .height(ItemHeight)
                    .background(
                        if (items.indexOf(item) == 1) DoodlerTheme.Colors.HierarchyView.DimensionsDirectoryBackground
                        else Color.Transparent
                    )
                    .let {
                        if (onClick != null) it.fillMaxWidth().clickable { onClick(item) }
                        else it
                    },
                content = { content(item) }
            )
        }
    }
}

@Composable
fun DirectoryToggleIcon(expandedProvider: BooleanProvider) {
    Image(
        painter = painterResource("/icons/icon_${if (expandedProvider()) "collapse" else "expand"}.png"),
        contentDescription = null,
        modifier = Modifier.size(30.dp).alpha(0.6f).padding(4.dp)
    )
}

@Composable
fun DirectoryToggleInteraction(onClick: () -> Unit) {
    Spacer(modifier = Modifier.size(30.dp).padding(4.dp).clickable(onClick = onClick))
}

@Composable
fun WorldIcon(bitmap: ImageBitmap) {
    Image(
        bitmap = bitmap,
        contentDescription = null,
        modifier = Modifier.size(size = 27.dp).offset(x = 2.5f.dp, y = 2.5f.dp).clip(RoundedCornerShape(3.dp))
    )
}

@Composable
fun DirectoryIcon() {
    Image(
        painter = painterResource("/icons/editor_folder.png"),
        contentDescription = null,
        modifier = Modifier.size(32.dp)
    )
}

@Composable
fun FileTypeIcon(extension: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(width = 33.dp, height = 16.dp)
            .background(
                when (extension) {
                    "dat" -> DoodlerTheme.Colors.HierarchyView.Dat
                    "mca" -> DoodlerTheme.Colors.HierarchyView.Mca
                    "png" -> DoodlerTheme.Colors.HierarchyView.Png
                    "json" -> DoodlerTheme.Colors.HierarchyView.Json
                    else -> Color.Transparent
                }
            )
    ) {
        Text(
            text = ExtensionAlias[extension] ?: "",
            color = DoodlerTheme.Colors.HierarchyView.TextColor,
            fontSize = MaterialTheme.typography.h6.fontSize
        )
    }
}

@Composable
fun HierarchyText(
    text: String,
    bold: Boolean = false,
    fontSize: TextUnit = MaterialTheme.typography.h3.fontSize
) {
    Text(
        text = text,
        color = DoodlerTheme.Colors.HierarchyView.TextColor,
        fontSize = fontSize,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
    )
}

fun loadImageFile(file: File): ImageBitmap {
    val image = org.jetbrains.skia.Image.makeFromEncoded(file.readBytes())
    val bitmap = Bitmap()
    bitmap.allocPixels(ImageInfo(64, 64, ColorType.N32, ColorAlphaType.OPAQUE))
    image.readPixels(bitmap)
    return bitmap.toBufferedImage().toComposeImageBitmap()
}

fun createWorldTreeItems(hierarchy: WorldHierarchy): List<HierarchyItem> {
    val result = mutableListOf<HierarchyItem>()
    val rootDepth = 0
    val depth = 1
    result.add(
        DirectoryHierarchyItem(
            children = mutableListOf<HierarchyItem>().apply {
                add(DirectoryHierarchyItem(
                    children = hierarchy.listWorldFiles(WorldDimension.OVERWORLD),
                    name = "overworld",
                    depth = depth
                ))
                add(DirectoryHierarchyItem(
                    children = hierarchy.listWorldFiles(WorldDimension.NETHER),
                    name = "nether",
                    depth = depth
                ))
                add(DirectoryHierarchyItem(
                    children = hierarchy.listWorldFiles(WorldDimension.THE_END),
                    name = "the_end",
                    depth = depth
                ))
            },
            name = "dimensions",
            depth = rootDepth
        )
    )
    result.add(DirectoryHierarchyItem(
        children = hierarchy.advancements.map { FileHierarchyItem(it, it.name, depth) },
        name = "advancements",
        depth = rootDepth
    ))
    result.add(DirectoryHierarchyItem(
        children = hierarchy.players
            .filter { !it.name.endsWith(".dat_old") }
            .map { FileHierarchyItem(it, it.name, depth) },
        name = "playerdata",
        depth = rootDepth
    ))
    result.add(DirectoryHierarchyItem(
        children = hierarchy.stats.map { FileHierarchyItem(it, it.name, depth) },
        name = "stats",
        depth = rootDepth
    ))
    result.add(FileHierarchyItem(
        file = hierarchy.icon,
        name = "icon.png",
        depth = rootDepth
    ))
    result.add(FileHierarchyItem(
        file = hierarchy.level,
        name = "level.dat",
        depth = rootDepth))
    return result
}

sealed class HierarchyItem(
    val name: String,
    val depth: Int
) {

    val padding: Dp get() =
        ((depth + 1) * 30 - (if (this is DirectoryHierarchyItem) 35 else 0)).coerceAtLeast(0).dp

}

class FileHierarchyItem(
    val file: File,
    name: String,
    depth: Int
): HierarchyItem(name, depth)

class DirectoryHierarchyItem(
    private val children: List<HierarchyItem>,
    name: String,
    depth: Int
): HierarchyItem(name, depth) {

    var expanded by mutableStateOf(false); private set
    val items = derivedStateOf {
        if (expanded) children()
        else emptyList()
    }

    fun children(): List<HierarchyItem> =
        children
            .map {
                when (it) {
                    is FileHierarchyItem -> listOf(it)
                    is DirectoryHierarchyItem -> it.children()
                }
            }
            .flatten()

    fun toggle() {
        expanded = !expanded
    }
    
}


