package composable.editor.world

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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import doodler.editor.*
import doodler.extension.toUUID
import doodler.minecraft.structures.*
import doodler.theme.DoodlerTheme
import doodler.types.BooleanProvider
import doodler.unit.ddp
import doodler.unit.dsp
import java.io.File
import javax.imageio.ImageIO


val Width get() = 202.5.ddp
val ItemHeight get() = 22.ddp

val ExtensionAlias = mapOf(
    "mca" to "AVL",
    "dat" to "NBT",
    "nbt" to "NBT",
    "png" to "IMG",
    "jpg" to "IMG",
    "json" to "JSN"
)

@Composable
fun BoxScope.WorldHierarchy(
    worldSpec: WorldSpecification,
    open: (OpenRequest) -> Unit
) {

    val hierarchy = worldSpec.tree

    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    val root = remember {
        DirectoryHierarchyItem(createWorldTreeItems(hierarchy), "<WORLD_ROOT>", -1).apply { toggle() }
    }
    val items by root.items

    var selected by remember { mutableStateOf<HierarchyItem?>(null) }

    val worldIcon = if (hierarchy.icon.exists()) remember { loadImageFile(hierarchy.icon) } else null
    val fallbackWorldIcon = painterResource("/icons/intro/open_new_world.png")

    val onItemClick: (HierarchyItem) -> Unit = itemClick@ {
        if (it is FileHierarchyItem && it.file.length() == 0L) return@itemClick
        selected = it

        if (items.indexOf(it) == 1) {
            open(GlobalOpenRequest)
            return@itemClick
        }

        if (it !is FileHierarchyItem) return@itemClick
        when (it.file.extension) {
            "dat", "nbt" -> open(NbtOpenRequest(it.file))
            "mca" -> open(SingleOpenRequest(McaPayload(
                WorldDimension.fromMcaPath(it.file.absolutePath),
                McaType.fromMcaPath(it.file.absolutePath),
                AnvilLocation.fromFileName(it.file.name),
                it.file
            )))
        }
    }

    val isPlayerUUID: (HierarchyItem) -> Boolean = lambda@ {
        val uuidString = it.name.split(".")[0]

        if (uuidString.toUUID() == null) return@lambda false
        if (worldSpec.playerNames[uuidString] == null) return@lambda false

        return@lambda true
    }

    Box(modifier = Modifier.requiredWidth(Width).verticalScroll(verticalScrollState)) {

        SelectedIndicator(1, DoodlerTheme.Colors.HierarchyView.DimensionsDirectoryBackground)

        val index = items.indexOf(selected)
        if (index >= 0) {
            SelectedIndicator(index)
        }

        HierarchyItemsColumn(
            horizontalScrollState = horizontalScrollState,
            items = items,
            disabled = { it is FileHierarchyItem && it.file.length() == 0L }
        ) {
            if (it is DirectoryHierarchyItem) {
                if (it.depth >= 0) DirectoryToggleIcon { it.expanded }
                if (it.depth < 0) WorldIcon(worldIcon, fallbackWorldIcon)
                else DirectoryIcon()
            } else if (it is FileHierarchyItem) {
                FileTypeIcon(it.file.extension)
            }
            Spacer(modifier = Modifier.width(6.3.ddp))
            if (isPlayerUUID(it))
                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.height(ItemHeight)
                ) {
                    HierarchyText(text = it.name, bold = items.indexOf(it) == 0, fontSize = 8.5.dsp)
                    PlayerNameText(text = worldSpec.playerNames.getValue(it.name.split(".")[0]))
                }
            else HierarchyText(
                text = if (it.name == "<WORLD_ROOT>") worldSpec.name else it.name,
                bold = items.indexOf(it) == 0
            )
            if (it is FileHierarchyItem && it.description != null) {
                Spacer(modifier = Modifier.width(5.ddp))
                HierarchyDescription(it.description)
            }
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
        style = DoodlerTheme.ScrollBar.Default,
        modifier = Modifier.align(Alignment.TopEnd)
    )

    HorizontalScrollbar(
        adapter = ScrollbarAdapter(horizontalScrollState),
        style = DoodlerTheme.ScrollBar.Default,
        modifier = Modifier.align(Alignment.BottomStart)
    )

}

@Composable
fun HierarchyItemsColumn(
    horizontalScrollState: ScrollState? = null,
    onClick: ((HierarchyItem) -> Unit)? = null,
    disabled: ((HierarchyItem) -> Boolean)? = null,
    items: List<HierarchyItem>,
    content: @Composable RowScope.(HierarchyItem) -> Unit
) = Column(
    modifier = Modifier
        .fillMaxWidth()
        .let { if (horizontalScrollState != null) it.horizontalScroll(horizontalScrollState) else it }
) {
    for (item in items) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier
                .padding(start = item.padding, end = 13.5.ddp)
                .requiredHeight(ItemHeight)
                .alpha(if (disabled?.invoke(item) == true) 0.6f else 1f)
                .let {
                    if (onClick != null) it.fillMaxWidth().clickable { onClick(item) }
                    else it
                },
            content = { content(item) }
        )
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
    modifier = Modifier.offset { IntOffset(0, ItemHeight.roundToPx() * index) },
    content = { Box(modifier = Modifier.height(ItemHeight).fillMaxWidth().background(color)) }
)

@Composable
fun DirectoryToggleIcon(expandedProvider: BooleanProvider) {
    Image(
        painter = painterResource("/icons/icon_${if (expandedProvider()) "collapse" else "expand"}.png"),
        contentDescription = null,
        modifier = Modifier.size(9.9.ddp).alpha(0.6f)
    )
    Spacer(modifier = Modifier.width(4.5.ddp))
}

@Composable
fun DirectoryToggleInteraction(onClick: () -> Unit) {
    Spacer(modifier = Modifier.size(16.2.ddp).clickable(onClick = onClick))
    Spacer(modifier = Modifier.width(4.5.ddp))
}

@Composable
fun WorldIcon(bitmap: ImageBitmap?, fallback: Painter) {
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = Modifier.size(size = 15.ddp).clip(RoundedCornerShape(2.7.ddp))
        )
    } else {
        Image(
            painter = fallback,
            contentDescription = null,
            modifier = Modifier.size(size = 15.ddp).clip(RoundedCornerShape(2.7.ddp))
        )
    }
}

@Composable
fun DirectoryIcon() =
    Image(
        painter = painterResource("/icons/editor_folder.png"),
        contentDescription = null,
        modifier = Modifier.size(11.5.ddp)
    )

@Composable
fun FileTypeIcon(extension: String) {
    Spacer(modifier = Modifier.width(2.7.ddp))
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(width = 14.5.ddp, height = 8.8.ddp)
            .background(
                when (extension) {
                    "dat", "nbt" -> DoodlerTheme.Colors.HierarchyView.Dat
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
            fontSize = 5.85.dsp
        )
    }
}

@Composable
fun HierarchyText(
    text: String,
    bold: Boolean = false,
    fontSize: TextUnit = 10.8.dsp
) = Text(
    text = text,
    color = DoodlerTheme.Colors.HierarchyView.TextColor,
    fontSize = fontSize,
    fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
)

@Composable
fun HierarchyDescription(
    text: String
) = Box(modifier = Modifier.background(Color(0xff363636), RoundedCornerShape(2.ddp.value))) {
    Text(
        text = text,
        color = Color(0xffaaaaaa),
        fontSize = 7.dsp,
        modifier = Modifier.padding(vertical = 2.ddp, horizontal = 5.ddp)
    )
}

@Composable
fun PlayerNameText(
    text: String
) = Text(
    text = text,
    fontSize = 7.dsp,
    color = Color.White.copy(alpha = 0.5f)
)

fun loadImageFile(file: File): ImageBitmap {
    return ImageIO.read(file).toComposeImageBitmap()
}

fun createWorldTreeItems(hierarchy: WorldHierarchy): List<HierarchyItem> {
    val result = mutableListOf<HierarchyItem>()
    val rootDepth = 0
    val depth = 1
    result.add(
        DirectoryHierarchyItem(
            children = mutableListOf<HierarchyItem>().apply {
                add(DirectoryHierarchyItem(
                    children = hierarchy.listWorldFiles(WorldDimension.Overworld),
                    name = "overworld",
                    depth = depth
                ))
                add(DirectoryHierarchyItem(
                    children = hierarchy.listWorldFiles(WorldDimension.Nether),
                    name = "nether",
                    depth = depth
                ))
                add(DirectoryHierarchyItem(
                    children = hierarchy.listWorldFiles(WorldDimension.TheEnd),
                    name = "the_end",
                    depth = depth
                ))
            },
            name = "dimensions",
            depth = rootDepth
        ).apply { toggle() }
    )
    if (hierarchy.generated.isNotEmpty()) {
        result.add(DirectoryHierarchyItem(
            children = hierarchy.generated.map { FileHierarchyItem(it, name = it.name, depth = depth) },
            name = "generated",
            depth = rootDepth
        ))
    }
    result.add(DirectoryHierarchyItem(
        children = hierarchy.advancements.map { FileHierarchyItem(it, name = it.name, depth = depth) },
        name = "advancements",
        depth = rootDepth
    ))
    result.add(DirectoryHierarchyItem(
        children = hierarchy.players
            .filter { !it.name.endsWith(".dat_old") }
            .map { FileHierarchyItem(it, name = it.name, depth = depth) },
        name = "playerdata",
        depth = rootDepth
    ).apply { toggle() })
    result.add(DirectoryHierarchyItem(
        children = hierarchy.stats.map { FileHierarchyItem(it, name = it.name, depth = depth) },
        name = "stats",
        depth = rootDepth
    ))
    result.add(FileHierarchyItem(
        file = hierarchy.icon,
        name = "icon.png",
        depth = rootDepth
    ))
    when (hierarchy) {
        is VanillaWorldHierarchy ->
            result.add(FileHierarchyItem(
                file = hierarchy.level,
                name = "level.dat",
                depth = rootDepth
            ))
        is SpigotServerWorldHierarchy -> {
            WorldDimension.values().forEach {
                result.add(FileHierarchyItem(
                    file = hierarchy.level.getValue(it),
                    name = "level.dat",
                    depth = rootDepth,
                    description = it.displayName
                ))
            }
        }
    }
    return result
}

sealed class HierarchyItem(
    val name: String,
    val depth: Int
) {

    val padding: Dp get() {
        val initial = 6.3
        val depthPadding = (depth + 1) * 14.4
        val directoryOffset = if (this is DirectoryHierarchyItem) 11 else 0

        return (initial + (depthPadding - directoryOffset).coerceAtLeast(0.0)).ddp
    }

}

class FileHierarchyItem(
    val file: File,
    val description: String? = null,
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

    private fun children(): List<HierarchyItem> =
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


