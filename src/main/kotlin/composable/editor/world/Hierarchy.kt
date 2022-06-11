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
import doodler.editor.*
import doodler.minecraft.structures.AnvilLocation
import doodler.minecraft.structures.McaType
import doodler.minecraft.structures.WorldDimension
import doodler.minecraft.structures.WorldHierarchy
import doodler.theme.DoodlerTheme
import doodler.types.BooleanProvider
import doodler.unit.dp
import doodler.unit.sp
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skiko.toBufferedImage
import java.io.File


val Width = 225.dp
val ItemHeight = 29.dp

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
            open(GlobalOpenRequest)
            return@itemClick
        }

        if (it !is FileHierarchyItem) return@itemClick
        when (it.file.extension) {
            "dat" -> open(NbtOpenRequest(it.file))
            "mca" -> open(SingleMcaRequest(
                WorldDimension.fromMcaPath(it.file.absolutePath),
                McaType.fromMcaPath(it.file.absolutePath),
                AnvilLocation.fromFileName(it.file.name),
                it.file
            ))
        }
    }

    Box(modifier = Modifier.requiredWidth(Width).verticalScroll(verticalScrollState)) {

        SelectedIndicator(1, DoodlerTheme.Colors.HierarchyView.DimensionsDirectoryBackground)

        val index = items.indexOf(selected)
        if (index >= 0) {
            SelectedIndicator(index)
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
            Spacer(modifier = Modifier.width(7.dp))
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
fun SelectedIndicator(
    index: Int
) = SelectedIndicator(index, DoodlerTheme.Colors.HierarchyView.Selected)

@Composable
fun SelectedIndicator(
    index: Int,
    color: Color
) = Box(
    modifier = Modifier.offset { IntOffset(0, (ItemHeight * index).value.toInt()) },
    content = { Box(modifier = Modifier.height(ItemHeight).fillMaxWidth().background(color)) }
)

@Composable
fun DirectoryToggleIcon(expandedProvider: BooleanProvider) {
    Image(
        painter = painterResource("/icons/icon_${if (expandedProvider()) "collapse" else "expand"}.png"),
        contentDescription = null,
        modifier = Modifier.size(18.dp).alpha(0.6f)
    )
    Spacer(modifier = Modifier.width(5.dp))
}

@Composable
fun DirectoryToggleInteraction(onClick: () -> Unit) {
    Spacer(modifier = Modifier.size(18.dp).clickable(onClick = onClick))
    Spacer(modifier = Modifier.width(5.dp))
}

@Composable
fun WorldIcon(bitmap: ImageBitmap) {
    Image(
        bitmap = bitmap,
        contentDescription = null,
        modifier = Modifier.size(size = 20.dp).clip(RoundedCornerShape(3.dp))
    )
}

@Composable
fun DirectoryIcon() {
    Image(
        painter = painterResource("/icons/editor_folder.png"),
        contentDescription = null,
        modifier = Modifier.size(18.dp)
    )
}

@Composable
fun FileTypeIcon(extension: String) {
    Spacer(modifier = Modifier.width(3.dp))
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(width = 20.dp, height = 16.dp)
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
            fontSize = 8.sp
        )
    }
}

@Composable
fun HierarchyText(
    text: String,
    bold: Boolean = false,
    fontSize: TextUnit = MaterialTheme.typography.h5.fontSize
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
        7.dp + ((depth + 1) * 20 - (if (this is DirectoryHierarchyItem) 20 else 0)).coerceAtLeast(0).dp

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
        else listOf(this)
    }

    fun children(): List<HierarchyItem> =
        mutableListOf<HierarchyItem>().apply {
            add(this@DirectoryHierarchyItem)
            addAll(
                children
                    .map {
                        when (it) {
                            is FileHierarchyItem -> listOf(it)
                            is DirectoryHierarchyItem -> it.items.value
                        }
                    }
                    .flatten()
            )
        }

    fun toggle() {
        expanded = !expanded
    }
    
}


