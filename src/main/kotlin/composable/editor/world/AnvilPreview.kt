package composable.editor.world

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.zIndex
import doodler.editor.CachedTerrainInfo
import doodler.editor.TerrainCache
import doodler.extension.toReversedRange
import doodler.minecraft.McaWorker
import doodler.minecraft.SurfaceWorker
import doodler.minecraft.structures.AnvilLocation
import doodler.minecraft.structures.AnvilLocationSurroundings
import doodler.minecraft.structures.ChunkLocation
import doodler.minecraft.structures.WorldDimension
import doodler.theme.DoodlerTheme
import doodler.unit.ddp
import doodler.unit.dsp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skiko.toBufferedImage
import java.io.File


@Composable
fun AnvilPreview(
    terrain: File?,
    properties: ChunkPreviewProperties,
    cache: TerrainCache,
    chunk: ChunkLocation?,
    anvil: AnvilLocation,
    hasNbt: (ChunkLocation) -> Boolean,
    moveToSurroundings: (AnvilLocation) -> Unit,
    invalidateCache: () -> Unit,
    onItemClick: (ChunkLocation) -> Unit
) {

    var yLimit by remember(properties.dimension) { mutableStateOf(properties.dimension.defaultYLimit) }

    var propertiesVisible by remember { mutableStateOf(false) }

    Box(
        contentAlignment = PropertiesAlignment,
        modifier = Modifier
            .requiredSizeIn(minWidth = MinimumViewSize.ddp, minHeight = MinimumViewSize.ddp)
            .fillMaxHeight()
            .aspectRatio(1f)
    ) {
        AnvilImageLoader(
            cache = cache,
            terrainFile = terrain,
            chunk = chunk,
            anvil = anvil,
            dimension = properties.dimension,
            yLimit = yLimit,
            overlay = { anvil ->
                ChunkButtons(
                    chunk = chunk,
                    anvil = anvil,
                    hasNbt = hasNbt,
                    onItemClick = onItemClick,
                    onRightClick = { propertiesVisible = !propertiesVisible }
                )
            }
        )
        AnvilPreviewProperties(
            properties = properties,
            yLimit = yLimit,
            moveToSurroundings = moveToSurroundings,
            changeYLimit = { yLimit = (yLimit - it).coerceIn(properties.dimension.yRange) },
            invalidateCache = invalidateCache,
            visible = propertiesVisible
        )
    }

}

@Composable
fun AnvilImageLoader(
    cache: TerrainCache,
    terrainFile: File?,
    chunk: ChunkLocation?,
    anvil: AnvilLocation,
    dimension: WorldDimension,
    yLimit: Int,
    overlay: @Composable BoxScope.(AnvilLocation) -> Unit
) {

    val location = remember(chunk) { chunk?.toAnvilLocation() } ?: anvil
    val terrainInfo = CachedTerrainInfo(yLimit, location)

    val terrain = cache.terrains[terrainInfo]

    val load = load@ {
        if (terrainFile == null) return@load

        val bytes = terrainFile.readBytes()
        val subChunks = McaWorker.loadChunksWith(bytes) { chunkLocation, compoundTag ->
            chunkLocation to SurfaceWorker.createSubChunk(compoundTag)
        }
        val pixels = ByteArray(512 * 512 * 4)
        val heights = ShortArray(512 * 512)

        val createY = cache.yRanges[terrainInfo.location] == null
        val yRange = mutableSetOf<Short>()

        subChunks.forEach { (location, chunks) ->
            val baseX = location.x * 16
            val baseZ = location.z * 16
            val surface = SurfaceWorker.createSurface(location, chunks, yLimit.toShort(), createY)
            val blocks = surface.blocks

            if (createY) yRange.addAll(surface.validY)

            blocks.forEachIndexed { index, block ->
                val x = 511 - (baseX + (index / 16))
                val z = baseZ + (index % 16)

                val multiplier = if (block.isWater) block.depth / 7f * 30 - 30 else 1f
                val cutout = if (block.y == yLimit.toShort()) 0.5f else 1f

                pixels[(x * 512 + z) * 4 + 0] = ((block.color[2].toUByte().toInt() + multiplier) * cutout).toInt().coerceIn(0, 255).toByte()
                pixels[(x * 512 + z) * 4 + 1] = ((block.color[1].toUByte().toInt() + multiplier) * cutout).toInt().coerceIn(0, 255).toByte()
                pixels[(x * 512 + z) * 4 + 2] = ((block.color[0].toUByte().toInt() + multiplier) * cutout).toInt().coerceIn(0, 255).toByte()
                pixels[(x * 512 + z) * 4 + 3] = block.color[3]

                heights[x * 512 + z] = block.y

                val hIndex = (x + 1).coerceAtMost(511) * 512 + z
                val pIndex = hIndex * 4
                val aboveY = heights[hIndex]
                if (block.y < aboveY) {
                    (0..2).forEach {
                        pixels[pIndex + it] = (pixels[pIndex + it].toUByte().toInt() + 15)
                            .coerceAtMost(255).toByte()
                    }
                } else if (block.y > aboveY) {
                    (0..2).forEach {
                        pixels[pIndex + it] = (pixels[pIndex + it].toUByte().toInt() - 15)
                            .coerceAtLeast(0).toByte()
                    }
                }
            }
        }

        if (createY)
            cache.yRanges[terrainInfo.location] = yRange
                .map { it.toInt() }
                .toReversedRange(dimension.yRange.first, dimension.yRange.last)

        val bitmap = Bitmap()
            .apply {
                allocPixels(ImageInfo(512, 512, ColorType.N32, ColorAlphaType.OPAQUE))
                installPixels(pixels)
            }
            .toBufferedImage()
            .toComposeImageBitmap()

        val y = cache.yRanges[terrainInfo.location]?.find { it.contains(yLimit) }
        if (y != null) {
            y.asIterable().forEach { limit ->
                val criteriaInfo = terrainInfo.copy(yLimit = limit)
                val criteria = cache.terrains[criteriaInfo]
                if (criteria == null) cache.terrains[criteriaInfo] = bitmap
            }
        } else {
            cache.terrains[terrainInfo] = bitmap
        }

    }

    LaunchedEffect(terrainInfo, terrain) {
        if (terrain != null) return@LaunchedEffect

        withContext(Dispatchers.IO) { load() }
    }

    if (terrain == null) return

    Box(
        modifier = Modifier.fillMaxSize().zIndex(0f)
    ) {
        Image(
            bitmap = terrain,
            contentDescription = null,
            filterQuality = FilterQuality.None,
            modifier = Modifier.fillMaxSize()
        )
        overlay(location)
    }

}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ChunkButtons(
    chunk: ChunkLocation?,
    anvil: AnvilLocation,
    hasNbt: (ChunkLocation) -> Boolean,
    onItemClick: (ChunkLocation) -> Unit,
    onRightClick: () -> Unit,
) {
    var hovered by remember { mutableStateOf(-1 to -1) }

    Column(
        modifier = Modifier.fillMaxSize()
            .onPointerEvent(PointerEventType.Exit) { hovered = -1 to -1 }
    ) {
        for (x in 0 until 32) {
            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                for (z in 0 until 32) {
                    val chunkEach = ChunkLocation(31 - x + 32 * anvil.x, z + 32 * anvil.z)
                    ChunkButton(
                        hovered = hovered.first == x && hovered.second == z,
                        selected = chunkEach == chunk,
                        enabled = hasNbt(chunkEach),
                        onHover = { hovered = x to z },
                        onClick = { onItemClick(chunkEach) },
                        onRightClick = onRightClick
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RowScope.ChunkButton(
    hovered: Boolean,
    selected: Boolean,
    enabled: Boolean,
    onHover: () -> Unit,
    onClick: () -> Unit,
    onRightClick: () -> Unit
) {
    Canvas(
        modifier = Modifier
            .weight(1f).fillMaxHeight()
            .onPointerEvent(PointerEventType.Enter) { onHover() }
            .onPointerEvent(PointerEventType.Press) {
                if (enabled && currentEvent.buttons.isPrimaryPressed) onClick()
                else if (currentEvent.buttons.isSecondaryPressed) onRightClick()
            }
    ) {
        if (hovered || !enabled) {
            drawRect(if (!enabled) Color.Black.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.2f))
        }
        if (selected) {
            drawRect(Color.White, style = Stroke(width = 2.ddp.value))
        }
    }
}

@Composable
fun AnvilPreviewProperties(
    properties: ChunkPreviewProperties,
    yLimit: Int,
    moveToSurroundings: (AnvilLocation) -> Unit,
    changeYLimit: (Int) -> Unit,
    invalidateCache: () -> Unit,
    visible: Boolean
) {
    if (!visible) return

    Box(
        contentAlignment = PropertiesAlignment,
        modifier = Modifier.requiredSize(MinimumViewSize.ddp)
    ) {
        AnvilPreviewPropertyBackground()
        Column(modifier = Modifier.width(220.ddp).padding(top = 8.ddp, start = 12.ddp)) {
            if (!properties.surroundings.isEmpty) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    AnvilNavigateButton("above", properties.surroundings.above, moveToSurroundings)
                    AnvilNavigateButton("left", properties.surroundings.left, moveToSurroundings)
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    AnvilNavigateButton("below", properties.surroundings.below, moveToSurroundings)
                    AnvilNavigateButton("right", properties.surroundings.right, moveToSurroundings)
                }
                PropertyGroupSpacer()
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                PropertyKeyText("loader = ")
                PropertyButton("reload", invalidateCache)
            }
            PropertyGroupSpacer()
            Row(verticalAlignment = Alignment.CenterVertically) {
                PropertyKeyText("yLimit = ")
                YLimitText(yLimit = yLimit, changeYLimit = changeYLimit)
            }
        }
    }
}

@Composable
fun AnvilPreviewPropertyBackground() =
    Box(
        modifier = Modifier
            .fillMaxSize()
            .scale(scaleX = 1f, scaleY = GradientYScale)
            .absoluteOffset(y = GradientOffset.ddp)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    center = Offset.Zero,
                    radius = MinimumViewSize.ddp.value
                )
            )
    )

@Composable
fun RowScope.AnvilNavigateButton(
    direction: String,
    destination: AnvilLocation? = null,
    navigate: (AnvilLocation) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        PropertyKeyText("$direction = ")
        if (destination == null) NullText()
        else PropertyButton("Go") { navigate(destination) }
    }
    Spacer(modifier = Modifier.width(16.ddp))
}

@Composable
fun PropertyKeyText(text: String) =
    Text(
        text = text,
        color = DoodlerTheme.Colors.Text.IdeFunctionProperty,
        fontSize = 9.6.dsp
    )

@Composable
fun PropertyButton(
    text: String,
    onClick: () -> Unit
) = Box(
    modifier = Modifier
        .wrapContentSize()
        .clickable(onClick = onClick)
        .background(DoodlerTheme.Colors.Editor.PropertyButtonBackground, RoundedCornerShape(1.6.ddp)),
    content = {
        Text(
            text = text,
            color = DoodlerTheme.Colors.Text.IdeGeneral,
            fontSize = 8.dsp,
            modifier = Modifier.padding(horizontal = 4.ddp)
        )
    }
)

@Composable
fun NullText() =
    Text(
        text = "null",
        color = DoodlerTheme.Colors.Text.IdeKeyword,
        fontSize = 9.6.dsp
    )

@Composable
fun PropertyGroupSpacer() = Spacer(modifier = Modifier.height(8.ddp))

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun YLimitText(
    yLimit: Int,
    changeYLimit: (Int) -> Unit
) =
    Text(
        text = AnnotatedString(
            text = "$yLimit".padStart(3, '_').plus(" //scroll it!"),
            listOf(
                AnnotatedString.Range(
                    item = SpanStyle(color = DoodlerTheme.Colors.Text.IdeGeneral.copy(alpha = 0.2941f)),
                    start = 0,
                    end = 3 - "$yLimit".length
                ),
                AnnotatedString.Range(
                    item = SpanStyle(color = DoodlerTheme.Colors.Text.IdeComment),
                    start = 3,
                    end = 16
                )
            )
        ),
        color = DoodlerTheme.Colors.Text.IdeGeneral,
        fontSize = 8.dsp,
        modifier = Modifier.padding(vertical = 2.4.ddp)
            .onPointerEvent(PointerEventType.Scroll) { changeYLimit(currentEvent.changes[0].scrollDelta.y.toInt()) }
    )

val PropertiesAlignment = Alignment.TopStart

const val MinimumViewSize = 360f
const val GradientYScale = 0.75f
const val GradientOffset = -1 * MinimumViewSize * (1f - GradientYScale)

val WorldDimension.yRange get() =
    when (this) {
        WorldDimension.Overworld -> -64 until 319
        else -> 0 until 128
    }

val WorldDimension.defaultYLimit get() =
    when(this) {
        WorldDimension.Nether -> 89
        else -> this.yRange.last
    }

data class ChunkPreviewProperties(
    val dimension: WorldDimension,
    val surroundings: AnvilLocationSurroundings
)